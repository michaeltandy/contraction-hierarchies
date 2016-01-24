package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class Dijkstra {
    
    public enum Direction{FORWARDS,BACKWARDS};
    private enum EndAfterFinding{ALL,ONE,EXHAUSTED};
    
    private static final int DEFAULT_SET_SIZE = 4096;
    
    /**
     * Convenience method for a one-to-one search on an uncontracted graph.
     */
    public static DijkstraSolution dijkstrasAlgorithm(Node startNode, Node endNode, Direction direction) {
        Preconditions.checkNoneNull(startNode,endNode,direction);
        HashSet<Node> hs = new HashSet<>(1);
        hs.add(endNode);
        List<DijkstraSolution> solutions = dijkstrasAlgorithm(startNode, hs, Integer.MAX_VALUE, direction);
        if (solutions.size() == 1)
            return solutions.get(0);
        else
            return null;
    }
    
    public static DijkstraSolution dijkstrasAlgorithm(ColocatedNodeSet startNode, ColocatedNodeSet endNode, Direction direction) {
        Preconditions.checkNoneNull(startNode,endNode,direction);
        List<DijkstraSolution> solutions = dijkstrasAlgorithm(startNode, endNode, Integer.MAX_VALUE, direction, EndAfterFinding.ONE);
        if (solutions.size() == 1)
            return solutions.get(0);
        else
            return null;
    }
    
    // Called for the two halves of a contracted dijkstra.
    public static List<DijkstraSolution> dijkstrasAlgorithm(ColocatedNodeSet startNode, Direction direction) {
        Preconditions.checkNoneNull(startNode,direction);
        return dijkstrasAlgorithm(startNode, null, Integer.MAX_VALUE, direction, EndAfterFinding.EXHAUSTED);
    }
    
    public static List<DijkstraSolution> dijkstrasAlgorithm(Node startNode, HashSet<Node> endNodes, int maxSearchTime, Direction direction ) {
        return dijkstrasAlgorithm(ColocatedNodeSet.singleton(startNode), endNodes, maxSearchTime, direction, EndAfterFinding.ALL);
    }
    
    /**
     * dijkstrasAlgorithm performs a best-first graph search starting at startNode
     * and continuing until all endNodes have been reached, or until the best
     * solution has a drive time greater than maxSearchTime, whichever happens
     * first.
     */
    private static List<DijkstraSolution> dijkstrasAlgorithm(ColocatedNodeSet startNodes, Set<Node> endNodes, int maxSearchTime, Direction direction, EndAfterFinding endCondition) {
        Preconditions.checkNoneNull(startNodes,direction);
        Preconditions.require(!startNodes.isEmpty());
        HashMap<Node,NodeInfo> nodeInfo = new HashMap<>(DEFAULT_SET_SIZE);
        ArrayList<DijkstraSolution> solutions = new ArrayList<>(DEFAULT_SET_SIZE);

        PriorityQueue<DistanceOrder> unvisitedNodes = new PriorityQueue<>();
        
        for (Node startNode : startNodes) {
            DistanceOrder startDo = new DistanceOrder(0,startNode);
            unvisitedNodes.add(startDo);

            NodeInfo startNodeInfo = new NodeInfo();
            startNodeInfo.minDriveTime = 0;
            startNodeInfo.distanceOrder = startDo;
            nodeInfo.put(startNode, startNodeInfo);
        }
        
        while (!unvisitedNodes.isEmpty()) {
            // Find the node with the shortest drive time so far:
            DistanceOrder minHeapEntry = unvisitedNodes.poll();
            Node shortestTimeNode = minHeapEntry.node;
            NodeInfo thisNodeInfo = nodeInfo.get(shortestTimeNode);
            
            if (thisNodeInfo.minDriveTime > maxSearchTime)
                break;

            if (endNodes == null) {
                solutions.add(extractShortest(shortestTimeNode, nodeInfo));
            } else if (endNodes.contains(shortestTimeNode)) {
                solutions.add(extractShortest(shortestTimeNode, nodeInfo));
                if (solutions.size() == endNodes.size() || endCondition==EndAfterFinding.ONE)
                    return solutions;
            }
            
            thisNodeInfo.visited = true;
            thisNodeInfo.distanceOrder = null;

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
        public final Node node;

        public DistanceOrder(int minDriveTime, Node node) {
            this.minDriveTime = minDriveTime;
            this.node = node;
        }
        
        @Override
        public int compareTo(DistanceOrder that) {
            if (this.minDriveTime < that.minDriveTime) {
                return -1;
            } else if (this.minDriveTime > that.minDriveTime) {
                return 1;
            } else {
                return Long.compare(this.node.nodeId,that.node.nodeId);
            }
        }
    }
        
    
    private static final class NodeInfo {
        boolean visited = false;
        int minDriveTime = Integer.MAX_VALUE;
        Node minTimeFrom = null;
        DirectedEdge minTimeVia = null;
        DistanceOrder distanceOrder = null;
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
