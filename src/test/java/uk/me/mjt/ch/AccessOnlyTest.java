
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
        
        Node partAccessOnly1 = graph.getNodeById(1L);
        Node partAccessOnly7 = graph.getNodeById(7L);
        HashSet<Node> expectedCluster = new HashSet();
        expectedCluster.add(partAccessOnly1);
        expectedCluster.add(partAccessOnly7);
        
        List<AccessOnly.AccessOnlyCluster> result = AccessOnly.findAccessOnlyClusters(graph.getAllNodes());
        
        assertNotNull(result);
        assertEquals(1,result.size());
        AccessOnly.AccessOnlyCluster cluster = result.get(0);
        
        assertEquals(expectedCluster, cluster.nodes);
    }
    
    @Test
    public void testCloneNodesAndConnections() {
        MapData original = MakeTestData.makePartlyAccessOnlyRing();
        
        HashMap<Long,Node> clone = AccessOnly.cloneNodesAndConnectionsAddingPrefix(original.getAllNodes(), 0L, new AtomicLong(1));
        
        assertEquals(original.getAllNodeIds(),clone.keySet());
        for (Long nodeId : original.getAllNodeIds()) {
            Node originalNode = original.getNodeById(nodeId);
            Node cloneNode = clone.get(nodeId);
            
            assertEquals(originalNode,cloneNode);
            assertEquals(originalNode.edgesFrom.toString(),cloneNode.edgesFrom.toString());
            assertEquals(originalNode.edgesTo.toString(),cloneNode.edgesTo.toString());
        }
    }
    
    @Test
    public void testStratify() {
        MapData graph = MakeTestData.makePartlyAccessOnlyRing();
        assertEquals(7, graph.getNodeCount());
        
        Node partAccessOnly1 = graph.getNodeById(1L);
        Node partAccessOnly7 = graph.getNodeById(7L);
        assertTrue(partAccessOnly1.getNeighbors().contains(partAccessOnly7));
        assertTrue(partAccessOnly7.getNeighbors().contains(partAccessOnly1));
        
        AccessOnly.stratifyMarkedAccessOnlyClusters(graph);
        
        assertEquals(11, graph.getNodeCount());
        assertFalse(partAccessOnly1.getNeighbors().contains(partAccessOnly7));
        assertFalse(partAccessOnly7.getNeighbors().contains(partAccessOnly1));
    }
    
}