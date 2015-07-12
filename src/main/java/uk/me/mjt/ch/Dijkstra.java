package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.teneighty.heap.FibonacciHeap;
import org.teneighty.heap.Heap;

public class Dijkstra {
    
    public enum Direction{FORWARDS,BACKWARDS};
    private static final int DEFAULT_SET_SIZE = 4096;

    /**
     * Convenience method for a simple search on an uncontracted graph.
     */
    public static DijkstraSolution dijkstrasAlgorithm(HashMap<Long,Node> allNodes, Node startNode, Node endNode, Direction direction) {
        HashSet<Node> hs = new HashSet<>(1);
        hs.add(endNode);
        List<DijkstraSolution> solutions = dijkstrasAlgorithm(allNodes, startNode, hs, Float.POSITIVE_INFINITY, direction);
        if (solutions.size() > 0)
            return solutions.get(0);
        else
            return null;
    }
    
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
