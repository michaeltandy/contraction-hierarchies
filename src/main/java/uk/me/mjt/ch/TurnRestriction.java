
package uk.me.mjt.ch;

import java.util.*;


public class TurnRestriction {
    public enum TurnRestrictionType { NOT_ALLOWED, ONLY_ALLOWED }
    
    private final long turnRestrictionId;
    private final TurnRestrictionType type;
    private final List<Long> directedEdgeIds;
    
    public TurnRestriction(long turnRestrictionId, TurnRestrictionType type, List<Long> directedEdgeIds) {
        Preconditions.checkNoneNull(type, directedEdgeIds);
        this.turnRestrictionId = turnRestrictionId;
        this.type = type;
        this.directedEdgeIds = Collections.unmodifiableList(directedEdgeIds);
    }

    public long getTurnRestrictionId() {
        return turnRestrictionId;
    }

    public TurnRestrictionType getType() {
        return type;
    }

    public List<Long> getDirectedEdgeIds() {
        return directedEdgeIds;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (int) (this.turnRestrictionId ^ (this.turnRestrictionId >>> 32));
        hash = 41 * hash + Objects.hashCode(this.type);
        hash = 41 * hash + Objects.hashCode(this.directedEdgeIds);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TurnRestriction other = (TurnRestriction) obj;
        return this.turnRestrictionId == other.turnRestrictionId
                && this.type == other.type
                && Objects.equals(this.directedEdgeIds, other.directedEdgeIds);
    }
    
    public String toString() {
        return turnRestrictionId + "/" + type + "/" + directedEdgeIds;
    }
    
    public static void adjustGraphToImplementTurnRestrictions(MapData allNodes) {
        
        Multimap<Long,TurnRestriction> turnRestrictionsByEdge = indexRestrictionsByEdge(allNodes.allTurnRestrictions());
        List<TurnRestrictionCluster> clusters = findClusters(allNodes.getAllNodes(), turnRestrictionsByEdge);
        
        Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge = turnRestrictionsByStartEdge(allNodes.allTurnRestrictions());
        Node testFrom = allNodes.getNodeById(672630347L);
        Node testTo = allNodes.getNodeById(175683944L);
        HashSet<Node> toSet = new HashSet();
        toSet.add(testTo);
        DijkstraSolution ds = dijkstrasAlgorithm(testFrom, toSet, turnRestrictionsByStartEdge).get(0);
        System.out.println("asdfqwer " + ds);
        System.out.println(GeoJson.solution(ds));
        
        /*for (Long key : turnRestrictionsByEdge.keySet()) {
            List<TurnRestriction> tr = turnRestrictionsByEdge.get(key);
            if (tr.size() > 1) {
                System.out.println(key + " has " + tr.size() + " e.g. " + tr);
            }
        }*/
        
        for (TurnRestrictionCluster trc : clusters) {
            //adjustGraphForCluster(trc, turnRestrictionsByEdge);
        }
    }
    
    private static List<TurnRestrictionCluster> findClusters(Collection<Node> allNodes, Multimap<Long,TurnRestriction> turnRestrictionsByEdge) {
        Preconditions.checkNoneNull(allNodes, turnRestrictionsByEdge);
        
        HashSet<Node> alreadyAssignedToCluster = new HashSet();
        ArrayList<TurnRestrictionCluster> clusters = new ArrayList<>();
        
        for (Node n : allNodes) {
            if (anyEdgesTurnRestricted(n,turnRestrictionsByEdge) && !alreadyAssignedToCluster.contains(n)) {
                TurnRestrictionCluster cluster = identifyCluster(n,turnRestrictionsByEdge);
                alreadyAssignedToCluster.addAll(cluster.nodes);
                clusters.add(cluster);
            }
        }
        
        return Collections.unmodifiableList(clusters);
    }
    
    public static class TurnRestrictionCluster {
        final HashSet<Node> nodes = new HashSet();
    }
    
