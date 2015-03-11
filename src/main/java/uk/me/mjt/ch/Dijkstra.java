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
        HashSet<Node> everyNode = new HashSet<Node>(allNodes.values());
        List<DijkstraSolution> upwardSolutions = dijkstrasAlgorithm(allNodes, startNode, everyNode, Float.POSITIVE_INFINITY, Direction.FORWARDS);

        HashMap<Node,DijkstraSolution> upwardPaths = new HashMap<Node,DijkstraSolution>();

        for (DijkstraSolution ds : upwardSolutions) {
            upwardPaths.put(ds.nodes.getLast(), ds);
        }

        List<DijkstraSolution> downwardSolutions = dijkstrasAlgorithm(allNodes, endNode, everyNode, Float.POSITIVE_INFINITY, Direction.BACKWARDS);

        DijkstraSolution shortestSolution = null;

        for (DijkstraSolution down : downwardSolutions) {
            Node n = down.nodes.getLast();
            if (upwardPaths.containsKey(n)) {
                DijkstraSolution up = upwardPaths.get(n);
                up.totalDistance += down.totalDistance;
                if (shortestSolution == null || up.totalDistance < shortestSolution.totalDistance) {
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
        newSolution.totalDistance = ds.totalDistance;
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
     * solution has a distance greater than maxSearchDist, whichever happens
     * first.
     */
    public static List<DijkstraSolution> dijkstrasAlgorithm(HashMap<Long,Node> allNodes, Node startNode, HashSet<Node> endNodes, float maxSearchDist, Direction direction ) {
        Preconditions.checkNoneNull(allNodes,startNode,endNodes,direction);
        //ArrayList<Node> visitedNodes = new ArrayList<Node>();
        HashSet<Node> visitedNodes = new HashSet<Node>();
        HashMap<Node,Float> minDistance = new HashMap<Node,Float>();
        HashMap<Node,Node> minDistFrom = new HashMap<Node,Node>();
        HashMap<Node,DirectedEdge> minDistVia = new HashMap<Node,DirectedEdge>();
        HashSet<Node> endNodesRemaining = (HashSet<Node>)endNodes.clone(); //

        ArrayList<DijkstraSolution> solutions = new ArrayList<DijkstraSolution>(endNodes.size());

        //ArrayList<Node> unvisitedNodes = new ArrayList<Node>();
        TreeSet<Node> unvisitedNodes = new TreeSet<Node>(new CompareNodes(minDistance));

        minDistance.put(startNode, 0.0f);
        unvisitedNodes.add(startNode);
        
        while (unvisitedNodes.size() > 0 && minDistance.get(unvisitedNodes.first()) <= maxSearchDist) {

            // Find the node with the shortest distance so far:
            Node shortestDistNode = unvisitedNodes.first();
            Float thisNodeDistance = minDistance.get(shortestDistNode);

            if (endNodesRemaining.contains(shortestDistNode)) {
                solutions.add(extractShortest(shortestDistNode, minDistance, minDistFrom, minDistVia));
                endNodesRemaining.remove(shortestDistNode);
                if (endNodesRemaining.isEmpty())
                    return solutions;
            }
            
            unvisitedNodes.remove(shortestDistNode);
            visitedNodes.add(shortestDistNode);

            for (DirectedEdge edge : (direction == Direction.FORWARDS ? shortestDistNode.edgesFrom : shortestDistNode.edgesTo)) {
                Node n = (direction == Direction.FORWARDS ? edge.to : edge.from);
                if (n.contractionOrder < shortestDistNode.contractionOrder)
                    continue;
                if (visitedNodes.contains(n))
                    continue;
                float newDist = thisNodeDistance + edge.distance;

                Float previousDist = minDistance.get(n);
                if (previousDist==null) previousDist = Float.POSITIVE_INFINITY;

                if (newDist < previousDist) {
                    unvisitedNodes.remove(n);
                    minDistance.put(n, newDist);
                    minDistFrom.put(n, shortestDistNode);
                    minDistVia.put(n, edge);
                    unvisitedNodes.add(n);
                }
            }
        }
        
        return solutions;
    }

    private static class CompareNodes implements Comparator<Node> {
        final HashMap<Node,Float> minDistance;

        CompareNodes(HashMap<Node,Float> minDistance) {
            this.minDistance = minDistance;
        }

        public int compare(Node o1, Node o2) {
            Float distA = minDistance.get(o1);
            distA = (distA==null?Float.POSITIVE_INFINITY:distA);

            Float distB = minDistance.get(o2);
            distB = (distB==null?Float.POSITIVE_INFINITY:distB);

            if (distA < distB) {
                return -1;
            } else if (distA > distB) {
                return 1;
            } else {
                return Long.compare(o1.nodeId,o2.nodeId);
            }
        }
    }

    /**
     * Prints a formatted string represening the results of dijkstrasAlgorithm
     * by repeatedly following minDistFrom until the peak is reached.
     * @param endNode, the furthest node from the peak on the route.
     * @return String of the format <pre>FR,XH,PEAK</pre>
     */
    public static DijkstraSolution extractShortest(Node endNode, HashMap<Node,Float> minDistance, HashMap<Node,Node> minDistFrom, HashMap<Node,DirectedEdge> minDistVia) {
        DijkstraSolution solution = new DijkstraSolution();

        Node thisNode = endNode;
        solution.totalDistance = minDistance.get(endNode);

        while (thisNode != null) {
            solution.nodes.addFirst(thisNode);
            if (minDistVia.containsKey(thisNode))
                solution.edges.addFirst(minDistVia.get(thisNode));
            thisNode = minDistFrom.get(thisNode);
        }
        return solution;
    }
}
