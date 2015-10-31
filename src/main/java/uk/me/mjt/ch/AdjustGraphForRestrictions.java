
package uk.me.mjt.ch;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import uk.me.mjt.ch.TurnRestriction.TurnRestrictionType;


public class AdjustGraphForRestrictions {
    private static final int U_TURN_DELAY_MILLIS = 60*1000;
    
    private static enum AccessOnlyState { SOURCE, NO, DESTINATION, IMPLICIT }
    private static enum BarrierState { NO, IMPLICIT }
    private static enum UTurnState { PENALTY_UNPAID, UNRESTRICTED }
    
    public String testRestrictedDijkstra(MapData md, Node startNode, Node endNode) {
        Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge = turnRestrictionsByStartEdge(md.allTurnRestrictions());
        
        List<Solution> fullyPathed = dijkstrasAlgorithm(startNode, turnRestrictionsByStartEdge, Integer.MAX_VALUE);
        
        List<Solution> rightDestination = new ArrayList();
        for (Solution solution : fullyPathed) {
            if (solution.to.node == endNode) {
                rightDestination.add(solution);
            }
        }
        
        Solution bestSolution = chooseBestSolution(rightDestination);
        
        StringBuilder solutionAsString = new StringBuilder();
        for (int i=0 ; i<bestSolution.route.size() ; i++) {
            ShortestPathElement spe = bestSolution.route.get(i);
            if (i==0) solutionAsString.append(spe.from.node.nodeId);
            solutionAsString.append("--").append(spe.via.driveTimeMs).append("-->").append(spe.to.node.nodeId);
        }
        return solutionAsString.toString();
    }
    
