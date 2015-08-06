
package uk.me.mjt.ch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccessOnlyTest {

    public AccessOnlyTest() {
    }
    
    @Test
    public void testFindAccessOnlyClusters() {
        HashMap<Long,Node> graph = MakeTestData.makePartlyAccessOnlyRing();
        
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
        assertEquals(2,cluster.edges.size());
    }
    
}