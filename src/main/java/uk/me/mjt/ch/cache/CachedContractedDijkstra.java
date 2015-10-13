
package uk.me.mjt.ch.cache;

import java.util.HashMap;
import java.util.concurrent.Callable;
import uk.me.mjt.ch.ContractedDijkstra;
import uk.me.mjt.ch.DijkstraSolution;
import uk.me.mjt.ch.MapData;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.PartialSolution;


public class CachedContractedDijkstra {

    private static UpAndDownPair getOrCalculateUpDownPair(MapData allNodes, Node startEndNode, PartialSolutionCache cache) {
        UpAndDownPair udp = cache.getIfPresent(startEndNode);
        if (udp == null) {
            udp = calculateUpDownPair(allNodes, startEndNode);
            cache.put(startEndNode, udp);
        }
        return udp;
    }

    static UpAndDownPair calculateUpDownPair(MapData allNodes, Node startEndNode) {
        PartialSolution.UpwardSolution upwardSolution = ContractedDijkstra.calculateUpwardSolution(allNodes, startEndNode);
        PartialSolution.DownwardSolution downwardSolution = ContractedDijkstra.calculateDownwardSolution(allNodes, startEndNode);
        return new UpAndDownPair(upwardSolution, downwardSolution);
    }

    public static DijkstraSolution contractedGraphDijkstra(MapData allNodes, Node startNode, Node endNode, PartialSolutionCache cache) {
        UpAndDownPair startNodePair = getOrCalculateUpDownPair(allNodes, startNode, cache);
        UpAndDownPair endNodePair = getOrCalculateUpDownPair(allNodes, endNode, cache);
        return ContractedDijkstra.mergeUpwardAndDownwardSolutions(allNodes, startNodePair.up, endNodePair.down);
    }
    
    public static Callable<DijkstraSolution> callableContractedGraphDijkstra(final MapData allNodes, final Node startNode, final Node endNode, final PartialSolutionCache cache) {
        return new Callable<DijkstraSolution>() {
            public DijkstraSolution call() {
                return contractedGraphDijkstra(allNodes, startNode, endNode, cache);
            }
        };
    }

}
