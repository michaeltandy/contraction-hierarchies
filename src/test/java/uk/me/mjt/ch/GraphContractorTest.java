package uk.me.mjt.ch;

import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraphContractorTest {

    public GraphContractorTest() {
    }

    @Test
    public void testContractAll() {
        MapData graph = MakeTestData.makeLadder(2,10);
        
        System.out.println(GeoJson.allLinks(graph.getAllNodes()));
        System.out.println("\n\n\n");
        
        GraphContractor instance = new GraphContractor(graph);
        instance.initialiseContractionOrder();
        instance.contractAll();
        
        Node startNode = graph.getNodeById(1L);
        Node endNode = graph.getNodeById(18L);
            
        DijkstraSolution contracted = ContractedDijkstra.contractedGraphDijkstra(graph, startNode, endNode);
        System.out.println("Contraction: "+contracted);
        
        assertNotNull(contracted);
        assertEquals(8000, contracted.totalDriveTimeMs);
        assertEquals(9, contracted.nodes.size());
        
    }
    
    @Test
    public void testAllToAll() {
        MapData graph = MakeTestData.makeLadder(2,10);
        
        GraphContractor instance = new GraphContractor(graph);
        instance.initialiseContractionOrder();
        instance.contractAll();
        
        for (Node startNode : graph.getAllNodes()) {
            for (Node endNode : graph.getAllNodes()) {
                DijkstraSolution contracted = ContractedDijkstra.contractedGraphDijkstra(graph, startNode, endNode);
                assertNotNull(contracted);
                assertTrue(contracted.totalDriveTimeMs >= 0);
                assertTrue(contracted.totalDriveTimeMs <= 10000);
            }
        }
    }

    

}