package uk.me.mjt.ch;

import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccessOnlyDijkstraTest {

    public AccessOnlyDijkstraTest() {
    }

    @Test
    public void testDijkstra() {
        HashMap<Long,Node> graph = MakeTestData.makePartlyAccessOnlyRing();
        
        Node startNode = graph.get(2L);
        Node endNode = graph.get(6L);
        
        DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(graph, startNode, endNode, Dijkstra.Direction.FORWARDS);
        System.out.println("Solution: "+ds);
        
        assertNotNull(ds);
        assertEquals(5, ds.nodes.size());
    }
    
    @Test
    public void testWithContraction() {
        HashMap<Long,Node> graph = MakeTestData.makePartlyAccessOnlyRing();
        
        Node startNode = graph.get(2L);
        Node endNode = graph.get(6L);
        
        GraphContractor instance = new GraphContractor(graph);
        instance.initialiseContractionOrder();
        instance.contractAll();
        DijkstraSolution ds = ContractedDijkstra.contractedGraphDijkstra(graph, startNode, endNode);
        
        System.out.println("Solution: "+ds);
        
        assertNotNull(ds);
        assertEquals(5, ds.nodes.size());
        
    }

    @Test
    public void testThorn() {
        HashMap<Long,Node> graph = MakeTestData.makePartlyAccessOnlyThorn();
        
        Node startNode = graph.get(1L);
        Node endNode = graph.get(5L);
        
        DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(graph, startNode, endNode, Dijkstra.Direction.FORWARDS);
        System.out.println("Solution: "+ds);
        
        assertNotNull(ds);
        assertEquals(4000, ds.totalDriveTimeMs);
        assertEquals(5, ds.nodes.size());
    }

}