    private Solution chooseBestSolution(List<Solution> solutions) {
        if (solutions.isEmpty())
            throw new RuntimeException("No solutions found?");
        
        Solution bestSolutionSeen = solutions.get(0);
        for (Solution solution : solutions) {
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
    
    private List<Solution> dijkstrasAlgorithm(Node startNode, Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge, int driveTimeLimitMs) {
        HashMap<NodeAndState,NodeInfo> nodeInfo = new HashMap<>();
        ArrayList<Solution> solutions = new ArrayList<>();
        NodeAndState startState = nodeAndStateForStartNode(startNode);
        
        PriorityQueue<DistanceOrder> unvisitedNodes = new PriorityQueue<>();
        DistanceOrder startDo = new DistanceOrder(0,startState);
        unvisitedNodes.add(startDo);
        
        NodeInfo startNodeInfo = new NodeInfo();
        startNodeInfo.minDriveTime = 0;
        startNodeInfo.distanceOrder = startDo;
        
        nodeInfo.put(startState, startNodeInfo);
        
        while (!unvisitedNodes.isEmpty()) {
            DistanceOrder minHeapEntry = unvisitedNodes.poll();
            NodeAndState shortestTimeNode = minHeapEntry.nodeAndState;
            NodeInfo thisNodeInfo = nodeInfo.get(shortestTimeNode);
            
            //System.out.println("Visiting " + shortestTimeNode);
            solutions.add(extractShortest(shortestTimeNode, nodeInfo));
            
            if (thisNodeInfo.minDriveTime > driveTimeLimitMs) {
                System.out.println("Hit drive time limit without visiting all nodes for turn restrictions around " + startNode + 
                        " which probably just means this is an exit-only node.");
                return solutions;
            }
            
            thisNodeInfo.visited = true;
            thisNodeInfo.distanceOrder = null;
            
            List<DirectedEdge> outgoingEdges;
            if (shortestTimeNode.uTurnState==UTurnState.PENALTY_UNPAID) {
                outgoingEdges = new ArrayList(shortestTimeNode.node.edgesFrom);
                outgoingEdges.add(makeUTurnDelayEdge(shortestTimeNode));
            } else {
                outgoingEdges = shortestTimeNode.node.edgesFrom;
            }
            
            for (DirectedEdge edge : outgoingEdges) {
                NodeAndState n = updateStateIfLegal(thisNodeInfo.minTimeVia, shortestTimeNode, edge, turnRestrictionsByStartEdge);
                
                if (n==null)
                    continue;
                
                NodeInfo neighborNodeInfo = nodeInfo.get(n);
                if (neighborNodeInfo == null) {
                    neighborNodeInfo = new NodeInfo();
                    nodeInfo.put(n, neighborNodeInfo);
                }
                
                if (neighborNodeInfo.visited)
                    continue;
                
                int newTime = thisNodeInfo.minDriveTime + edge.driveTimeMs;
                int previousTime = neighborNodeInfo.minDriveTime;
                
                if (newTime < previousTime) {
                    neighborNodeInfo.minDriveTime = newTime;
                    neighborNodeInfo.minTimeFrom = shortestTimeNode;
                    neighborNodeInfo.minTimeVia = edge;
                    
                    if (neighborNodeInfo.distanceOrder != null) {
                        unvisitedNodes.remove(neighborNodeInfo.distanceOrder);
                    }
                    DistanceOrder newDistOrder = new DistanceOrder(newTime, n);
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
    }
    
    private NodeAndState nodeAndStateForStartNode(Node n) {
        AccessOnlyState aos = (anyEdgesAccessOnly(n) ? AccessOnlyState.SOURCE : AccessOnlyState.NO);
        return new NodeAndState(n, new HashSet(), aos, BarrierState.NO, UTurnState.UNRESTRICTED);
    }
    
    private class NodeAndState {
        public final Node node;
        public final HashSet<TurnRestriction> activeTurnRestrictions;
        public final AccessOnlyState accessOnlyState;
        public final BarrierState gateState;
        public final UTurnState uTurnState;

        public NodeAndState(Node node, HashSet<TurnRestriction> activeTurnRestrictions, AccessOnlyState accessOnlyState, BarrierState gateState, UTurnState uTurnState) {
            Preconditions.checkNoneNull(node, activeTurnRestrictions, accessOnlyState, gateState, uTurnState);
            this.node = node;
            this.activeTurnRestrictions = activeTurnRestrictions;
            this.accessOnlyState = accessOnlyState;
            this.gateState = gateState;
            this.uTurnState = uTurnState;
        }
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + Objects.hashCode(this.node);
            hash = 53 * hash + Objects.hashCode(this.activeTurnRestrictions);
            hash = 53 * hash + Objects.hashCode(this.accessOnlyState);
            hash = 53 * hash + Objects.hashCode(this.gateState);
            hash = 53 * hash + Objects.hashCode(this.uTurnState);
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
                    && this.uTurnState == other.uTurnState;
        }
        
        
        public String toString() {
            return node + " with " + activeTurnRestrictions + " AO." + accessOnlyState + " Barrier." + gateState + " U." + uTurnState;
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
    
    private NodeAndState updateStateIfLegal(DirectedEdge fromEdge, NodeAndState fromNodeState, DirectedEdge toEdge, Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge) {
        Preconditions.checkNoneNull(fromNodeState,toEdge,turnRestrictionsByStartEdge);
        
        Node toNode = toEdge.to;
        
        HashSet<TurnRestriction> turnRestrictionsAfter = getUpdatedTurnRestrictionsIfLegal(fromEdge, fromNodeState, toEdge, turnRestrictionsByStartEdge);
        if (turnRestrictionsAfter==null) return null;
        
        BarrierState gs = updateBarrierState(fromNodeState, toNode);
        AccessOnlyState aos = updateAccessOnlyState(fromNodeState, toEdge);
        
        UTurnState us = updateUTurnStateIfLegal(fromEdge, toEdge);
        if (us==null) return null;
        
        return new NodeAndState(toNode, turnRestrictionsAfter, aos, gs, us);
    }
    
    private HashSet<TurnRestriction> getUpdatedTurnRestrictionsIfLegal(DirectedEdge fromEdge, NodeAndState fromNode, DirectedEdge toEdge, Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge) {
        Preconditions.checkNoneNull(fromNode,toEdge,turnRestrictionsByStartEdge);
        if (toEdge.isDelayEdge()) { // U-turns don't deactivate turn restrictions.
            return fromNode.activeTurnRestrictions;
        }
        
        HashSet<TurnRestriction> turnRestrictionsAfter = new HashSet();
        turnRestrictionsAfter.addAll(turnRestrictionsByStartEdge.get(toEdge.edgeId));
        
        if (fromEdge == null) { // Start node.
            return turnRestrictionsAfter;
        }
        
        for (TurnRestriction tr : fromNode.activeTurnRestrictions) {
            List<Long> edgeIds = tr.directedEdgeIds;
            int fromEdgeIdx = edgeIds.indexOf(fromEdge.edgeId);
            int toEdgeIdx = edgeIds.indexOf(toEdge.edgeId);
            
            boolean endOfRestriction = (toEdgeIdx==edgeIds.size()-1);
            boolean restrictionCoversMove = toEdgeIdx>=0; // REVISIT are there any real turn restrictions where graph-adjacent edges appear nonconsecutively?
            
            if (fromEdgeIdx>=0 && (restrictionCoversMove != (fromEdgeIdx+1==toEdgeIdx))) {
                throw new RuntimeException("I wasn't sure whether it's possible to have turn restrictions where "
                        + "edges that are adjacent in the graph appear nonconsecutively in the restriction. "
                        + "Ignoring that case made dealing with U-turns during turn restrictions simpler. If "
                        + "you're seeing this exception, then it is possible and the code should be updated "
                        + "to account for it! Restriction " + tr.turnRestrictionId);
            }
            
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
    
    private BarrierState updateBarrierState(NodeAndState fromNode, Node toNode) {
        Preconditions.checkNoneNull(fromNode,toNode);
        if (fromNode.gateState == BarrierState.IMPLICIT || toNode.barrier==Barrier.TRUE)
            return BarrierState.IMPLICIT;
        else
            return BarrierState.NO;
    }
    
    private AccessOnlyState updateAccessOnlyState(NodeAndState fromNode, DirectedEdge toEdge) {
        if (fromNode.accessOnlyState==AccessOnlyState.SOURCE) {
            if (toEdge.accessOnly==AccessOnly.TRUE)
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
            else
                return AccessOnlyState.IMPLICIT;
        } else { //if (fromNode.accessOnlyState==AccessOnlyState.IMPLICIT) {
            return AccessOnlyState.IMPLICIT;
        }
    }
    
    private UTurnState updateUTurnStateIfLegal(DirectedEdge fromEdge, DirectedEdge toEdge) {
        if (fromEdge==null) { // Start node
            return UTurnState.PENALTY_UNPAID;
        } else if (isUnpaidUturn(fromEdge, toEdge)) {
            return null;
        } else if (toEdge.isDelayEdge()) {
            return UTurnState.UNRESTRICTED;
        } else {
            return UTurnState.PENALTY_UNPAID;
        }
    }
    
    private boolean isUnpaidUturn(DirectedEdge fromEdge, DirectedEdge toEdge) {
        return (fromEdge.from == toEdge.to);
    }
    
    private class Solution {
        final NodeAndState from;
        final NodeAndState to;
        final int driveTimeMs;
        final List<ShortestPathElement> route;

        public Solution(NodeAndState from, NodeAndState to, int driveTimeMs, List<ShortestPathElement> route) {
            this.from = from;
            this.to = to;
            this.driveTimeMs = driveTimeMs;
            this.route = route;
        }
    }
    
    private DirectedEdge makeUTurnDelayEdge(NodeAndState nas) {
        AccessOnly ao;
        if (nas.accessOnlyState==AccessOnlyState.SOURCE || nas.accessOnlyState==AccessOnlyState.DESTINATION)
            ao = AccessOnly.TRUE;
        else
            ao = AccessOnly.FALSE;
        
        return DirectedEdge.makeDelayEdge(nas.node, U_TURN_DELAY_MILLIS, ao);
    }
    
    private Solution extractShortest(final NodeAndState endNode, HashMap<NodeAndState,NodeInfo> nodeInfo) {
        List<ShortestPathElement> route = new LinkedList();
        
        NodeAndState lastNode = null;
        NodeAndState thisNode = endNode;
        
        while (thisNode != null) {
            NodeInfo lastNodeInfo = nodeInfo.get(lastNode);
            NodeInfo thisNodeInfo = nodeInfo.get(thisNode);
            if (lastNode != null) 
                route.add(0,new ShortestPathElement(thisNode, lastNode, lastNodeInfo.minTimeVia));
            
            lastNode = thisNode;
            thisNode = thisNodeInfo.minTimeFrom;
        }
        
        NodeInfo endNodeInfo = nodeInfo.get(endNode);
        return new Solution(lastNode,endNode,endNodeInfo.minDriveTime,route);
    }
    
    private class ShortestPathElement {
        final NodeAndState from;
        final NodeAndState to;
        final DirectedEdge via;

        public ShortestPathElement(NodeAndState from, NodeAndState to, DirectedEdge via) {
            Preconditions.checkNoneNull(from,to,via);
            this.from = from;
            this.to = to;
            this.via = via;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.from);
            hash = 37 * hash + Objects.hashCode(this.to);
            hash = 37 * hash + Objects.hashCode(this.via);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final ShortestPathElement other = (ShortestPathElement) obj;
            return  Objects.equals(this.from, other.from)
                    && Objects.equals(this.to, other.to)
                    && Objects.equals(this.via, other.via);
        }
        
        public String toString() {
            return "Via:" + via + " restrictions " + from + " to " + to;
        }
    }
    
}
