
package uk.me.mjt.ch;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class TurnRestrictionTest {

    public TurnRestrictionTest() {
    }

    @Test
    public void testAdjustGraphToImplementTurnRestrictions() {
        System.out.println("adjustGraphToImplementTurnRestrictions");
        
        MapData allNodes = MakeTestData.makeTurnRestrictedH();
        TurnRestriction.adjustGraphToImplementTurnRestrictions(allNodes);
        
        System.out.println("PUML for post-modification graph: " + Puml.forGraph(allNodes.getAllNodes()));
        
        Node startNode = allNodes.getNodeById(3L);
        Node endNode = allNodes.getNodeById(6L);
        
        DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(startNode, endNode, Dijkstra.Direction.FORWARDS);
        System.out.println("Solution: "+ds);
        assertEquals(5000, ds.totalDriveTimeMs);
    }
    
}