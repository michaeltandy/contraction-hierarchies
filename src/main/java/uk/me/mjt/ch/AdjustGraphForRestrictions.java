
package uk.me.mjt.ch;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import uk.me.mjt.ch.TurnRestriction.TurnRestrictionType;


public class AdjustGraphForRestrictions {
    private static final int U_TURN_DELAY_MILLIS = 60*1000;
    
    private static enum AccessOnlyState { SOURCE, NO, DESTINATION, IMPLICIT }
    private static enum BarrierState { SOURCE, NO, IMPLICIT }
    private static enum UTurnState { PENALTY_UNPAID, UNRESTRICTED }
    private static enum GenerateOriginsForDestinations { YES, NO }
    
    private final MapData md;
    private final Node startNode;
    private Set<Long> implicitAccessOnlyNodeIds = null;
    private Set<Long> implicitGatedNodeIds = null;
    private Set<NodeAndState> interestingUturnOrigins = null;
    
    public static MapData makeNewGraph(MapData md, Node startNode) {
        return new AdjustGraphForRestrictions(md, startNode).adjustGraph();
    }
    
    public static String testRestrictedDijkstra(MapData md, Node startNode, Node endNode) {
        return new AdjustGraphForRestrictions(md, null).testRestrictedDijkstraInternal(startNode, endNode);
    }
    
    private AdjustGraphForRestrictions(MapData md, Node startNode) {
        Preconditions.checkNoneNull(md);
        if (startNode != null) {
            Preconditions.require(startNode.barrier==Barrier.FALSE);
        }
        this.md = md;
        this.startNode = startNode;
    }
    
    public MapData adjustGraph() {
        Set<ShortPathElement> shortPathElements = findNodeStateLinks();
        removeSpuriousNodes(shortPathElements);
        
        Set<NodeAndState> nodeStates = findUniqueNodeAndStates(shortPathElements);
        
        Map<NodeAndState,Node> newNodes = makeNewNodes(nodeStates);
        makeNewDirectedEdges(newNodes, shortPathElements);
        
        return new MapData(newNodes.values());
    }
    
    private static Set<NodeAndState> findUniqueNodeAndStates(Set<ShortPathElement> solutions) {
        HashSet<NodeAndState> unique = new HashSet(solutions.size());
        for (ShortPathElement s : solutions) {
            unique.add(s.from);
            unique.add(s.to);
        }
        return unique;
    }
    
    private Map<NodeAndState,Node> makeNewNodes(Set<NodeAndState> sourceData) {
        HashMap<NodeAndState,Node> newNodes = new HashMap(sourceData.size());
        
        long newNodeId = 0;
        for (NodeAndState source : sourceData) {
            newNodes.put(source, new Node(newNodeId++, source.node));
        }
        
        return newNodes;
    }
    
    private void makeNewDirectedEdges(Map<NodeAndState,Node> newNodes, Set<ShortPathElement> sourceData) {
        long newEdgeId = 0;
        
        for (ShortPathElement sourceSPE : sourceData) {
            Node newFromNode = newNodes.get(sourceSPE.from);
            Node newToNode = newNodes.get(sourceSPE.to);
            
            DirectedEdge sourceEdge = sourceSPE.via;
            if (sourceEdge.hasPlaceholderId() && (sourceEdge.driveTimeMs==0 || sourceSPE.to.uTurnState==UTurnState.UNRESTRICTED)) {
                DirectedEdge.makeEdgeWithNoSourceDataEquivalent(newEdgeId++, newFromNode, newToNode, sourceEdge.driveTimeMs, AccessOnly.FALSE)
                        .addToToAndFromNodes();
            } else {
                sourceEdge.cloneWithEdgeIdAndFromToNodeAddingToLists(newEdgeId++, newFromNode, newToNode, AccessOnly.FALSE);
            }
        }
        
    }
    
    private Set<ShortPathElement> findNodeStateLinks() {
        Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge = turnRestrictionsByStartEdge(md.allTurnRestrictions());
        Set<ShortPathElement> fullyPathed = dijkstrasAlgorithm(startNode, turnRestrictionsByStartEdge, GenerateOriginsForDestinations.NO);
        
        identifyImplicitOnlyNodes(groupArrivalOptionsByNode(fullyPathed));
        
        System.out.println("Repathing, only applying implicitly-restricted to nodes that can't be reached without it.");
        fullyPathed = dijkstrasAlgorithm(startNode, turnRestrictionsByStartEdge, GenerateOriginsForDestinations.YES);
        
        if (fullyPathed.size() < 100) {
            System.out.println("Limited implicit-only, found nodes and states:\n"+debugInfoDot(fullyPathed));
        }
        
        System.out.println("Before, " + md.getNodeCount() + " nodes");
        System.out.println("Found, " + fullyPathed.size() + " node-states");
        
        return fullyPathed;
    }
    
