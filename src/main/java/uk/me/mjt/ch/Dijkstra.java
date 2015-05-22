package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

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

    public static DijkstraSolution contractedGraphDijkstra(HashMap<Long,Node> allNodes, Node startNode, Node endNode ) {
        Preconditions.checkNoneNull(allNodes,startNode,endNode);
        List<DijkstraSolution> upwardSolutions = dijkstrasAlgorithm(allNodes, startNode, null, Float.POSITIVE_INFINITY, Direction.FORWARDS);

        HashMap<Node,DijkstraSolution> upwardPaths = new HashMap<Node,DijkstraSolution>();

        for (DijkstraSolution ds : upwardSolutions) {
            upwardPaths.put(ds.nodes.getLast(), ds);
        }

        List<DijkstraSolution> downwardSolutions = dijkstrasAlgorithm(allNodes, endNode, null, Float.POSITIVE_INFINITY, Direction.BACKWARDS);

        DijkstraSolution shortestSolution = null;

        for (DijkstraSolution down : downwardSolutions) {
            Node n = down.nodes.getLast();
            if (upwardPaths.containsKey(n)) {
                DijkstraSolution up = upwardPaths.get(n);
                up.totalDriveTime += down.totalDriveTime;
                if (shortestSolution == null || up.totalDriveTime < shortestSolution.totalDriveTime) {
                    down.nodes.removeLast();
                    while (!down.nodes.isEmpty()) {
                        up.nodes.addLast(down.nodes.getLast());
                        down.nodes.removeLast();
                        up.edges.addLast(down.edges.getLast());
                        down.edges.removeLast();
                    }
                    shortestSolution = up;
                }
            }
        }

        return unContract(shortestSolution);
    }

    /**
     * Take in a solution of some contracted nodes, and make a new solution with
     * them uncontracted.
     * @param ds
     * @return
     */
    public static DijkstraSolution unContract(DijkstraSolution ds) {
        if (ds == null) return null;
        DijkstraSolution newSolution = new DijkstraSolution();
        newSolution.totalDriveTime = ds.totalDriveTime;
        for (DirectedEdge de : ds.edges) {
            newSolution.edges.addAll(de.getUncontractedEdges());
        }
        newSolution.nodes.add(ds.nodes.getFirst());
        for (DirectedEdge de : newSolution.edges) {
            newSolution.nodes.add(de.to);
        }
        return newSolution;
    }

    /**
     * dijkstrasAlgorithm performs a best-first graph search starting at startNode
     * and continuing until all endNodes have been reached, or until the best
     * solution has a drive time greater than maxSearchTime, whichever happens
     * first.
     */
    public static List<DijkstraSolution> dijkstrasAlgorithm(HashMap<Long,Node> allNodes, Node startNode, HashSet<Node> endNodes, float maxSearchTime, Direction direction ) {
        Preconditions.checkNoneNull(allNodes,startNode,direction);
        HashSet<Node> visitedNodes = new HashSet<>();
        HashMap<Node,Integer> minDriveTime = new HashMap<>();
        HashMap<Node,Node> minTimeFrom = new HashMap<>();
        HashMap<Node,DirectedEdge> minTimeVia = new HashMap<>();
        ArrayList<DijkstraSolution> solutions = new ArrayList<>();

        TreeSet<Node> unvisitedNodes = new TreeSet<Node>(new CompareNodes(minDriveTime));

        minDriveTime.put(startNode, 0);
        unvisitedNodes.add(startNode);
        
        while (unvisitedNodes.size() > 0 && minDriveTime.get(unvisitedNodes.first()) <= maxSearchTime) {

            // Find the node with the shortest drive time so far:
            Node shortestTimeNode = unvisitedNodes.first();
            int thisNodeDriveTime = minDriveTime.get(shortestTimeNode);

            if (endNodes == null) {
                solutions.add(extractShortest(shortestTimeNode, minDriveTime, minTimeFrom, minTimeVia));
            } else if (endNodes.contains(shortestTimeNode)) {
                solutions.add(extractShortest(shortestTimeNode, minDriveTime, minTimeFrom, minTimeVia));
                if (solutions.size() == endNodes.size())
                    return solutions;
            }
            
            unvisitedNodes.remove(shortestTimeNode);
            visitedNodes.add(shortestTimeNode);

            for (DirectedEdge edge : (direction == Direction.FORWARDS ? shortestTimeNode.edgesFrom : shortestTimeNode.edgesTo)) {
                Node n = (direction == Direction.FORWARDS ? edge.to : edge.from);
                if (n.contractionOrder < shortestTimeNode.contractionOrder)
                    continue;
                if (visitedNodes.contains(n))
                    continue;
                int newTime = thisNodeDriveTime + edge.driveTimeMs;

                Integer previousTime = minDriveTime.get(n);
                if (previousTime==null) previousTime = Integer.MAX_VALUE;

                if (newTime < previousTime) {
                    unvisitedNodes.remove(n);
                    minDriveTime.put(n, newTime);
                    minTimeFrom.put(n, shortestTimeNode);
                    minTimeVia.put(n, edge);
                    unvisitedNodes.add(n);
                }
            }
        }
        
        return solutions;
    }

    private static class CompareNodes implements Comparator<Node> {
        final HashMap<Node,Integer> minDriveTime;

        CompareNodes(HashMap<Node,Integer> minDriveTime) {
            this.minDriveTime = minDriveTime;
        }

        public int compare(Node o1, Node o2) {
            Integer timeA = minDriveTime.get(o1);
            timeA = (timeA==null?Integer.MAX_VALUE:timeA);

            Integer timeB = minDriveTime.get(o2);
            timeB = (timeB==null?Integer.MAX_VALUE:timeB);

            if (timeA < timeB) {
                return -1;
            } else if (timeA > timeB) {
                return 1;
            } else {
                return Long.compare(o1.nodeId,o2.nodeId);
            }
        }
    }

    public static DijkstraSolution extractShortest(Node endNode, HashMap<Node,Integer> minDriveTime, HashMap<Node,Node> minTimeFrom, HashMap<Node,DirectedEdge> minTimeVia) {
        DijkstraSolution solution = new DijkstraSolution();

        Node thisNode = endNode;
        solution.totalDriveTime = minDriveTime.get(endNode);

        while (thisNode != null) {
            solution.nodes.addFirst(thisNode);
            if (minTimeVia.containsKey(thisNode))
                solution.edges.addFirst(minTimeVia.get(thisNode));
            thisNode = minTimeFrom.get(thisNode);
        }
        return solution;
    }
}
