
package uk.me.mjt.ch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccessOnlyTest {

    public AccessOnlyTest() {
    }
    
    @Test
    public void testFindAccessOnlyClusters() {
        MapData graph = MakeTestData.makePartlyAccessOnlyRing();
        
        Node partAccessOnly1 = graph.get(1L);
        Node partAccessOnly7 = graph.get(7L);
        HashSet<Node> expectedCluster = new HashSet();
        expectedCluster.add(partAccessOnly1);
        expectedCluster.add(partAccessOnly7);
        
        List<AccessOnly.AccessOnlyCluster> result = AccessOnly.findAccessOnlyClusters(graph.values());
        
        assertNotNull(result);
        assertEquals(1,result.size());
        AccessOnly.AccessOnlyCluster cluster = result.get(0);
        
        assertEquals(expectedCluster, cluster.nodes);
    }
    
    @Test
    public void testCloneNodesAndConnections() {
        MapData original = MakeTestData.makePartlyAccessOnlyRing();
        
        HashMap<Long,Node> clone = AccessOnly.cloneNodesAndConnectionsAddingPrefix(original.values(), 0L, new AtomicLong(1));
        
        assertEquals(original.keySet(),clone.keySet());
        for (Long nodeId : original.keySet()) {
            Node originalNode = original.get(nodeId);
            Node cloneNode = clone.get(nodeId);
            
            assertEquals(originalNode,cloneNode);
            assertEquals(originalNode.edgesFrom.toString(),cloneNode.edgesFrom.toString());
            assertEquals(originalNode.edgesTo.toString(),cloneNode.edgesTo.toString());
        }
    }
    
    @Test
    public void testStratify() {
        MapData graph = MakeTestData.makePartlyAccessOnlyRing();
        assertEquals(7, graph.size());
        
        Node partAccessOnly1 = graph.get(1L);
        Node partAccessOnly7 = graph.get(7L);
        assertTrue(partAccessOnly1.getNeighbors().contains(partAccessOnly7));
        assertTrue(partAccessOnly7.getNeighbors().contains(partAccessOnly1));
        
        AccessOnly.stratifyMarkedAccessOnlyClusters(graph);
        
        assertEquals(11, graph.size());
        assertFalse(partAccessOnly1.getNeighbors().contains(partAccessOnly7));
        assertFalse(partAccessOnly7.getNeighbors().contains(partAccessOnly1));
    }
    
}