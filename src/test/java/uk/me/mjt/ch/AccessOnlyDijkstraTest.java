package uk.me.mjt.ch;

import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccessOnlyDijkstraTest {

    public AccessOnlyDijkstraTest() {
    }

    @Test
    public void testDijkstra() {
        MapData graph = MakeTestData.makePartlyAccessOnlyRing();
        AccessOnly.stratifyMarkedAccessOnlyClusters(graph);
        
        Node startNode = graph.getNodeById(2L);
        Node endNode = graph.getNodeById(6L);
        
        DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(startNode, endNode, Dijkstra.Direction.FORWARDS);
        System.out.println("Solution: "+ds);
        
        assertNotNull(ds);
        assertEquals(4000, ds.totalDriveTimeMs);
        assertEquals(5, ds.nodes.size());
    }
    
    @Test
    public void testWithContraction() {
        MapData graph = MakeTestData.makePartlyAccessOnlyRing();
        AccessOnly.stratifyMarkedAccessOnlyClusters(graph);
        
        Node startNode = graph.getNodeById(2L);
        Node endNode = graph.getNodeById(6L);
        
        GraphContractor instance = new GraphContractor(graph);
        instance.initialiseContractionOrder();
        instance.contractAll();
        DijkstraSolution ds = ContractedDijkstra.contractedGraphDijkstra(graph, startNode, endNode);
        
        System.out.println("Solution: "+ds);
        
        assertNotNull(ds);
        assertEquals(4000, ds.totalDriveTimeMs);
        assertEquals(5, ds.nodes.size());
    }

    @Test
    public void testThorn() {
        MapData graph = MakeTestData.makePartlyAccessOnlyThorn();
        AccessOnly.stratifyMarkedAccessOnlyClusters(graph);
        
        Node startNode = graph.getNodeById(1L);
        Node endNode = graph.getNodeById(5L);
        
        DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(startNode, endNode, Dijkstra.Direction.FORWARDS);
        System.out.println("Solution: "+ds);
        
        assertNotNull(ds);
        assertEquals(4000, ds.totalDriveTimeMs);
        assertEquals(5, ds.nodes.size());
    }

}