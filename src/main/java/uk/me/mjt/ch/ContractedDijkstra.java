
package uk.me.mjt.ch;

import java.nio.IntBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import uk.me.mjt.ch.PartialSolution.DownwardSolution;
import uk.me.mjt.ch.PartialSolution.UpwardSolution;


public class ContractedDijkstra {
    
    public static DijkstraSolution contractedGraphDijkstra(MapData allNodes, Node startNode, Node endNode, ExecutorService es) {
        Preconditions.checkNoneNull(allNodes, startNode, endNode);
        Future<UpwardSolution> fUpwardSolution = futureUpwardSolution(allNodes, startNode, es);
        Future<DownwardSolution> fDownwardSolution = futureDownwardSolution(allNodes, endNode, es);
        
        return mergeUpwardAndDownwardSolutions(allNodes, getFutureQuietly(fUpwardSolution), getFutureQuietly(fDownwardSolution));
    }
    
    private static <E> E getFutureQuietly(Future<E> f) {
        try {
            return f.get();
        } catch (ExecutionException|InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Future<UpwardSolution> futureUpwardSolution(final MapData allNodes, final Node startNode, final ExecutorService es) {
        return es.submit(new Callable<UpwardSolution>() {
            @Override
            public UpwardSolution call() throws Exception {
                return calculateUpwardSolution(allNodes, startNode);
            }
        });
    }
    
    public static Future<DownwardSolution> futureDownwardSolution(final MapData allNodes, final Node endNode, final ExecutorService es) {
        return es.submit(new Callable<DownwardSolution>() {
            @Override
            public DownwardSolution call() throws Exception {
                return calculateDownwardSolution(allNodes, endNode);
            }
        });
    }
    
    public static DijkstraSolution contractedGraphDijkstra(MapData allNodes, Node startNode, Node endNode) {
        Preconditions.checkNoneNull(allNodes, startNode, endNode);
        UpwardSolution upwardSolution = calculateUpwardSolution(allNodes, startNode);
        DownwardSolution downwardSolution = calculateDownwardSolution(allNodes, endNode);
        return mergeUpwardAndDownwardSolutions(allNodes, upwardSolution, downwardSolution);
    }
    
    public static DijkstraSolution mergeUpwardAndDownwardSolutions(MapData allNodes, UpwardSolution up, DownwardSolution down) {
        int upLength = up.getSize();
        int downLength = down.getSize();
        IntBuffer upCO = up.getContractionOrderBuffer();
        IntBuffer downCO = down.getContractionOrderBuffer();
        
        int upIdx = 0;
        int downIdx = 0;
        
        int shortestSolutionDriveTime = Integer.MAX_VALUE;
        int shortestUpIdx = -1;
        int shortestDownIdx = -1;
        
        while (upIdx<upLength && downIdx<downLength) {
            int upContractionOrder = upCO.get(upIdx);
            int downContractionOrder = downCO.get(downIdx);
            
            if (upContractionOrder==downContractionOrder) {
                int upTotalDriveTime = up.getTotalDriveTime(upIdx);
                int downTotalDriveTime = down.getTotalDriveTime(downIdx);
                if (upTotalDriveTime + downTotalDriveTime < shortestSolutionDriveTime) {
                    shortestSolutionDriveTime = upTotalDriveTime + downTotalDriveTime;
                    shortestUpIdx = upIdx;
                    shortestDownIdx = downIdx;
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
        
        DijkstraSolution shortestSolutionUp = up.getDijkstraSolution(allNodes, shortestUpIdx);
        DijkstraSolution shortestSolutionDown = down.getDijkstraSolution(allNodes, shortestDownIdx);
        return unContract(upThenDown(shortestSolutionUp,shortestSolutionDown));
    }

    private static DijkstraSolution upThenDown(DijkstraSolution up, DijkstraSolution down) {
        int totalDriveTime = up.totalDriveTimeMs + down.totalDriveTimeMs;
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
        int totalDriveTime = ds.totalDriveTimeMs;
        List<DirectedEdge> edges = Collections.EMPTY_LIST;
        for (DirectedEdge de : ds.edges) {
            edges = new UnionList<>(edges,de.getUncontractedEdges());
        }
        
        List<Node> nodes;
        if (edges.isEmpty()) {
            nodes = Collections.singletonList(ds.getFirstNode());
        } else {
            nodes = new NodeListFromEdgeList(edges);
        }
        return new DijkstraSolution(totalDriveTime, nodes, edges);
    }
    
    public static UpwardSolution calculateUpwardSolution(MapData allNodes, Node startNode) {
        List<DijkstraSolution> upwardSolutions = Dijkstra.dijkstrasAlgorithm(startNode, null, Integer.MAX_VALUE, Dijkstra.Direction.FORWARDS);
        return new UpwardSolution(upwardSolutions);
    }
    
    public static DownwardSolution calculateDownwardSolution(MapData allNodes, Node endNode) {
        List<DijkstraSolution> downwardSolutions = Dijkstra.dijkstrasAlgorithm(endNode, null, Integer.MAX_VALUE, Dijkstra.Direction.BACKWARDS);
        return new DownwardSolution(downwardSolutions);
    }

}
