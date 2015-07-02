package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.teneighty.heap.FibonacciHeap;
import org.teneighty.heap.Heap;

public class Dijkstra {

    public static DijkstraSolution dijkstrasAlgorithm(HashMap<Long,Node> allNodes, Node startNode, Node endNode, Direction direction) {
        HashSet<Node> hs = new HashSet<Node>(1);
        hs.add(endNode);
        List<DijkstraSolution> solutions = dijkstrasAlgorithm(allNodes, startNode, hs, Float.POSITIVE_INFINITY, direction);
        if (solutions.size() > 0)
            return solutions.get(0);
        else
            return null;
    }

    public enum Direction{FORWARDS,BACKWARDS};
    
    /*public static HashMap<Node,HashMap<Node,DijkstraSolution>> contractedGraphDijkstra(final HashMap<Long,Node> allNodes, Collection<Node> startNodes, Collection<Node> endNodes ) {
        Preconditions.checkNoneNull(allNodes,startNodes,endNodes);
        
        ExecutorService es = Executors.newFixedThreadPool(4);
        
        HashMap<Node,Future<List<DijkstraSolution>>> upwardSolutions = new HashMap();
        for (final Node startNode : startNodes) {
            Future<List<DijkstraSolution>> future = es.submit(new Callable() {
                @Override public List<DijkstraSolution> call() throws Exception {
                    return dijkstrasAlgorithm(allNodes, startNode, null, Float.POSITIVE_INFINITY, Direction.FORWARDS);
                }
            });
            upwardSolutions.put(startNode, future);
        }
        
        HashMap<Node,Future<List<DijkstraSolution>>> downwardSolutions = new HashMap();
        for (final Node endNode : endNodes) {
            Future<List<DijkstraSolution>> future = es.submit(new Callable() {
                @Override public List<DijkstraSolution> call() throws Exception {
                    return dijkstrasAlgorithm(allNodes, endNode, null, Float.POSITIVE_INFINITY, Direction.BACKWARDS);
                }
            });
            downwardSolutions.put(endNode, future);
        }
        
        try {
            es.shutdown();
            es.awaitTermination(10, TimeUnit.MINUTES);
        
            HashMap<Node,HashMap<Node,DijkstraSolution>> result = new HashMap<>();
            
            for (Node startNode : startNodes) {
                HashMap<Node,DijkstraSolution> thisStart = new HashMap<>();
                for (Node endNode : endNodes) {
                    List<DijkstraSolution> up = upwardSolutions.get(startNode).get();
                    List<DijkstraSolution> down = downwardSolutions.get(endNode).get();
                    DijkstraSolution ds = mergeUpwardAndDownwardSolutions(up,down);
                    thisStart.put(endNode, ds);
                }
                result.put(startNode, thisStart);
            }
            return result;
            
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }*/
    
    public static DijkstraSolution contractedGraphDijkstra(HashMap<Long,Node> allNodes, Node startNode, Node endNode ) {
        Preconditions.checkNoneNull(allNodes,startNode,endNode);
        List<DijkstraSolution> upwardSolutions = dijkstrasAlgorithm(allNodes, startNode, null, Float.POSITIVE_INFINITY, Direction.FORWARDS);
        List<DijkstraSolution> downwardSolutions = dijkstrasAlgorithm(allNodes, endNode, null, Float.POSITIVE_INFINITY, Direction.BACKWARDS);

        return mergeUpwardAndDownwardSolutions(upwardSolutions,downwardSolutions);
    }
    
    private static DijkstraSolution mergeUpwardAndDownwardSolutions(List<DijkstraSolution> upwardSolutions,List<DijkstraSolution> downwardSolutions) {
        HashMap<Node,DijkstraSolution> upwardPaths = new HashMap<Node,DijkstraSolution>();

        for (DijkstraSolution ds : upwardSolutions) {
            upwardPaths.put(ds.getLastNode(), ds);
        }
        
        DijkstraSolution shortestSolution = null;

        for (DijkstraSolution down : downwardSolutions) {
            if (down.nodes.isEmpty()) {
                System.out.println("Empty solution? " + down);
            } else {
                Node n = down.getLastNode();
                if (upwardPaths.containsKey(n)) {
                    DijkstraSolution up = upwardPaths.get(n);
                    if (shortestSolution == null || up.totalDriveTime + down.totalDriveTime < shortestSolution.totalDriveTime) {
                        shortestSolution = upThenDown(up, down);
                    }
                }
            }
        }

        return unContract(shortestSolution);
    }
    
    private static DijkstraSolution upThenDown(DijkstraSolution up, DijkstraSolution down) {
        int totalDriveTime = up.totalDriveTime+down.totalDriveTime;
        
        LinkedList<Node> nodes = new LinkedList();
        nodes.addAll(up.nodes);
        for (int i=down.nodes.size()-1 ; i>=0 ; i--) {
            nodes.add(down.nodes.get(i));
        }
        
        LinkedList<DirectedEdge> edges = new LinkedList();
        edges.addAll(up.edges);
        for (int i=down.edges.size()-1 ; i>=0 ; i--) {
            edges.add(down.edges.get(i));
        }
        
        return new DijkstraSolution(totalDriveTime, nodes, edges);
    }

