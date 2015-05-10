package uk.me.mjt.ch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class DijkstraTest {
    
    public DijkstraTest() {
    }
    
    @Test
    public void testDijkstrasAlgorithm_5args() {
        System.out.println("dijkstrasAlgorithm");
        HashMap<Long, Node> allNodes = MakeTestData.fourNodeProfiledNoShortcuts();
        Node startNode = allNodes.get(101L);
        HashSet<Node> endNodes = new HashSet();
        endNodes.add(allNodes.get(104L));
        Dijkstra.Direction direction = Dijkstra.Direction.FORWARDS;
        
        List<DirectedEdge> result = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, endNodes, Integer.MAX_VALUE, direction);
        assertEquals(1, result.size());
        DirectedEdge route = result.get(0);
        
        System.out.println(route);
        
        assertEquals(3,route.getMaxTransitDuration());
        assertEquals(3,route.getMinTransitDuration());
        
        System.out.println(route.getUncontractedNodesAt(0*60*60*1000));
        System.out.println(route.getUncontractedNodesAt(1*60*60*1000));
        System.out.println(route.getUncontractedNodesAt(2*60*60*1000));
        
    }
    
}
