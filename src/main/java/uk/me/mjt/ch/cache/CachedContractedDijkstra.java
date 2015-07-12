
package uk.me.mjt.ch.cache;

import java.util.HashMap;
import uk.me.mjt.ch.ContractedDijkstra;
import uk.me.mjt.ch.DijkstraSolution;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.PartialSolution;


public class CachedContractedDijkstra {

    private static UpAndDownPair getOrCalculateUpDownPair(HashMap<Long, Node> allNodes, Node startEndNode, PartialSolutionCache cache) {
        UpAndDownPair udp = cache.getIfPresent(startEndNode);
        if (udp == null) {
            udp = calculateUpDownPair(allNodes, startEndNode);
            cache.put(startEndNode, udp);
        }
        return udp;
    }

    private static UpAndDownPair calculateUpDownPair(HashMap<Long, Node> allNodes, Node startEndNode) {
        PartialSolution.UpwardSolution upwardSolution = ContractedDijkstra.calculateUpwardSolution(allNodes, startEndNode);
        PartialSolution.DownwardSolution downwardSolution = ContractedDijkstra.calculateDownwardSolution(allNodes, startEndNode);
        return new UpAndDownPair(upwardSolution, downwardSolution);
    }

    public static DijkstraSolution contractedGraphDijkstra(HashMap<Long, Node> allNodes, Node startNode, Node endNode, PartialSolutionCache cache) {
        UpAndDownPair startNodePair = getOrCalculateUpDownPair(allNodes, startNode, cache);
        UpAndDownPair endNodePair = getOrCalculateUpDownPair(allNodes, endNode, cache);
        return ContractedDijkstra.mergeUpwardAndDownwardSolutions(startNodePair.up, endNodePair.down);
    }

}
