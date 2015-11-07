
package uk.me.mjt.ch;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdjustGraphForRestrictionsTest {

    public AdjustGraphForRestrictionsTest() {
    }

    @Test
    public void testAvoidAccessOnlySegmentOfRing() {
        MapData graph = MakeTestData.makePartlyAccessOnlyRing();
        assertDijkstraResult(graph,2,6,"2--1000-->3--1000-->4--1000-->5--1000-->6");
        assertDijkstraResult(graph,1,6,"1--1000-->7--1000-->6");
    }
    
    @Test
    public void testEnterAccessOnlyWhenUnavoidable() {
        MapData graph = MakeTestData.makePartlyAccessOnlyRing();
        assertDijkstraResult(graph,1,6,"1--1000-->7--1000-->6");
        assertDijkstraResult(graph,2,7,"2--1000-->1--1000-->7");
    }
    
    @Test
    public void testPathTouchingBorderOfAccessOnlyRegion() {
        MapData graph = MakeTestData.makePartlyAccessOnlyThorn();
        assertDijkstraResult(graph,1,5,"1--1000-->2--1000-->3--1000-->4--1000-->5");
    }
    
    @Test
    public void testLongRouteTakenOnTurnRestrictedA() {
        MapData graph = MakeTestData.makeTurnRestrictedA();
        assertDijkstraResult(graph,3,6,"3--1000-->2--1000-->1--1000-->4--1000-->5--1000-->6");
    }
    
    @Test
    public void testGoThroughGateWhenItsTheOnlyOption() {
        MapData graph = MakeTestData.makeGatedRow();
        assertDijkstraResult(graph,1,3,"1--1000-->2--1000-->3");
    }
    
    @Test
    public void testAvoidGateWhenPossible() {
        MapData graph = MakeTestData.makeGatedThorn();
        assertDijkstraResult(graph,1,5,"1--1000-->2--1000-->3--1000-->4--1000-->5");
    }
    
    @Test
    public void testDelayedUTurnOnTurnRestrictedH() {
        MapData graph = MakeTestData.makeTurnRestrictedH();
        assertDijkstraResult(graph,3,6,"3--1000-->2--1000-->1--60000-->1--1000-->2--1000-->5--1000-->6");
    }
    
    @Test
    public void testTurnRestrictionsDontBreakStraightOn() {
        MapData graph = MakeTestData.makeTurnRestrictedThorn();
        assertDijkstraResult(graph,1,4,"1--1000-->2--1000-->3--1000-->4");
    }
    
    public void assertDijkstraResult(MapData graph, long startNodeId, long endNodeId, String expected) {
        Node startNode = graph.getNodeById(startNodeId);
        Node endNode = graph.getNodeById(endNodeId);
        
        AdjustGraphForRestrictions instance = new AdjustGraphForRestrictions();
        String result = instance.testRestrictedDijkstra(graph, startNode, endNode);
        
        assertNotNull(result);
        assertEquals(expected, result);
    }

}