    private static Multimap<Long,TurnRestriction> indexRestrictionsByEdge(Collection<TurnRestriction> allRestrictions) {
        Multimap<Long,TurnRestriction> reverseIndex = new Multimap<>();
        for (TurnRestriction tr : allRestrictions) {
            for (Long edgeId : tr.getDirectedEdgeIds()) {
                reverseIndex.add(edgeId, tr);
            }
        }
        return reverseIndex;
    }
    
    private static Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge(Collection<TurnRestriction> allRestrictions) {
        Multimap<Long,TurnRestriction> reverseIndex = new Multimap<>();
        for (TurnRestriction tr : allRestrictions) {
            reverseIndex.add(tr.directedEdgeIds.get(0), tr);
        }
        return reverseIndex;
    }
    
    private static boolean anyEdgesTurnRestricted(Node n, Multimap<Long,TurnRestriction> turnRestrictionsByEdge) {
        for (DirectedEdge de : n.getEdgesFromAndTo()) {
            if (turnRestrictionsByEdge.containsKey(de.edgeId)) {
                return true;
            }
        }
        return false;
    }
    
    private static TurnRestrictionCluster identifyCluster(Node startPoint, Multimap<Long,TurnRestriction> turnRestrictionsByEdge) {
        Preconditions.checkNoneNull(startPoint);
        
        TurnRestrictionCluster cluster = new TurnRestrictionCluster();
        TreeSet<Node> toVisit = new TreeSet<>();
        toVisit.add(startPoint);
        
        while (!toVisit.isEmpty()) {
            Node visiting = toVisit.pollFirst();
            cluster.nodes.add(visiting);
            
            for (DirectedEdge de : visiting.getEdgesFromAndTo() ) {
                if (turnRestrictionsByEdge.containsKey(de.edgeId)) {
                    if (!cluster.nodes.contains(de.to))
                        toVisit.add(de.to);
                    if (!cluster.nodes.contains(de.from))
                        toVisit.add(de.from);
                }
            }
        }
        
        return cluster;
    }
    
    private static void adjustGraphForCluster(TurnRestrictionCluster cluster, Multimap<Long,TurnRestriction> turnRestrictionsByEdge) {
        
        System.out.println("=========");
        for (Node n : cluster.nodes) {
            //List<DijkstraSolution> shortPaths = Dijkstra.dijkstrasAlgorithm(n, cluster.nodes, Integer.MAX_VALUE, Dijkstra.Direction.FORWARDS, turnRestrictionsByEdge);
            //for (DijkstraSolution ds : shortPaths) {
            //    System.out.println("{\"type\": \"Feature\",\"properties\": {},\"geometry\":"+GeoJson.solution(ds)+"},");
            //}
        }
    }
    
    public static String edgesToGeojson(MapData mapData) {
        Collection<Node> allNodes = mapData.getAllNodes();
        Multimap<Long,TurnRestriction> turnRestrictionsByEdge = indexRestrictionsByEdge(mapData.allTurnRestrictions());
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"type\": \"FeatureCollection\", \"features\": [\n");
        
        for (Node n : allNodes) {
            for (DirectedEdge de : n.edgesFrom) {
                if (turnRestrictionsByEdge.containsKey(de.edgeId)) {
                    sb.append(GeoJson.singleDirectedEdge(de)).append(",\n");
                }
            }
        }
        
        if (sb.toString().endsWith(",\n"))
            sb.deleteCharAt(sb.length()-2);
        sb.append("]}");
        
