
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
        throw new UnsupportedOperationException("Temporarily broken");
    }
    
    /*public static DijkstraSolution contractedGraphDijkstra(MapData allNodes, Node startNode, Node endNode, ExecutorService es) {
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
    
    private static Future<UpwardSolution> futureUpwardSolution(final MapData allNodes, final Node startNode, final ExecutorService es) {
        return es.submit(new Callable<UpwardSolution>() {
            @Override
            public UpwardSolution call() throws Exception {
                return calculateUpwardSolution(allNodes, startNode);
            }
        });
    }
    
    private static Future<DownwardSolution> futureDownwardSolution(final MapData allNodes, final Node endNode, final ExecutorService es) {
        return es.submit(new Callable<DownwardSolution>() {
            @Override
            public DownwardSolution call() throws Exception {
                return calculateDownwardSolution(allNodes, endNode);
            }
        });
    }*/
    
    public static DijkstraSolution contractedGraphDijkstra(MapData allNodes, Node startNode, Node endNode) {
        Preconditions.checkNoneNull(allNodes, startNode, endNode);
        return contractedGraphDijkstra(allNodes, ColocatedNodeSet.singleton(startNode), ColocatedNodeSet.singleton(endNode));
    }
    
    public static DijkstraSolution contractedGraphDijkstra(MapData allNodes, ColocatedNodeSet startNode, ColocatedNodeSet endNode) {
        Preconditions.checkNoneNull(allNodes, startNode, endNode);
        UpwardSolution upwardSolution = calculateUpwardSolution(startNode);
        DownwardSolution downwardSolution = calculateDownwardSolution(endNode);
        return mergeUpwardAndDownwardSolutions(allNodes, upwardSolution, downwardSolution);
    }
    
    public static DijkstraSolution mergeUpwardAndDownwardSolutions(MapData allNodes, UpwardSolution up, DownwardSolution down) {
        
        IntBuffer commonIndices = getCommonEntryIndices(up.getContractionOrderBuffer(),down.getContractionOrderBuffer(),up.getTotalDriveTimeBuffer(),down.getTotalDriveTimeBuffer());
        if (commonIndices.get(0) == -1)
            return null;
        
        int shortestUpIdx = commonIndices.get(0);
        int shortestDownIdx = commonIndices.get(1);
        
        DijkstraSolution shortestSolutionUp = up.getDijkstraSolution(allNodes, shortestUpIdx);
        DijkstraSolution shortestSolutionDown = down.getDijkstraSolution(allNodes, shortestDownIdx);
        return unContract(upThenDown(shortestSolutionUp,shortestSolutionDown));
    }
    
    /*
        10 repetitions cached pathing from hatfield to 4000 locations in 1479 ms.
        getCommonEntryIndicesCalls:   88013
        whileLoopIterations:      324083997
        matchedContractionOrders: 152378966
        replacedShortest:           1848114

        3682 loops per subroutine call
        Shared entries: 47.01%
        21 shortest replacements per subroutine call

        16.8 us per subroutine call
        4.5 ns per loop iteration -> average 18 clock cycles at 4GHz
     */
    private static IntBuffer getCommonEntryIndices(IntBuffer a, IntBuffer b, IntBuffer aTimes, IntBuffer bTimes) {
        IntBuffer result = IntBuffer.allocate(2);
        result.put(0,-1).put(1,-1);
        
        int shortestTime = Integer.MAX_VALUE;
        int aIdx = 0;
        int bIdx = 0;
        
        while (aIdx<a.limit() && bIdx<b.limit()) {
            int aValue = a.get(aIdx);
            int bValue = b.get(bIdx);
            
            if (aValue==bValue) {
                int aTime = aTimes.get(aIdx);
                int bTime = bTimes.get(bIdx);
                if (aTime+bTime < shortestTime) {
                    shortestTime = aTime+bTime;
                    result.put(0, aIdx);
                    result.put(1, bIdx);
                }
                
                bIdx++;
                aIdx++;
            } else if (aValue > bValue) {
                bIdx++;
            } else {
                aIdx++;
            }
        }
        
        return result;
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
    
    public static UpwardSolution calculateUpwardSolution(ColocatedNodeSet startNode) {
        List<DijkstraSolution> upwardSolutions = Dijkstra.dijkstrasAlgorithm(startNode, Dijkstra.Direction.FORWARDS);
        return new UpwardSolution(upwardSolutions);
    }
    
    public static DownwardSolution calculateDownwardSolution(ColocatedNodeSet endNode) {
        List<DijkstraSolution> downwardSolutions = Dijkstra.dijkstrasAlgorithm(endNode, Dijkstra.Direction.BACKWARDS);
        return new DownwardSolution(downwardSolutions);
    }

}
