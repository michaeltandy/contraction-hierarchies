
package uk.me.mjt.ch;

import java.util.HashMap;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarrierTest {

    public BarrierTest() {
    }
    
    @Test
    public void testSimpleReplaceDoesntBreakDijkstra() {
        MapData allNodes = MakeTestData.makeGatedRow();
        Node startNode = allNodes.getNodeById(1L);
        Node endNode = allNodes.getNodeById(3L);
        
        DijkstraSolution ignoringBarrier = Dijkstra.dijkstrasAlgorithm(startNode, endNode, Dijkstra.Direction.FORWARDS);
        assertEquals(2000,ignoringBarrier.totalDriveTimeMs);
        System.out.println(ignoringBarrier);
        assertEquals(2,ignoringBarrier.edges.size());
        
        Barrier.replaceBarriersWithAccessOnlyEdges(allNodes);
        System.out.println(Puml.forNodes(allNodes.getAllNodes()));
        AccessOnly.stratifyMarkedAndImplicitAccessOnlyClusters(allNodes, startNode);
        
        System.out.println(Puml.forNodes(allNodes.getAllNodes()));
        List<Node> endCandidates = allNodes.getNodeByIdAndSyntheticEquivalents(3L);
        endNode = endCandidates.get(1); // REVISIT explain index 1.
        
        DijkstraSolution replacedAndStratified = Dijkstra.dijkstrasAlgorithm(startNode, endNode, Dijkstra.Direction.FORWARDS);
        
        assertEquals(2000,replacedAndStratified.totalDriveTimeMs);
    }
    
    @Test
    public void testDivertAroundAvoidableGate() {
        MapData allNodes = MakeTestData.makeGatedThorn();
        Node startNode = allNodes.getNodeById(1L);
        Node endNode = allNodes.getNodeById(5L);
        
        Barrier.replaceBarriersWithAccessOnlyEdges(allNodes);
        AccessOnly.stratifyMarkedAndImplicitAccessOnlyClusters(allNodes, startNode);
        
        DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(startNode, endNode, Dijkstra.Direction.FORWARDS);
        
        assertEquals(4000,ds.totalDriveTimeMs);
    }

}