        return sb.toString();
    }
    
        
    private static List<DijkstraSolution> dijkstrasAlgorithm(Node startNode, HashSet<Node> endNodes, Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge) {
        HashMap<NodeAndState,NodeInfo> nodeInfo = new HashMap<>();
        ArrayList<DijkstraSolution> solutions = new ArrayList<>();
        HashSet<Node> remainingNodes = new HashSet(endNodes);
        NodeAndState startState = new NodeAndState(startNode, new HashSet());
        
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
            
            if (remainingNodes.contains(shortestTimeNode.node)) {
                solutions.add(extractShortest(shortestTimeNode, nodeInfo));
                remainingNodes.remove(shortestTimeNode.node);
                if (remainingNodes.isEmpty())
                    return solutions;
            }
            
            thisNodeInfo.visited = true;
            thisNodeInfo.distanceOrder = null;
            
            for (DirectedEdge edge : shortestTimeNode.node.edgesFrom) {
                NodeAndState n = updateTurnRestrictionsIfLegal(thisNodeInfo.minTimeVia, shortestTimeNode, edge, turnRestrictionsByStartEdge);
                
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

    private static final class DistanceOrder implements Comparable<DistanceOrder> {
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
    
    private static final class NodeInfo {
        boolean visited = false;
        int minDriveTime = Integer.MAX_VALUE;
        NodeAndState minTimeFrom = null;
        DirectedEdge minTimeVia = null;
        DistanceOrder distanceOrder = null;
        DijkstraSolution solution = null;
    }
    
    private static class NodeAndState {
        final Node node;
        final HashSet<TurnRestriction> activeTurnRestrictions;

        public NodeAndState(Node node, HashSet<TurnRestriction> activeTurnRestrictions) {
            this.node = node;
            this.activeTurnRestrictions = activeTurnRestrictions;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.node);
            hash = 29 * hash + Objects.hashCode(this.activeTurnRestrictions);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final NodeAndState other = (NodeAndState) obj;
            return (Objects.equals(this.node, other.node) && Objects.equals(this.activeTurnRestrictions, other.activeTurnRestrictions));
        }
    }
    
    private static NodeAndState updateTurnRestrictionsIfLegal(DirectedEdge fromEdge, NodeAndState fromNode, DirectedEdge toEdge, Multimap<Long,TurnRestriction> turnRestrictionsByStartEdge) {
        Preconditions.checkNoneNull(fromNode,toEdge,turnRestrictionsByStartEdge);
        
        HashSet<TurnRestriction> turnRestrictionsAfter = new HashSet();
        turnRestrictionsAfter.addAll(turnRestrictionsByStartEdge.get(toEdge.edgeId));
        
        if (fromEdge == null) { // Start node.
            return new NodeAndState(toEdge.to, turnRestrictionsAfter);
        }
        
        for (TurnRestriction tr : fromNode.activeTurnRestrictions) {
            List<Long> edgeIds = tr.directedEdgeIds;
            int fromEdgeIdx = edgeIds.indexOf(fromEdge.edgeId);
            int toEdgeIdx = edgeIds.indexOf(toEdge.edgeId);
            
            boolean endOfRestriction = (toEdgeIdx==edgeIds.size()-1);
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
        
        return new NodeAndState(toEdge.to, turnRestrictionsAfter);
    }
    
    private static DijkstraSolution extractShortest(final NodeAndState endNode, HashMap<NodeAndState,NodeInfo> nodeInfo) {
        NodeInfo endNodeInfo = nodeInfo.get(endNode);
        if (endNodeInfo.solution != null)
            return endNodeInfo.solution;
        
        int totalDriveTime = endNodeInfo.minDriveTime;
        List<Node> nodes = new LinkedList();
        List<DirectedEdge> edges = new LinkedList();
        
        NodeAndState thisNode = endNode;
        while (thisNode != null) {
            NodeInfo thisNodeInfo = nodeInfo.get(thisNode);
            nodes.add(0, thisNode.node);
            if (thisNodeInfo.minTimeVia != null)
                edges.add(0,thisNodeInfo.minTimeVia);
            thisNode = thisNodeInfo.minTimeFrom;
        }
        
        endNodeInfo.solution = new DijkstraSolution(totalDriveTime, nodes, edges);
        return endNodeInfo.solution;
    }
    
}