    /**
     * Take in a solution of some contracted nodes, and make a new solution with
     * them uncontracted.
     * @param ds
     * @return
     */
    public static DijkstraSolution unContract(DijkstraSolution ds) {
        if (ds == null) return null;
        
        int totalDriveTime = ds.totalDriveTime;
        LinkedList<Node> nodes = new LinkedList();
        LinkedList<DirectedEdge> edges = new LinkedList();
        
        for (DirectedEdge de : ds.edges) {
            edges.addAll(de.getUncontractedEdges());
        }
        nodes.add(ds.getFirstNode());
        for (DirectedEdge de : edges) {
            nodes.add(de.to);
        }
        return new DijkstraSolution(totalDriveTime, nodes, edges);
    }
    
    private static final int DEFAULT_SET_SIZE = 4096;

    /**
     * dijkstrasAlgorithm performs a best-first graph search starting at startNode
     * and continuing until all endNodes have been reached, or until the best
     * solution has a drive time greater than maxSearchTime, whichever happens
     * first.
     */
    public static List<DijkstraSolution> dijkstrasAlgorithm(HashMap<Long,Node> allNodes, Node startNode, HashSet<Node> endNodes, float maxSearchTime, Direction direction ) {
        Preconditions.checkNoneNull(allNodes,startNode,direction);
        HashMap<Node,NodeInfo> nodeInfo = new HashMap<>(DEFAULT_SET_SIZE);
        ArrayList<DijkstraSolution> solutions = new ArrayList<>(DEFAULT_SET_SIZE);

        FibonacciHeap<DistanceOrder,Node> unvisitedNodes = new FibonacciHeap<>();
        Heap.Entry<DistanceOrder,Node> startHeapEntry = unvisitedNodes.insert(new DistanceOrder(startNode.nodeId,0), startNode);
        
        NodeInfo startNodeInfo = new NodeInfo();
        startNodeInfo.minDriveTime = 0;
        startNodeInfo.heapEntry = startHeapEntry;
        nodeInfo.put(startNode, startNodeInfo);
        
        while (!unvisitedNodes.isEmpty()) {
            // Find the node with the shortest drive time so far:
            Heap.Entry<DistanceOrder,Node> minHeapEntry = unvisitedNodes.extractMinimum();
            Node shortestTimeNode = minHeapEntry.getValue();
            NodeInfo thisNodeInfo = nodeInfo.get(shortestTimeNode);
            
            if (thisNodeInfo.minDriveTime > maxSearchTime)
                break;

            if (endNodes == null) {
                solutions.add(extractShortest(shortestTimeNode, nodeInfo));
            } else if (endNodes.contains(shortestTimeNode)) {
                solutions.add(extractShortest(shortestTimeNode, nodeInfo));
                if (solutions.size() == endNodes.size())
                    return solutions;
            }
            
            thisNodeInfo.visited = true;
            thisNodeInfo.heapEntry = null;

            for (DirectedEdge edge : (direction == Direction.FORWARDS ? shortestTimeNode.edgesFrom : shortestTimeNode.edgesTo)) {
                Node n = (direction == Direction.FORWARDS ? edge.to : edge.from);
                if (n.contractionOrder < shortestTimeNode.contractionOrder)
                    break;
                
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
                    
                    DistanceOrder newDistOrder = new DistanceOrder(n.nodeId, newTime);
                    if (neighborNodeInfo.heapEntry == null) {
                        neighborNodeInfo.heapEntry = unvisitedNodes.insert(newDistOrder, n);
                    } else {
                        unvisitedNodes.decreaseKey(neighborNodeInfo.heapEntry, newDistOrder);
                    }
                    
                }
            }
        }
        
        return solutions;
    }

    private static final class DistanceOrder implements Comparable<DistanceOrder> {
        private final long nodeId;
        private final int minDriveTime;

        public DistanceOrder(long nodeId, int minDriveTime) {
            this.nodeId = nodeId;
            this.minDriveTime = minDriveTime;
        }
        
        @Override
        public int compareTo(DistanceOrder that) {
            if (this.minDriveTime < that.minDriveTime) {
                return -1;
            } else if (this.minDriveTime > that.minDriveTime) {
                return 1;
            } else {
                return Long.compare(this.nodeId,that.nodeId);
            }
        }
    }
        
    
    private static final class NodeInfo {
        boolean visited = false;
        int minDriveTime = Integer.MAX_VALUE;
        Node minTimeFrom = null;
        DirectedEdge minTimeVia = null;
        Heap.Entry<DistanceOrder,Node> heapEntry = null;
        DijkstraSolution solution = null;
    }

    private static DijkstraSolution extractShortest(final Node endNode, HashMap<Node,NodeInfo> nodeInfo) {
        NodeInfo endNodeInfo = nodeInfo.get(endNode);
        int totalDriveTime = endNodeInfo.minDriveTime;
        
        List<Node> nodes = new LinkedList();
        List<DirectedEdge> edges = new LinkedList();
        
        Node thisNode = endNode;
        while (thisNode != null) {
            NodeInfo thisNodeInfo = nodeInfo.get(thisNode);
            if (thisNodeInfo.solution == null) {
                nodes.add(0, thisNode);
                if (thisNodeInfo.minTimeVia != null)
                    edges.add(0,thisNodeInfo.minTimeVia);
                thisNode = thisNodeInfo.minTimeFrom;
            } else {
                endNodeInfo.solution = new DijkstraSolution(totalDriveTime, nodes, edges, thisNodeInfo.solution);
                return endNodeInfo.solution;
            }
        }
        
        if (nodes.isEmpty()) {
            System.out.println("Created empty solution?!?!");
        }
        
        endNodeInfo.solution = new DijkstraSolution(totalDriveTime, nodes, edges);
        return endNodeInfo.solution;
    }
}
