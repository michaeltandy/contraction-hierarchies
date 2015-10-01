
package uk.me.mjt.ch.cache;

import org.junit.Test;
import static org.junit.Assert.*;
import uk.me.mjt.ch.DijkstraSolution;
import uk.me.mjt.ch.GraphContractor;
import uk.me.mjt.ch.MakeTestData;
import uk.me.mjt.ch.MapData;
import uk.me.mjt.ch.Node;

public class BinaryCacheTest {

    public BinaryCacheTest() {
    }

    @Test
    public void testContractedGraphDijkstra() {
        MapData graph = MakeTestData.makeLadder(2,10);
        GraphContractor instance = new GraphContractor(graph);
        instance.initialiseContractionOrder();
        instance.contractAll();
        
        Node startNode = graph.getNodeById(1L);
        Node endNode = graph.getNodeById(18L);
        
        BinaryCache cache = new BinaryCache();
        
        // This should populate the cache:
        DijkstraSolution result1 = CachedContractedDijkstra.contractedGraphDijkstra(graph, startNode, endNode, cache);
        assertNotNull(result1);
        assertEquals(8000, result1.totalDriveTimeMs);
        assertEquals(9, result1.nodes.size());
        
        // This should be retrieved from the cache:
        DijkstraSolution result2 = CachedContractedDijkstra.contractedGraphDijkstra(graph, startNode, endNode, cache);
        assertNotNull(result2);
        assertEquals(8000, result2.totalDriveTimeMs);
        assertEquals(9, result2.nodes.size());
    }
    
    @Test
    public void testAllToAll() {
        MapData graph = MakeTestData.makeLadder(2,10);
        GraphContractor instance = new GraphContractor(graph);
        instance.initialiseContractionOrder();
        instance.contractAll();
        
        BinaryCache cache = new BinaryCache();
        
        for (Node startNode : graph.getAllNodes()) {
            for (Node endNode : graph.getAllNodes()) {
                for (int i=0 ; i<2 ; i++) {
                    DijkstraSolution result = CachedContractedDijkstra.contractedGraphDijkstra(graph, startNode, endNode, cache);
                    assertNotNull(result);
                    assertTrue(result.totalDriveTimeMs >= 0);
                    assertTrue(result.totalDriveTimeMs <= 10000);
                }
            }
        }
    }

}