    private static Multimap<Long,NodeAndState> groupArrivalOptionsByNode(Set<ShortPathElement> solutions) {
        HashSet<NodeAndState> uniqueNodeAndState = new HashSet();
        
        for (ShortPathElement solution : solutions) {
            uniqueNodeAndState.add(solution.from);
            uniqueNodeAndState.add(solution.to);
        }
        
        Multimap<Long,NodeAndState> result = new Multimap<>();
        for (NodeAndState nas : uniqueNodeAndState) {
            result.add(nas.node.nodeId, nas);
        }
        
        return result;
    }
    
    /**
     * If routing in a big private estate, prefer private roads without barriers
     * to any roads with barriers. Example: OSM node 1654539753 50.93984,-0.65906
     */
    private void identifyImplicitOnlyNodes(Multimap<Long,NodeAndState> arrivalOptionsByNode) {
        implicitAccessOnlyNodeIds = new HashSet();
        implicitGatedNodeIds = new HashSet();
        
        for (Long nodeId : arrivalOptionsByNode.keySet()) {
            boolean alwaysImplicit = true;
            boolean alwaysImplicitBarrier = true;
            for (NodeAndState arrivalOption : arrivalOptionsByNode.get(nodeId)) {
                if (arrivalOption.accessOnlyState!=AccessOnlyState.IMPLICIT
                        && arrivalOption.gateState!=BarrierState.IMPLICIT) {
                    alwaysImplicit = false;
                }
                if (arrivalOption.gateState!=BarrierState.IMPLICIT) {
                    alwaysImplicitBarrier = false;
                }
            }
            if (alwaysImplicit && !alwaysImplicitBarrier) {
                implicitAccessOnlyNodeIds.add(nodeId);
            } else if (alwaysImplicit && alwaysImplicitBarrier) {
                implicitGatedNodeIds.add(nodeId);
            }
        }
        
        System.out.println("Implicitly access-only nodes: " + implicitAccessOnlyNodeIds.size());
        System.out.println("Implicitly gated nodes: " + implicitGatedNodeIds.size());
    }
    
    private void removeSpuriousNodes(Set<ShortPathElement> solutions) {
        Set<NodeAndState> nodeStates = findUniqueNodeAndStates(solutions);
        
        Multimap<NodeAndState, ShortPathElement> inboundEdges = new Multimap();
        Multimap<NodeAndState, ShortPathElement> outboundEdges = new Multimap();
        for (ShortPathElement spe : solutions) {
            inboundEdges.add(spe.to, spe);
            outboundEdges.add(spe.from, spe);
        }
        
        HashSet<NodeAndState> disconnectedNodesForRemoval = new HashSet();
        for (NodeAndState nas : nodeStates) {
            List<ShortPathElement> inbound = inboundEdges.get(nas);
            List<ShortPathElement> outbound = outboundEdges.get(nas);
            if (inbound.isEmpty() || outbound.isEmpty()) {
                disconnectedNodesForRemoval.add(nas);
            }
        }
        
        for (NodeAndState nas : disconnectedNodesForRemoval) {
            deleteNodeAndStateFromFourLists(nas, solutions, nodeStates, inboundEdges, outboundEdges);
        }
        
        HashSet<NodeAndState> uTurnNodesWorthSaving = new HashSet();
        
        for (NodeAndState nas : nodeStates) {
            if (nas.uTurnState == UTurnState.PENALTY_UNPAID) {
                boolean onlyInFromUturns = true;
                for (ShortPathElement spe : inboundEdges.get(nas)) {
                    if (spe.from.uTurnState == UTurnState.PENALTY_UNPAID) {
                        onlyInFromUturns = false;
                    }
                }
                if (onlyInFromUturns) {
                    for (ShortPathElement spe : inboundEdges.get(nas)) {
                        uTurnNodesWorthSaving.add(spe.from);
                    }
                }
            }
        }
        
        for (NodeAndState nas : nodeStates) {
            if (nas.uTurnState == UTurnState.PENALTY_UNPAID) {
                boolean onlyOutToUturns = true;
                for (ShortPathElement spe : outboundEdges.get(nas)) {
                    if (spe.to.uTurnState == UTurnState.PENALTY_UNPAID) {
                        onlyOutToUturns = false;
                    }
                }
                if (onlyOutToUturns) {
                    for (ShortPathElement spe : outboundEdges.get(nas)) {
                        uTurnNodesWorthSaving.add(spe.to);
                    }
                }
            }
        }
        
        HashSet<NodeAndState> unlikelyUturnsForRemoval = new HashSet();
        for (NodeAndState nas : nodeStates) {
            if (nas.uTurnState == UTurnState.UNRESTRICTED && !uTurnNodesWorthSaving.contains(nas)) {
                unlikelyUturnsForRemoval.add(nas);
            }
        }
        
        for (NodeAndState nas : unlikelyUturnsForRemoval) {
            deleteNodeAndStateFromFourLists(nas, solutions, nodeStates, inboundEdges, outboundEdges);
        }
    }
    
