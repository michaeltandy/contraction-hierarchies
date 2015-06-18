package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    
    public static HashMap<Node,HashMap<Node,DijkstraSolution>> contractedGraphDijkstra(final HashMap<Long,Node> allNodes, Collection<Node> startNodes, Collection<Node> endNodes ) {
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
    }

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
                if (shortestSolution == null || up.totalDriveTime+down.totalDriveTime < shortestSolution.totalDriveTime) {
                    shortestSolution = upThenDown(up,down);
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
                    break;
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
        Node thisNode = endNode;
        int totalDriveTime = minDriveTime.get(endNode);
        
        LinkedList<Node> nodes = new LinkedList();
        LinkedList<DirectedEdge> edges = new LinkedList();

        while (thisNode != null) {
            nodes.addFirst(thisNode);
            if (minTimeVia.containsKey(thisNode))
                edges.addFirst(minTimeVia.get(thisNode));
            thisNode = minTimeFrom.get(thisNode);
        }
        
        if (nodes.isEmpty()) {
            System.out.println("Created empty solution?!?!");
        }
        
        return new DijkstraSolution(totalDriveTime, nodes, edges);
    }
}
