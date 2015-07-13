
package uk.me.mjt.ch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import uk.me.mjt.ch.PartialSolution.DownwardSolution;
import uk.me.mjt.ch.PartialSolution.UpwardSolution;


public class ContractedDijkstra {
    
    public static DijkstraSolution contractedGraphDijkstra(HashMap<Long, Node> allNodes, Node startNode, Node endNode) {
        Preconditions.checkNoneNull(allNodes, startNode, endNode);
        UpwardSolution upwardSolution = calculateUpwardSolution(allNodes, startNode);
        DownwardSolution downwardSolution = calculateDownwardSolution(allNodes, endNode);
        return mergeUpwardAndDownwardSolutions(upwardSolution, downwardSolution);
    }
    
    public static DijkstraSolution mergeUpwardAndDownwardSolutions(UpwardSolution upwardSolution, DownwardSolution downwardSolution) {
        List<DijkstraSolution> upDs = upwardSolution.getIndividualNodeSolutions();
        List<DijkstraSolution> downDs = downwardSolution.getIndividualNodeSolutions();
        
        int upIdx = 0;
        int downIdx = 0;
        //DijkstraSolution shortestSolution = null;
        int shortestSolutionDriveTime = Integer.MAX_VALUE;
        DijkstraSolution shortestSolutionUp = null;
        DijkstraSolution shortestSolutionDown = null;
        
        while (upIdx<upDs.size() && downIdx<downDs.size()) {
            DijkstraSolution up = upDs.get(upIdx);
            DijkstraSolution down = downDs.get(downIdx);
            long upContractionOrder = upDs.get(upIdx).getLastNode().contractionOrder;
            long downContractionOrder = downDs.get(downIdx).getLastNode().contractionOrder;
            
            if (upContractionOrder==downContractionOrder) {
                if (up.totalDriveTime + down.totalDriveTime < shortestSolutionDriveTime) {
                    shortestSolutionDriveTime = up.totalDriveTime + down.totalDriveTime;
                    shortestSolutionUp = up;
                    shortestSolutionDown = down;
                }
                downIdx++;
                upIdx++;
            } else if (upContractionOrder > downContractionOrder) {
                downIdx++;
            } else {
                upIdx++;
            }
        }
        
        if (shortestSolutionDriveTime == Integer.MAX_VALUE) {
            return null;
        }
        
        return unContract(upThenDown(shortestSolutionUp,shortestSolutionDown));
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
    
    /**
     * Take in a solution with some shortcut edges / contracted nodes and 
     * convert to the equivalent non-contracted solution.
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
    
    public static UpwardSolution calculateUpwardSolution(HashMap<Long, Node> allNodes, Node startNode) {
        List<DijkstraSolution> upwardSolutions = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, null, Float.POSITIVE_INFINITY, Dijkstra.Direction.FORWARDS);
        return new UpwardSolution(startNode, upwardSolutions);
    }
    
    public static DownwardSolution calculateDownwardSolution(HashMap<Long, Node> allNodes, Node endNode) {
        List<DijkstraSolution> downwardSolutions = Dijkstra.dijkstrasAlgorithm(allNodes, endNode, null, Float.POSITIVE_INFINITY, Dijkstra.Direction.BACKWARDS);
        return new DownwardSolution(endNode, downwardSolutions);
    }

}
