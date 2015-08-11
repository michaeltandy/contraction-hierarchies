
package uk.me.mjt.ch;

import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarrierTest {

    public BarrierTest() {
    }
    
    @Test
    public void testSimpleReplaceDoesntBreakDijkstra() {
        HashMap<Long, Node> allNodes = MakeTestData.makeGatedRow();
        Node startNode = allNodes.get(1L);
        Node endNode = allNodes.get(3L);
        
        DijkstraSolution ignoringBarrier = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, endNode, Dijkstra.Direction.FORWARDS);
        assertEquals(2000,ignoringBarrier.totalDriveTimeMs);
        System.out.println(ignoringBarrier);
        assertEquals(2,ignoringBarrier.edges.size());
        
        Barrier.replaceBarriersWithAccessOnlyEdges(allNodes);
        AccessOnly.stratifyMarkedAndImplicitAccessOnlyClusters(allNodes, startNode);
        
        endNode = allNodes.get(3L+AccessOnly.ACCESSONLY_END_NODE_ID_PREFIX);
        DijkstraSolution replacedAndStratified = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, endNode, Dijkstra.Direction.FORWARDS);
        
        assertEquals(2000,replacedAndStratified.totalDriveTimeMs);
    }
    
    @Test
    public void testDivertAroundAvoidableGate() {
        HashMap<Long, Node> allNodes = MakeTestData.makeGatedThorn();
        Node startNode = allNodes.get(1L);
        Node endNode = allNodes.get(5L);
        
        Barrier.replaceBarriersWithAccessOnlyEdges(allNodes);
        AccessOnly.stratifyMarkedAndImplicitAccessOnlyClusters(allNodes, startNode);
        
        DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, endNode, Dijkstra.Direction.FORWARDS);
        
        assertEquals(4000,ds.totalDriveTimeMs);
    }

}