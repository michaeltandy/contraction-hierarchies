
package uk.me.mjt.ch.cache;

import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;
import uk.me.mjt.ch.DijkstraSolution;
import uk.me.mjt.ch.GraphContractor;
import uk.me.mjt.ch.MakeTestData;
import uk.me.mjt.ch.MapData;
import uk.me.mjt.ch.Node;

public class BinaryCacheTest {
    
    MapData graph;
    GraphContractor instance;
    BinaryCache cache;

    public BinaryCacheTest() {
        graph = MakeTestData.makeLadder(2,10);
        instance = new GraphContractor(graph);
        instance.initialiseContractionOrder();
        instance.contractAll();
        cache = new BinaryCache();
    }

    @Test
    public void testContractedGraphDijkstra() {
        Node startNode = graph.getNodeById(1L);
        Node endNode = graph.getNodeById(18L);
        
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
    
    @Test
    public void testRepeatedSerialize() {
        Node n = graph.getNodeById(1L);
        UpAndDownPair uadp = CachedContractedDijkstra.calculateUpDownPair(graph, n);
        
        ByteBuffer bb1 = cache.serialize(uadp);
        ByteBuffer bb2 = cache.serialize(cache.deserialize(bb1));
        ByteBuffer bb3 = cache.serialize(cache.deserialize(bb2));
        
        assertEquals(bb1.capacity(),bb3.capacity());
        assertEquals(bb1.limit(),bb3.limit());
    }

}