    private void deleteNodeAndStateFromFourLists(NodeAndState toDelete, Set<ShortPathElement> solutions, Set<NodeAndState> nodeStates, Multimap<NodeAndState, ShortPathElement> inboundEdges, Multimap<NodeAndState, ShortPathElement> outboundEdges ) {
        nodeStates.remove(toDelete);
        for (ShortPathElement spe : new ArrayList<>(inboundEdges.get(toDelete))) {
            inboundEdges.removeValueForKey(spe.to, spe);
            outboundEdges.removeValueForKey(spe.from, spe);
            solutions.remove(spe);
        }
        for (ShortPathElement spe : new ArrayList<>(outboundEdges.get(toDelete))) {
            inboundEdges.removeValueForKey(spe.to, spe);
            outboundEdges.removeValueForKey(spe.from, spe);
            solutions.remove(spe);
        }
    }
    
    private String testRestrictedDijkstraInternal(Node startNode, Node endNode) {
        Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge = turnRestrictionsByStartEdge(md.allTurnRestrictions());
        
        Set<ShortPathElement> fullyPathed = dijkstrasAlgorithm(startNode, turnRestrictionsByStartEdge, GenerateOriginsForDestinations.NO);
        
        List<ShortPathElement> rightDestination = new ArrayList();
        for (ShortPathElement solution : fullyPathed) {
            if (solution.to.node == endNode) {
                rightDestination.add(solution);
            }
        }
        
        ShortPathElement bestSolution = chooseBestSolution(rightDestination);
        
        return solutionAsString(bestSolution);
    }
    
    private static String solutionAsString(ShortPathElement spe) {
        if (spe.previous==null) {
            return spe.from.node.sourceDataNodeId + "--" + spe.via.driveTimeMs + "-->" + spe.to.node.sourceDataNodeId;
        } else if (isPublicToPrivateEdge(spe.via) ) {
            return solutionAsString(spe.previous);
        } else {
            return solutionAsString(spe.previous) + "--" + spe.via.driveTimeMs + "-->" + spe.to.node.sourceDataNodeId;
        }
    }
    
    private ShortPathElement chooseBestSolution(List<ShortPathElement> solutions) {
        if (solutions.isEmpty())
            throw new RuntimeException("No solutions found?");
        
        ShortPathElement bestSolutionSeen = solutions.get(0);
        for (ShortPathElement solution : solutions) {
            boolean accessOnlyImproved = (bestSolutionSeen.to.accessOnlyState==AccessOnlyState.IMPLICIT && solution.to.accessOnlyState!=AccessOnlyState.IMPLICIT);
            boolean accessOnlyWorsened = (bestSolutionSeen.to.accessOnlyState!=AccessOnlyState.IMPLICIT && solution.to.accessOnlyState==AccessOnlyState.IMPLICIT);
            
            boolean barrierImproved = (bestSolutionSeen.to.gateState==BarrierState.IMPLICIT && solution.to.gateState!=BarrierState.IMPLICIT);
            boolean barrierWorsened = (bestSolutionSeen.to.gateState!=BarrierState.IMPLICIT && solution.to.gateState==BarrierState.IMPLICIT);
            
            boolean driveTimeImproved = (bestSolutionSeen.driveTimeMs > solution.driveTimeMs);
            
            if (accessOnlyWorsened || barrierWorsened) {
                // Do nothing.
            } else if (accessOnlyImproved || barrierImproved || driveTimeImproved) {
                bestSolutionSeen = solution;
            }
        }
        return bestSolutionSeen;
    }
    
