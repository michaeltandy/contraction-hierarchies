
package uk.me.mjt.ch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import uk.me.mjt.ch.cache.PartialSolutionCache;


public class ContractedDjikstra {

    private static UpAndDownPair getOrCalculateUpDownPair(HashMap<Long, Node> allNodes, Node startEndNode, PartialSolutionCache cache) {
        UpAndDownPair udp = cache.getIfPresent(startEndNode);
        if (udp == null) {
            udp = calculateUpDownPair(allNodes, startEndNode);
            cache.put(startEndNode, udp);
        }
        return udp;
    }

    private static UpAndDownPair calculateUpDownPair(HashMap<Long, Node> allNodes, Node startEndNode) {
        List<DijkstraSolution> upwardSolutions = Dijkstra.dijkstrasAlgorithm(allNodes, startEndNode, null, Float.POSITIVE_INFINITY, Dijkstra.Direction.FORWARDS);
        List<DijkstraSolution> downwardSolutions = Dijkstra.dijkstrasAlgorithm(allNodes, startEndNode, null, Float.POSITIVE_INFINITY, Dijkstra.Direction.BACKWARDS);
        return new UpAndDownPair(upwardSolutions, downwardSolutions);
    }

    /**
     * Take in a solution of some contracted nodes, and make a new solution with
     * them uncontracted.
     * @param ds
     * @return
     */
    private static DijkstraSolution unContract(DijkstraSolution ds) {
        if (ds == null) {
            return null;
        }
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
    
    public static DijkstraSolution contractedGraphDijkstra(HashMap<Long, Node> allNodes, Node startNode, Node endNode, PartialSolutionCache cache) {
        List<DijkstraSolution> upwardSolutions = getOrCalculateUpDownPair(allNodes, startNode, cache).up;
        List<DijkstraSolution> downwardSolutions = getOrCalculateUpDownPair(allNodes, endNode, cache).down;
        return mergeUpwardAndDownwardSolutions(upwardSolutions, downwardSolutions);
    }

    public static DijkstraSolution contractedGraphDijkstra(HashMap<Long, Node> allNodes, Node startNode, Node endNode) {
        Preconditions.checkNoneNull(allNodes, startNode, endNode);
        List<DijkstraSolution> upwardSolutions = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, null, Float.POSITIVE_INFINITY, Dijkstra.Direction.FORWARDS);
        List<DijkstraSolution> downwardSolutions = Dijkstra.dijkstrasAlgorithm(allNodes, endNode, null, Float.POSITIVE_INFINITY, Dijkstra.Direction.BACKWARDS);
        return mergeUpwardAndDownwardSolutions(upwardSolutions, downwardSolutions);
    }

    private static DijkstraSolution mergeUpwardAndDownwardSolutions(List<DijkstraSolution> upwardSolutions, List<DijkstraSolution> downwardSolutions) {
        HashMap<Node, DijkstraSolution> upwardPaths = new HashMap<Node, DijkstraSolution>();
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
        int totalDriveTime = up.totalDriveTime + down.totalDriveTime;
        LinkedList<Node> nodes = new LinkedList();
        nodes.addAll(up.nodes);
        for (int i = down.nodes.size() - 1; i >= 0; i--) {
            nodes.add(down.nodes.get(i));
        }
        LinkedList<DirectedEdge> edges = new LinkedList();
        edges.addAll(up.edges);
        for (int i = down.edges.size() - 1; i >= 0; i--) {
            edges.add(down.edges.get(i));
        }
        return new DijkstraSolution(totalDriveTime, nodes, edges);
    }

}
