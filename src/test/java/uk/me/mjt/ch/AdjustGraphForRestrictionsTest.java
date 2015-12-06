
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
    public void testImplicitWorksOnDoubleGatedRow() {
        MapData graph = MakeTestData.makeDoubleGatedRow();
        assertDijkstraResult(graph,1,7,"1--1000-->2--1000-->3--1000-->4--1000-->5--1000-->6--1000-->7");
        assertDijkstraResult(graph,1,6,"1--1000-->2--1000-->3--1000-->4--1000-->5--1000-->6");
        assertDijkstraResult(graph,1,4,"1--1000-->2--1000-->3--1000-->4");
        assertDijkstraResult(graph,1,2,"1--1000-->2");
    }
    
    @Test
    public void testImplicitWorksOnDoubleAccessOnlyRow() {
        MapData graph = MakeTestData.makeDoubleAccessOnlyRow();
        assertDijkstraResult(graph,1,7,"1--1000-->2--1000-->3--1000-->4--1000-->5--1000-->6--1000-->7");
        assertDijkstraResult(graph,1,6,"1--1000-->2--1000-->3--1000-->4--1000-->5--1000-->6");
        assertDijkstraResult(graph,1,5,"1--1000-->2--1000-->3--1000-->4--1000-->5");
        assertDijkstraResult(graph,1,4,"1--1000-->2--1000-->3--1000-->4");
        assertDijkstraResult(graph,1,3,"1--1000-->2--1000-->3");
        assertDijkstraResult(graph,1,2,"1--1000-->2");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testAdjustMayNotStartAtGateNode() {
        MapData graph = MakeTestData.makeDoubleGatedRow();
        AdjustGraphForRestrictions.makeNewGraph(graph, graph.getNodeById(3));
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
    
    private void assertDijkstraResult(MapData graph, long startNodeId, long endNodeId, String expected) {
        Node startNode = graph.getNodeById(startNodeId);
        Node endNode = graph.getNodeById(endNodeId);
        
        String result = AdjustGraphForRestrictions.testRestrictedDijkstra(graph, startNode, endNode);
        
        assertNotNull(result);
        assertEquals(expected, result);
        
        assertModifiedGraph(AdjustGraphForRestrictions.makeNewGraph(graph, startNode),startNodeId,endNodeId,expected);
        assertModifiedGraph(AdjustGraphForRestrictions.makeNewGraph(graph, endNode),startNodeId,endNodeId,expected);
        
    }
    
    @Test
    public void testTrivialDoesntCrash() {
        MapData graph = MakeTestData.makeSimpleFiveEntry();
        Node startNode = graph.getNodeById(1);
        AdjustGraphForRestrictions.makeNewGraph(graph, startNode);
    }
    
    private void assertModifiedGraph(MapData modifiedGraph, long startNodeId, long endNodeId, String expected) {
        ColocatedNodeSet startNodes = modifiedGraph.getNodeBySourceDataId(startNodeId);
        ColocatedNodeSet endNodes = modifiedGraph.getNodeBySourceDataId(endNodeId);
        DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(startNodes, endNodes, Dijkstra.Direction.FORWARDS);
        
        if (ds==null) {
            System.out.println("Unable to route between " + startNodeId + " and " + endNodeId);
        } else {
            System.out.println("Successfully routed between " + startNodeId + " and " + endNodeId);
        }
        
        assertNotNull(ds);
        assertEquals(expected,solutionToSimpleString(ds));
        
        ds = Dijkstra.dijkstrasAlgorithm(endNodes, startNodes, Dijkstra.Direction.BACKWARDS);
        assertNotNull(ds);
        assertEquals(expected,backwardsSolutionToSimpleString(ds));
    }
    
    private String solutionToSimpleString(DijkstraSolution ds) {
        StringBuilder sb = new StringBuilder();
        sb.append(ds.edges.get(0).from.sourceDataNodeId);
        for (DirectedEdge de : ds.edges) {
            sb.append("--").append(de.driveTimeMs)
                    .append("-->")
                    .append(de.to.sourceDataNodeId);
        }
        return sb.toString();
    }
    
    private String backwardsSolutionToSimpleString(DijkstraSolution ds) {
        StringBuilder sb = new StringBuilder();
        sb.append(ds.edges.get(0).to.sourceDataNodeId);
        for (DirectedEdge de : ds.edges) {
            sb.insert(0, de.from.sourceDataNodeId+"--"+de.driveTimeMs+"-->");
        }
        return sb.toString();
    }

}