    private static Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge(Collection<TurnRestriction> allRestrictions) {
        Multimap<Long,TurnRestriction> reverseIndex = new Multimap<>();
        for (TurnRestriction tr : allRestrictions) {
            reverseIndex.add(tr.directedEdgeIds.get(0), tr);
        }
        return reverseIndex;
    }
    
    private Set<ShortPathElement> dijkstrasAlgorithm(Node startNode, Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge, GenerateOriginsForDestinations generateOrigins) {
        Preconditions.checkNoneNull(startNode, turnRestrictionsByStartEdge, generateOrigins);
        if (generateOrigins==GenerateOriginsForDestinations.YES)
            Preconditions.checkNoneNull(implicitGatedNodeIds,implicitAccessOnlyNodeIds);
        HashMap<NodeAndState,NodeInfo> nodeInfo = new HashMap<>();
        HashSet<ShortPathElement> solutions = new HashSet<>();
        NodeAndState startState = nodeAndStateForStartNode(startNode);
        
        PriorityQueue<DistanceOrder> unvisitedNodes = new PriorityQueue<>();
        if (true) {
            DistanceOrder startDo = new DistanceOrder(0,startState);
            unvisitedNodes.add(startDo);

            NodeInfo startNodeInfo = new NodeInfo();
            startNodeInfo.minDriveTime = 0;
            startNodeInfo.distanceOrder = startDo;

            nodeInfo.put(startState, startNodeInfo);
        }
        
        while (!unvisitedNodes.isEmpty()) {
            DistanceOrder minHeapEntry = unvisitedNodes.poll();
            NodeAndState shortestTimeNode = minHeapEntry.nodeAndState;
            NodeInfo thisNodeInfo = nodeInfo.get(shortestTimeNode);
            
            //System.out.println("Visiting " + shortestTimeNode);
            
            if (generateOrigins==GenerateOriginsForDestinations.YES) {
                if (shortestTimeNode.accessOnlyState==AccessOnlyState.DESTINATION || shortestTimeNode.accessOnlyState==AccessOnlyState.IMPLICIT) {
                    NodeAndState sourceEquivalentToThisDestination = nodeAndStateForStartNode(shortestTimeNode.node);
                    if (nodeInfo.containsKey(sourceEquivalentToThisDestination)) {
                        // Already generated - visited this node with a different state.
                    } else {
                        DistanceOrder sourceDo = new DistanceOrder(0,sourceEquivalentToThisDestination);
                        NodeInfo sourceNodeInfo = new NodeInfo();
                        sourceNodeInfo.minDriveTime = 0;
                        sourceNodeInfo.distanceOrder = sourceDo;
                        unvisitedNodes.add(sourceDo);
                        nodeInfo.put(sourceEquivalentToThisDestination, sourceNodeInfo);
                    }
                }

                if (shortestTimeNode.gateState==BarrierState.IMPLICIT && implicitGatedNodeIds.contains(shortestTimeNode.node.nodeId)) {
                    AccessOnlyState aos = (anyEdgesAccessOnly(shortestTimeNode.node) ? AccessOnlyState.SOURCE : AccessOnlyState.NO);
                    NodeAndState source = new NodeAndState(shortestTimeNode.node, new HashSet(), aos, BarrierState.SOURCE, UTurnState.UNRESTRICTED, null);
                    if (nodeInfo.containsKey(source)) {
                        // Already generated - visited this node with a different state.
                    } else {
                        DistanceOrder sourceDo = new DistanceOrder(0,source);
                        NodeInfo sourceNodeInfo = new NodeInfo();
                        sourceNodeInfo.minDriveTime = 0;
                        sourceNodeInfo.distanceOrder = sourceDo;
                        unvisitedNodes.add(sourceDo);
                        nodeInfo.put(source, sourceNodeInfo);
                    }
                }
            }
            
            thisNodeInfo.visited = true;
            thisNodeInfo.distanceOrder = null;
            
            List<DirectedEdge> outgoingEdges = shortestTimeNode.node.edgesFrom;
            if (shortestTimeNode.uTurnState==UTurnState.PENALTY_UNPAID
                    && (interestingUturnOrigins==null || interestingUturnOrigins.contains(shortestTimeNode))) {
                outgoingEdges = new UnionList<>(outgoingEdges,makeUTurnDelayEdge(shortestTimeNode));
            }
            
            if (shortestTimeNode.accessOnlyState!=AccessOnlyState.DESTINATION && anyEdgesAccessOnly(shortestTimeNode.node)) {
                outgoingEdges = new UnionList<>(outgoingEdges,makePublicToAccessRestrictedEdge(shortestTimeNode));
            }
            
            ShortPathElement lastNonSyntheticEdge = thisNodeInfo.minTimeFromElement;
            while (lastNonSyntheticEdge!=null && (isUturnEdge(lastNonSyntheticEdge.via) || isPublicToPrivateEdge(lastNonSyntheticEdge.via))) {
                lastNonSyntheticEdge = nodeInfo.get(lastNonSyntheticEdge.from).minTimeFromElement;
            }
            
            for (DirectedEdge edge : outgoingEdges) {
                NodeAndState neighbor = updateStateIfLegal(thisNodeInfo.minTimeVia, shortestTimeNode, edge, turnRestrictionsByStartEdge, (lastNonSyntheticEdge==null?null:lastNonSyntheticEdge.via));
                
                if (neighbor==null)
                    continue;
                
                NodeInfo neighborNodeInfo = nodeInfo.get(neighbor);
                if (neighborNodeInfo == null) {
                    neighborNodeInfo = new NodeInfo();
                    nodeInfo.put(neighbor, neighborNodeInfo);
                }
                
                ShortPathElement spe = new ShortPathElement(shortestTimeNode, neighbor, edge, thisNodeInfo.minTimeFromElement);
                solutions.add(spe);
                
                if (neighborNodeInfo.visited)
                    continue;
                
                int newTime = thisNodeInfo.minDriveTime + edge.driveTimeMs;
                int previousTime = neighborNodeInfo.minDriveTime;
                
                if (newTime < previousTime) {
                    neighborNodeInfo.minDriveTime = newTime;
                    neighborNodeInfo.minTimeFrom = shortestTimeNode;
                    neighborNodeInfo.minTimeVia = edge;
                    neighborNodeInfo.minTimeFromElement = spe;
                    
                    if (neighborNodeInfo.distanceOrder != null) {
                        unvisitedNodes.remove(neighborNodeInfo.distanceOrder);
                    }
                    DistanceOrder newDistOrder = new DistanceOrder(newTime, neighbor);
                    neighborNodeInfo.distanceOrder = newDistOrder;
                    unvisitedNodes.add(newDistOrder);
                }
            }
        }
        
        return solutions;
    }

