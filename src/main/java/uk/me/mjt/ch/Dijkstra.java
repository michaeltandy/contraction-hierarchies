package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

public class Dijkstra {
    
    private static final int EDGEID = -12345;

    public static DirectedEdge dijkstrasAlgorithm(HashMap<Long,Node> allNodes, Node startNode, Node endNode, Direction direction) {
        HashSet<Node> hs = new HashSet<Node>(1);
        hs.add(endNode);
        List<DirectedEdge> solutions = dijkstrasAlgorithm(allNodes, startNode, hs, Integer.MAX_VALUE, direction);
        if (solutions.size() > 0)
            return solutions.get(0);
        else
            return null;
    }

    public enum Direction{FORWARDS,BACKWARDS};

    public static DijkstraSolution contractedGraphDijkstra(HashMap<Long,Node> allNodes, Node startNode, Node endNode ) {
        Preconditions.checkNoneNull(allNodes,startNode,endNode);
        HashSet<Node> everyNode = new HashSet<Node>(allNodes.values());
        List<DijkstraSolution> upwardSolutions = null;//dijkstrasAlgorithm(allNodes, startNode, everyNode, Integer.MAX_VALUE, Direction.FORWARDS);

        HashMap<Node,DijkstraSolution> upwardPaths = new HashMap<Node,DijkstraSolution>();

        for (DijkstraSolution ds : upwardSolutions) {
            upwardPaths.put(ds.nodes.getLast(), ds);
        }

        List<DijkstraSolution> downwardSolutions = null;//dijkstrasAlgorithm(allNodes, endNode, everyNode, Integer.MAX_VALUE, Direction.BACKWARDS);

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
        /*if (ds == null) return null;
        DijkstraSolution newSolution = new DijkstraSolution();
        newSolution.totalDistance = ds.totalDistance;
        for (DirectedEdge de : ds.edges) {
            newSolution.edges.addAll(de.getUncontractedEdges());
        }
        newSolution.nodes.add(ds.nodes.getFirst());
        for (DirectedEdge de : newSolution.edges) {
            newSolution.nodes.add(de.to);
        }
        return newSolution;*/
        return null; // REVISIT
    }

    /**
     * dijkstrasAlgorithm performs a best-first graph search starting at startNode
     * and continuing until all endNodes have been reached, or until the best
     * solution has a distance greater than maxSearchDist, whichever happens
     * first.
     */
    public static List<DirectedEdge> dijkstrasAlgorithm(HashMap<Long,Node> allNodes, Node startNode, HashSet<Node> endNodes, int maxSearchDist, Direction direction ) {
        Preconditions.checkNoneNull(allNodes,startNode,endNodes,direction);
        HashMap<Node,DirectedEdge> bestDistances = new HashMap<>();
        TreeSet<Node> toProcess = new TreeSet<>(new CompareNodes(bestDistances));

        bestDistances.put(startNode, new DirectedEdge(EDGEID, startNode, startNode, 0) );
        markOutboundToProcess(toProcess,startNode);
        
        while (toProcess.size() > 0) {
            Node nodeBeingProcessed = toProcess.first();
            toProcess.remove(nodeBeingProcessed);
            
            DirectedEdge previousBest = bestDistances.get(nodeBeingProcessed);
            DirectedEdge newBest = findNewBest(nodeBeingProcessed, previousBest, bestDistances);
            
            if (previousBest == null || newBest.isEverLessThan(previousBest)) {
                bestDistances.put(nodeBeingProcessed, newBest);
                
                if (newBest.getMinTransitDuration() < maxSearchDist) {
                    markOutboundToProcess(toProcess,nodeBeingProcessed);
                }
                
                if (endNodes.contains(nodeBeingProcessed)) {
                    maxSearchDist = updateMaxSearchDist(maxSearchDist, endNodes, bestDistances);
                }
            }
        }
        
        ArrayList<DirectedEdge> solutions = new ArrayList<>(endNodes.size());
        for (Node n : endNodes) {
            if (bestDistances.containsKey(n)) {
                solutions.add(bestDistances.get(n));
            }
        }
        
        return solutions;
    }
    
    private static void markOutboundToProcess(TreeSet<Node> toProcess, Node n) {
        for (DirectedEdge de : n.edgesFrom) {
            toProcess.add(de.to);
        }
    }
    
    private static DirectedEdge findNewBest(Node target, DirectedEdge bestSoFar, HashMap<Node,DirectedEdge> allBests) {
        for (DirectedEdge thisOrigin : target.edgesTo) {
            // TODO deal with contracted nodes "right"
            if (allBests.containsKey(thisOrigin.from)) {
                DirectedEdge thisRoute = allBests.get(thisOrigin.from).plus(thisOrigin, EDGEID);
                bestSoFar = bestOf(bestSoFar,thisRoute);
            }
        }
        return bestSoFar;
    }
    
    private static int updateMaxSearchDist(int maxSearchDist, HashSet<Node> endNodes, HashMap<Node,DirectedEdge> bestDistances) {
        int largestRequiredSearchRadius = 0;
        for (Node n : endNodes) {
            if (bestDistances.containsKey(n)) {
                DirectedEdge thisDistance = bestDistances.get(n);
                largestRequiredSearchRadius = Math.max(largestRequiredSearchRadius, thisDistance.getMaxTransitDuration());
            } else {
                // At least one node hasn't been reached at all, so don't
                // tighten search radius.
                return maxSearchDist;
            }
        }
        return Math.min(maxSearchDist,largestRequiredSearchRadius);
    }
    
    private static DirectedEdge bestOf(DirectedEdge a, DirectedEdge b) {
        if (a==null) {
            return b;
        } else if (b==null) {
            return a;
        }
        boolean aSometimesBetter = a.isEverLessThan(b);
        boolean bSometimesBetter = b.isEverLessThan(a);
        if (aSometimesBetter && !bSometimesBetter) {
            return a;
        } else if (bSometimesBetter && !aSometimesBetter) {
            return b;
        } else if (aSometimesBetter && bSometimesBetter) {
            return a.minWith(b, EDGEID);
        } else { // They're equal.
            return a;
        }
    }

    private static class CompareNodes implements Comparator<Node> {
        final HashMap<Node,DirectedEdge> minDistance;

        CompareNodes(HashMap<Node,DirectedEdge> minDistance) {
            this.minDistance = minDistance;
        }

        public int compare(Node o1, Node o2) {
            DirectedEdge edgeA = minDistance.get(o1);
            int distA = (edgeA==null?0:edgeA.getMinTransitDuration());

            DirectedEdge edgeB = minDistance.get(o2);
            int distB = (edgeB==null?0:edgeB.getMinTransitDuration());

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
