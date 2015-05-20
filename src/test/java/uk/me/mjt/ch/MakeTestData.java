
package uk.me.mjt.ch;

import java.util.HashMap;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.Node;


public class MakeTestData {
    
    public static HashMap<Long,Node> makeSimpleThreeEntry() {
        Node n1 = new Node(1, 52f, 0.1f);
        Node n2 = new Node(2, 52f, 0.2f);
        Node n3 = new Node(3, 52f, 0.3f);
        
        DirectedEdge de1 = new DirectedEdge(1001, n1, n2, 1);
        DirectedEdge de2 = new DirectedEdge(1002, n2, n3, 2);
        DirectedEdge de3 = new DirectedEdge(1003, n1, n3, 3, de1, de2);
        
        n1.edgesFrom.add(de1);
        n2.edgesTo.add(de1);
        
        n2.edgesFrom.add(de2);
        n3.edgesTo.add(de2);
        
        n1.edgesFrom.add(de3);
        n3.edgesTo.add(de3);
        
        HashMap<Long,Node> result = new HashMap(3);
        result.put(1L, n1);
        result.put(2L, n2);
        result.put(3L, n3);
        
        return result;
    }

}