    private final class DistanceOrder implements Comparable<DistanceOrder> {
        private final int minDriveTime;
        public final NodeAndState nodeAndState;

        public DistanceOrder(int minDriveTime, NodeAndState node) {
            this.minDriveTime = minDriveTime;
            this.nodeAndState = node;
        }
        
        @Override
        public int compareTo(DistanceOrder that) {
            if (this.minDriveTime < that.minDriveTime) {
                return -1;
            } else if (this.minDriveTime > that.minDriveTime) {
                return 1;
            } else if (this.nodeAndState.node.nodeId != that.nodeAndState.node.nodeId) {
                return Long.compare(this.nodeAndState.node.nodeId,that.nodeAndState.node.nodeId);
            } else {
                return Integer.compare(this.nodeAndState.hashCode(), that.nodeAndState.hashCode());
            }
        }
    }
    
    private final class NodeInfo {
        boolean visited = false;
        int minDriveTime = Integer.MAX_VALUE;
        NodeAndState minTimeFrom = null;
        DirectedEdge minTimeVia = null;
        DistanceOrder distanceOrder = null;
        ShortPathElement minTimeFromElement = null;
    }
    
    private NodeAndState nodeAndStateForStartNode(Node n) {
        AccessOnlyState aos = (anyEdgesAccessOnly(n) ? AccessOnlyState.SOURCE : AccessOnlyState.NO);
        return new NodeAndState(n, new HashSet(), aos, BarrierState.NO, UTurnState.UNRESTRICTED, null);
    }
    
