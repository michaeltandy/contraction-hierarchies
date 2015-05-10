package uk.me.mjt.ch;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static uk.me.mjt.ch.DirectedEdge.ARRAY_LENGTH_SEGMENTS;

public class DirectedEdgeProfileTest {
    private final int MILLIS_IN_A_DAY = 24*60*60*1000;
    
    private final int EDGE_ID=1;
            
    private final Node testNode1 = new Node(1,0,0);
    private final Node testNode2 = new Node(2,0,0);
    private final Node testNode3 = new Node(3,0,0);
    private final Node testNode4 = new Node(4,0,0);
    
    private final Node[] nullNodeArray = new Node[ARRAY_LENGTH_SEGMENTS];
    
    public DirectedEdgeProfileTest() {
    }

    @Test
    public void testFlatTransitTimes() {
        int[] flatProfile = DirectedEdge.repeatInts(10000);
        DirectedEdge edge12 = new DirectedEdge(EDGE_ID, testNode1, testNode2, flatProfile, nullNodeArray, 0);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=1000) {
            assertEquals(10000,edge12.transitTimeAt(i));
        }
        
        DirectedEdge edge23 = new DirectedEdge(EDGE_ID, testNode2, testNode3, flatProfile, nullNodeArray, 0);
        
        DirectedEdge edge13 = edge12.plus(edge23, EDGE_ID);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=1000) {
            assertEquals(20000,edge13.transitTimeAt(i));
            assertEquals(testNode2, edge13.viaPointAt(i));
        }
    }
    
    @Test
    public void testVaryingTransitTimes() {
        int[] repeatedProfile12 = DirectedEdge.repeatInts(2100000,2700000);
        
        DirectedEdge edge12 = new DirectedEdge(EDGE_ID, testNode1, testNode2, repeatedProfile12, nullNodeArray, 0);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=60000) {
            assertTrue(edge12.transitTimeAt(i) >= 2100000);
            assertTrue(edge12.transitTimeAt(i) <= 2700000);
        }
        
        
        int[] repeatedProfile23 = DirectedEdge.repeatInts(600000,0);
        DirectedEdge edge23 = new DirectedEdge(EDGE_ID, testNode2, testNode3, repeatedProfile23, nullNodeArray, 0);
        
        DirectedEdge edge13 = edge12.plus(edge23, EDGE_ID);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=60000) {
            assertTrue(edge13.transitTimeAt(i) >= 2100000+0);
            assertTrue(edge13.transitTimeAt(i) <= 2700000+600000);
            assertEquals(testNode2, edge13.viaPointAt(i));
        }
    }
    
    @Test
    public void testMinWith() {
        
        int[] repeatedProfile124 = DirectedEdge.repeatInts(1000,5000);
        Node[] viaNodes124 = DirectedEdge.repeatNodes(testNode2);
        DirectedEdge edge124 = new DirectedEdge(EDGE_ID, testNode1, testNode4, repeatedProfile124, viaNodes124, 0);
        
        int[] flatProfile134 = DirectedEdge.repeatInts(2500);
        Node[] viaNodes134 = DirectedEdge.repeatNodes(testNode3);
        DirectedEdge edge134 = new DirectedEdge(EDGE_ID, testNode1, testNode4, flatProfile134, viaNodes134, 0);
        
        DirectedEdge edge14 = edge124.minWith(edge134, EDGE_ID);
        
        int node2Count = 0;
        int node3Count = 0;
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=1000) {
            assertTrue(edge14.transitTimeAt(i) >= 1000);
            assertTrue(edge14.transitTimeAt(i) <= 2500);
            if (edge14.viaPointAt(i)==testNode2) {
                node2Count++;
            } else if (edge14.viaPointAt(i)==testNode3) {
                node3Count++;
            } else {
                fail("This should never happen");
            }
        }
        
        System.out.println("Counts: " + node2Count + " & " + node3Count);
        assertEquals(node2Count, node3Count, 2);
    }
    
    @Test
    public void testExtractRoute() {
        int[] flatProfile = DirectedEdge.repeatInts(10000);
        DirectedEdge edge12 = new DirectedEdge(1, testNode1, testNode2, flatProfile, nullNodeArray, 0);
        DirectedEdge edge23 = new DirectedEdge(2, testNode2, testNode3, flatProfile, nullNodeArray, 0);
        DirectedEdge edge34 = new DirectedEdge(3, testNode3, testNode4, flatProfile, nullNodeArray, 0);
        
        DirectedEdge edge13 = edge12.plus(edge23, 4);
        DirectedEdge edge14 = edge13.plus(edge34, 5);
        
        int[] flatSlowProfile = DirectedEdge.repeatInts(100000);
        DirectedEdge edge14slow = new DirectedEdge(6, testNode1, testNode4, flatSlowProfile, nullNodeArray, 0);
        
        testNode1.edgesFrom.add(edge12);
        testNode1.edgesFrom.add(edge13);
        testNode1.edgesFrom.add(edge14);
        testNode1.edgesFrom.add(edge14slow);
        
        testNode2.edgesFrom.add(edge23);
        testNode3.edgesFrom.add(edge34);
        
        testNode2.edgesTo.add(edge12);
        testNode3.edgesTo.add(edge13);
        testNode4.edgesTo.add(edge14);
        
        testNode3.edgesTo.add(edge23);
        testNode4.edgesTo.add(edge34);
        testNode4.edgesTo.add(edge14slow);
        
        List<Node> uncontractedNodes = edge14.getUncontractedNodesAt(0);
        
        assertEquals(4,uncontractedNodes.size());
        assertEquals(testNode1,uncontractedNodes.get(0));
        assertEquals(testNode2,uncontractedNodes.get(1));
        assertEquals(testNode3,uncontractedNodes.get(2));
        assertEquals(testNode4,uncontractedNodes.get(3));
    }
    
}
