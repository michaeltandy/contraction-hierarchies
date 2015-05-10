
package uk.me.mjt.ch;

import java.util.HashMap;
import uk.me.mjt.ch.DirectedEdge;
import static uk.me.mjt.ch.DirectedEdge.ARRAY_LENGTH_SEGMENTS;
import uk.me.mjt.ch.Node;


public class MakeTestData {
    
    public static HashMap<Long,Node> threeNodeFlatProfileWithShortcut() {
        Node n1 = new Node(1, 52f, 0.1f);
        Node n2 = new Node(2, 52f, 0.2f);
        Node n3 = new Node(3, 52f, 0.3f);
        
        DirectedEdge de1 = new DirectedEdge(1001, n1, n2, 1);
        DirectedEdge de2 = new DirectedEdge(1002, n2, n3, 2);
        DirectedEdge de3 = de1.plus(de2, 1003);
        
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
    
    /**
     * Produces a graph such that one can always travel from 101 to 104 at a cost
     * of 3, but by three different routes. Those routes are 101->104, 
     * 101->102->104 and 101->102->103->104 - but sometimes the fastest route
     * to 104 is via 103, and other times the fastest route to 103 is via 104
     * <pre>
     *     3,10,10
     * 101-------------104
     *  \              / |
     *   \     10,2,10/  |
     *    \          /   |
     *     \        /    |
     * 1,1,1\      /     |
     *       \    /      |
     *        \  /       |
     *         102       |
     *          \  1,10,1|
     *           \       |
     *            \      |
     *             \     |
     *        10,1,1\    |
     *               \   |
     *                \  |
     *                 103
     * </pre>
     * @return 
     */
    public static HashMap<Long,Node> fourNodeProfiledNoShortcuts() {
        Node n101 = new Node(101, 52f, 0.1f);
        Node n102 = new Node(102, 52f, 0.2f);
        Node n103 = new Node(103, 52f, 0.3f);
        Node n104 = new Node(104, 52f, 0.3f);
        
        makeInstallBidirectionalEdge(n101,n102, DirectedEdge.repeatInts(1,1,1));
        makeInstallBidirectionalEdge(n102,n103, DirectedEdge.repeatInts(10,1,1));
        makeInstallBidirectionalEdge(n103,n104, DirectedEdge.repeatInts(1,10,1));
        makeInstallBidirectionalEdge(n102,n104, DirectedEdge.repeatInts(10,2,10));
        makeInstallBidirectionalEdge(n101,n104, DirectedEdge.repeatInts(3,10,10));
        
        return nodesToMap(n101,n102,n103,n104);
    }
    
    private static void makeInstallBidirectionalEdge(Node a, Node b, int[] profile) {
        makeInstallUncontractedEdge(a, b, profile);
        makeInstallUncontractedEdge(b, a, profile);
    }
    
    private static DirectedEdge makeInstallUncontractedEdge(Node from, Node to, int[] profile) {
        Node[] nullNodeArray = new Node[ARRAY_LENGTH_SEGMENTS];
        DirectedEdge de = new DirectedEdge(55555, from, to, profile, nullNodeArray, 0);
        from.edgesFrom.add(de);
        to.edgesTo.add(de);
        return de;
    }
    
    private static HashMap<Long,Node> nodesToMap(Node... nodes) {
        HashMap<Long,Node> nodeMap = new HashMap(nodes.length);
        for (Node n : nodes) {
            nodeMap.put(n.nodeId, n);
        }
        return nodeMap;
    }

}