    private class NodeAndState {
        public final Node node;
        public final HashSet<TurnRestriction> activeTurnRestrictions;
        public final AccessOnlyState accessOnlyState;
        public final BarrierState gateState;
        public final UTurnState uTurnState;
        public final DirectedEdge arrivingViaEdge;

        public NodeAndState(Node node, HashSet<TurnRestriction> activeTurnRestrictions, AccessOnlyState accessOnlyState, BarrierState gateState, UTurnState uTurnState, DirectedEdge arrivingViaEdge) {
            Preconditions.checkNoneNull(node, activeTurnRestrictions, accessOnlyState, gateState, uTurnState);
            this.node = node;
            this.activeTurnRestrictions = activeTurnRestrictions;
            this.accessOnlyState = accessOnlyState;
            this.gateState = gateState;
            this.uTurnState = uTurnState;
            this.arrivingViaEdge = arrivingViaEdge;
        }
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + Objects.hashCode(this.node);
            hash = 53 * hash + Objects.hashCode(this.activeTurnRestrictions);
            hash = 53 * hash + Objects.hashCode(this.accessOnlyState);
            hash = 53 * hash + Objects.hashCode(this.gateState);
            hash = 53 * hash + Objects.hashCode(this.uTurnState);
            hash = 53 * hash + Objects.hashCode(this.arrivingViaEdge);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final NodeAndState other = (NodeAndState) obj;
            return Objects.equals(this.node, other.node)
                    && Objects.equals(this.activeTurnRestrictions, other.activeTurnRestrictions)
                    && this.accessOnlyState == other.accessOnlyState 
                    && this.gateState == other.gateState
                    && this.uTurnState == other.uTurnState
                    && Objects.equals(this.arrivingViaEdge, other.arrivingViaEdge);
        }
        
        
        public String toString() {
            return node + " with " + activeTurnRestrictions + " AO." + accessOnlyState + " Barrier." + gateState + " U." + uTurnState + " from " + (arrivingViaEdge==null?"null":arrivingViaEdge.edgeId);
        }
    }
    
    private boolean anyEdgesAccessOnly(Node n) {
        for (DirectedEdge de : n.getEdgesFromAndTo()) {
            if (de.accessOnly == AccessOnly.TRUE) {
                return true;
            }
        }
        return false;
    }
    
    private NodeAndState updateStateIfLegal(DirectedEdge fromEdge, NodeAndState fromNodeState, DirectedEdge toEdge, Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge, DirectedEdge priorFromEdge) {
        Preconditions.checkNoneNull(fromNodeState,toEdge,turnRestrictionsByStartEdge);
        
        Node toNode = toEdge.to;
        
        HashSet<TurnRestriction> turnRestrictionsAfter = getUpdatedTurnRestrictionsIfLegal(fromEdge, fromNodeState, toEdge, turnRestrictionsByStartEdge, priorFromEdge);
        if (turnRestrictionsAfter==null) return null;
        
        BarrierState gs = updateBarrierStateIfLegal(fromNodeState, toNode);
        if (gs==null) return null;
        
        AccessOnlyState aos = updateAccessOnlyStateIfLegal(fromNodeState, toEdge);
        if (aos==null) return null;
        
        UTurnState us = updateUTurnStateIfLegal(fromEdge, toEdge);
        if (us==null) return null;
        
        return new NodeAndState(toNode, turnRestrictionsAfter, aos, gs, us, toEdge);
    }
    
    private HashSet<TurnRestriction> getUpdatedTurnRestrictionsIfLegal(DirectedEdge fromEdge, NodeAndState fromNode, DirectedEdge toEdge, Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge, DirectedEdge priorFromEdge) {
        Preconditions.checkNoneNull(fromNode,toEdge,turnRestrictionsByStartEdge);
        if (isUturnEdge(toEdge) || isPublicToPrivateEdge(toEdge)) { // U-turns don't deactivate turn restrictions.
            return fromNode.activeTurnRestrictions;
        }
        
        HashSet<TurnRestriction> turnRestrictionsAfter = new HashSet();
        turnRestrictionsAfter.addAll(turnRestrictionsByStartEdge.get(toEdge.edgeId));
        
        if (fromEdge == null) { // Start node.
            return turnRestrictionsAfter;
        }
        
        if (priorFromEdge == null)
            priorFromEdge = fromEdge;
        
        for (TurnRestriction tr : fromNode.activeTurnRestrictions) {
            List<Long> edgeIds = tr.directedEdgeIds;
            
            int fromEdgeIdx = edgeIds.indexOf(priorFromEdge.edgeId);
            int toEdgeIdx = edgeIds.indexOf(toEdge.edgeId);
            
            boolean endOfRestriction = (fromEdgeIdx==edgeIds.size()-2);
            boolean restrictionCoversMove = (fromEdgeIdx+1==toEdgeIdx);
            
            if (endOfRestriction) {
                if (tr.type == TurnRestrictionType.ONLY_ALLOWED && !restrictionCoversMove) {
                    return null;
                } else if (tr.type == TurnRestrictionType.NOT_ALLOWED && restrictionCoversMove) {
                    return null;
                }
            } else if (restrictionCoversMove) {
                turnRestrictionsAfter.add(tr);
            }
        }
        
        return turnRestrictionsAfter;
    }
    
    private BarrierState updateBarrierStateIfLegal(NodeAndState fromNode, Node toNode) {
        Preconditions.checkNoneNull(fromNode,toNode);
        if (fromNode.gateState == BarrierState.SOURCE && implicitGatedNodeIds!=null && implicitGatedNodeIds.contains(toNode.nodeId))
            return BarrierState.SOURCE;
        else if (fromNode.gateState == BarrierState.IMPLICIT || toNode.barrier==Barrier.TRUE)
            if (implicitGatedNodeIds==null || implicitGatedNodeIds.contains(toNode.nodeId))
                return BarrierState.IMPLICIT;
            else
                return null;
        else
            return BarrierState.NO;
    }
    
    private AccessOnlyState updateAccessOnlyStateIfLegal(NodeAndState fromNode, DirectedEdge toEdge) {
        boolean implicitPermitted = (implicitAccessOnlyNodeIds==null || implicitAccessOnlyNodeIds.contains(toEdge.to.nodeId));
                
        if (fromNode.accessOnlyState==AccessOnlyState.SOURCE) {
            if (toEdge.accessOnly==AccessOnly.TRUE)
                return AccessOnlyState.SOURCE;
            else if (implicitAccessOnlyNodeIds!=null && (implicitAccessOnlyNodeIds.contains(toEdge.to.nodeId)||implicitAccessOnlyNodeIds.contains(fromNode.node.nodeId)) )
                return AccessOnlyState.SOURCE;
            else
                return AccessOnlyState.NO;
        } else if (fromNode.accessOnlyState==AccessOnlyState.NO) {
            if (toEdge.accessOnly==AccessOnly.TRUE)
                return AccessOnlyState.DESTINATION;
            else
                return AccessOnlyState.NO;
        } else if (fromNode.accessOnlyState==AccessOnlyState.DESTINATION) {
            if (toEdge.accessOnly==AccessOnly.TRUE)
                return AccessOnlyState.DESTINATION;
            else if (implicitPermitted)
                return AccessOnlyState.IMPLICIT;
            else
                return null;
        } else { //if (fromNode.accessOnlyState==AccessOnlyState.IMPLICIT) {
            if (implicitPermitted)
                return AccessOnlyState.IMPLICIT;
            else
                return null;
        }
    }
    
    private UTurnState updateUTurnStateIfLegal(DirectedEdge fromEdge, DirectedEdge toEdge) {
        if (fromEdge==null) { // Start node
            return UTurnState.PENALTY_UNPAID;
        } else if (isUnpaidUturn(fromEdge, toEdge)) {
            return null;
        } else if (isUturnEdge(toEdge)) {
            return UTurnState.UNRESTRICTED;
        } else {
            return UTurnState.PENALTY_UNPAID;
        }
    }
    
    private boolean isUnpaidUturn(DirectedEdge fromEdge, DirectedEdge toEdge) {
        return (fromEdge.from == toEdge.to);
    }
    
    private boolean isUturnEdge(DirectedEdge de) {
        return (de.from==de.to && de.hasPlaceholderId() && de.driveTimeMs==U_TURN_DELAY_MILLIS);
    }
    
    private static boolean isPublicToPrivateEdge(DirectedEdge de) {
        return (de.from==de.to && de.hasPlaceholderId() && de.driveTimeMs==0 && de.accessOnly==AccessOnly.TRUE);
    }
    
    private DirectedEdge makeUTurnDelayEdge(NodeAndState nas) {
        AccessOnly ao;
        if (nas.accessOnlyState==AccessOnlyState.SOURCE || nas.accessOnlyState==AccessOnlyState.DESTINATION)
            ao = AccessOnly.TRUE;
        else
            ao = AccessOnly.FALSE;
        
        return DirectedEdge.makeDelayEdge(nas.node, U_TURN_DELAY_MILLIS, ao);
    }
    
    private DirectedEdge makePublicToAccessRestrictedEdge(NodeAndState nas) {
        return DirectedEdge.makeDelayEdge(nas.node, 0, AccessOnly.TRUE);
    }
    
    private class ShortPathElement {
        final NodeAndState from;
        final NodeAndState to;
        final DirectedEdge via;
        final ShortPathElement previous;
        final int driveTimeMs;
        final int hashCode;

        public ShortPathElement(NodeAndState from, NodeAndState to, DirectedEdge via, ShortPathElement previous) {
            Preconditions.checkNoneNull(from,to,via);
            Preconditions.require(previous==null || from.equals(previous.to));
            this.from = from;
            this.to = to;
            this.via = via;
            this.previous = previous;
            this.driveTimeMs=via.driveTimeMs+(previous==null?0:previous.driveTimeMs);
            
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.from);
            hash = 37 * hash + Objects.hashCode(this.to);
            hash = 37 * hash + Objects.hashCode(this.via);
            hash = 37 * hash + Objects.hashCode(this.driveTimeMs);
            hash = 37 * hash + Objects.hashCode(this.previous);
            this.hashCode = hash;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final ShortPathElement other = (ShortPathElement) obj;
            return  Objects.equals(this.from, other.from)
                    && Objects.equals(this.to, other.to)
                    && Objects.equals(this.via, other.via)
                    && Objects.equals(this.driveTimeMs, other.driveTimeMs)
                    && Objects.equals(this.hashCode, other.hashCode);
        }
        
        public String toString() {
            return "Via " + via + " from " + from + " to " + to;
        }
    }
    
    private static String nodeToString(NodeAndState n) {
        StringBuilder sb = new StringBuilder();
        sb.append("Node ").append(n.node.nodeId)
                .append("\\nR ").append(n.activeTurnRestrictions)
                .append("\\nAO ").append(n.accessOnlyState)
                .append("\\nB ").append(n.gateState).append(" U ").append(n.uTurnState);
        if (n.arrivingViaEdge==null) {
            sb.append("\\nOrigin node");
        } else {
            sb.append("\\nfrom ").append(n.arrivingViaEdge.from.nodeId);
        }
        
        return sb.toString();
    }
    
    private String debugInfoDot(Set<ShortPathElement> shortPathElements) {
        Set<NodeAndState> uniqueNodes = findUniqueNodeAndStates(shortPathElements);

        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n")
                .append("    overlap = false;\n");
        
        HashMap<NodeAndState,String> nodeTags = new HashMap<>();
        int nextNodeId=1;
        for (NodeAndState nas : uniqueNodes) {
            String nodeTag = "N"+nextNodeId;
            nodeTags.put(nas, nodeTag);
            sb.append("    ").append(nodeTag).append(" [label=\"").append(nodeToString(nas))
                    .append("\",style=\"filled\",color=\"").append(randomColorFor(nas.node.toString())).append("\"];\n");
            nextNodeId++;
        }
        
        for (ShortPathElement element : shortPathElements) {
            String fromTag = nodeTags.get(element.from);
            String toTag = nodeTags.get(element.to);
            sb.append("    ").append(fromTag).append(" -> ").append(toTag).append(";\n");
        }
        sb.append("    label=\"").append(uniqueNodes.size()).append(" node-states; ")
                .append(shortPathElements.size()).append(" short path elements.\";\n");
        
        sb.append("}");
        return sb.toString();
    }
    
    private String randomColorFor(String toHash) {
        byte[] hashed;
        try {
            hashed = MessageDigest.getInstance("SHA-256").digest(toHash.getBytes());
        } catch (NoSuchAlgorithmException e) { throw new RuntimeException("Impossible"); }
        int brightenIdx = hashed[3]%3;
        int red = (hashed[0]&0xFF) | (brightenIdx==0?0b10000000:0);
        int green = (hashed[1]&0xFF) | (brightenIdx==1?0b10000000:0);;
        int blue = (hashed[2]&0xFF) | (brightenIdx==2?0b10000000:0);;
        return String.format("#%02x%02x%02x", red,green,blue);
    }
    
}
