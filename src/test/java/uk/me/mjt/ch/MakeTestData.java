
package uk.me.mjt.ch;

import java.util.*;

public class MakeTestData {
    
    public static MapData makeSimpleThreeEntry() {
        Node n1 = new Node(1, 52f, 0.1f, Barrier.FALSE);
        Node n2 = new Node(2, 52f, 0.2f, Barrier.FALSE);
        Node n3 = new Node(3, 52f, 0.3f, Barrier.FALSE);
        
        DirectedEdge de1 = new DirectedEdge(1001,1001, n1, n2, 1, AccessOnly.FALSE);
        de1.addToToAndFromNodes();
        DirectedEdge de2 = new DirectedEdge(1002,1002, n2, n3, 2, AccessOnly.FALSE);
        de2.addToToAndFromNodes();
        DirectedEdge de3 = new DirectedEdge(1003,1003, de1, de2);
        de3.addToToAndFromNodes();
        
        HashMap<Long,Node> result = new HashMap(3);
        result.put(1L, n1);
        result.put(2L, n2);
        result.put(3L, n3);
        
        Node.sortNeighborListsAll(result.values());
        return new MapData(result);
    }
    
    public static MapData makeLadder(int rowCount, int colCount) {
        HashMap<Long,Node> result = new HashMap();
        
        float rowSpacing = Math.min(0.05f/rowCount,0.05f/colCount);
        
        for (int row = 0 ; row < rowCount ; row++) {
            for (int column = 0 ; column < colCount ; column++) {
                long nodeId = ladderNodeId(row,column, rowCount,colCount);
                Node n = new Node(nodeId,51.51f+rowSpacing*row,-0.12f+rowSpacing*column, Barrier.FALSE);
                result.put(nodeId, n);
            }
        }
        
        int edgeId = 1000;
        
        for (int row = 0 ; row < rowCount ; row++) {
            for (int column = 0 ; column < colCount ; column++) {
                Node fromNode = result.get(ladderNodeId(row,column, rowCount,colCount));
                long[] possibleToNodeIds = {ladderNodeId(row,column+1, rowCount,colCount),
                            ladderNodeId(row+1,column, rowCount,colCount)};
                for (long toNodeId : possibleToNodeIds) {
                    if (result.containsKey(toNodeId)) {
                        Node toNode = result.get(toNodeId);
                        makeEdgeAndAddToNodes(edgeId++,fromNode,toNode,1000, AccessOnly.FALSE);
                        makeEdgeAndAddToNodes(edgeId++,toNode,fromNode,1000, AccessOnly.FALSE);
                    }
                }
            }
        }
        
        Node.sortNeighborListsAll(result.values());
        return new MapData(result);
    }

    private static long ladderNodeId(int row, int col, int rowCount, int colCount) {
        if (row >= rowCount || col >= colCount || row < 0 || col < 0) {
            return Integer.MIN_VALUE;
        } else {
            return colCount * row + col;
        }
    }
    
    /**
     * Makes a ring of nodes, from 1 to 7, with an access-only edge between node
     * 1 and node 7. Hence the shortest route from node 2 to node 6 would be 
     * 6->7->1->2 ignoring access-only restrictions but 2->3->4->5->6 if access 
     * only restrictions are respected.
     */
    public static MapData makePartlyAccessOnlyRing() {
        HashMap<Long,Node> result = makeRow(7);
        
        Node firstNode = result.get(1L);
        Node lastNode = result.get(7L);
        
        makeEdgeAndAddToNodes(5000,firstNode,lastNode,1000, AccessOnly.TRUE);
        makeEdgeAndAddToNodes(5001,lastNode,firstNode,1000, AccessOnly.TRUE);
        
        Node.sortNeighborListsAll(result.values());
        return new MapData(result);
    }
    
    /**
     * A graph shaped like Þ so the shortest way to reach node 4 is via an
     * access-only edge, but you can also access it via a non-access-only edge
     * which is required to route 1->5.
     */
    public static MapData makePartlyAccessOnlyThorn() {
        HashMap<Long,Node> result = makeRow(5);
        makeEdgeAndAddToNodes(5000,result.get(2L),result.get(4L),1000, AccessOnly.TRUE);
        Node.sortNeighborListsAll(result.values());
        return new MapData(result);
    }
    
    /**
     * A graph shaped like Þ with a gate on the short path between nodes 2 and 4.
     */
    public static MapData makeGatedThorn() {
        HashMap<Long,Node> result = makeRow(5);
        Node newNode = new Node(10, 52f, 0.1f, Barrier.TRUE);
        result.put(newNode.nodeId, newNode);
        
        makeEdgeAndAddToNodes(5000,result.get(2L),newNode,500, AccessOnly.FALSE);
        makeEdgeAndAddToNodes(5001,newNode,result.get(4L),500, AccessOnly.FALSE);
        Node.sortNeighborListsAll(result.values());
        return new MapData(result);
    }
    
    /**
     * No right turn 3->2->5
     * <pre>
     *   1   4
     *   |   |
     *   2---5
     *   |   |
     *   3   6
     * </pre>
     */
    public static MapData makeTurnRestrictedH() {
        HashMap<Long,Node> nodes = new HashMap();
        for (long i=1 ; i<=6 ; i++) {
            nodes.put(i, new Node(i, 52f, 0f, Barrier.FALSE));
        }
        
        makeBidirectionalEdgesAndAddToNodes(nodes.get(1L), nodes.get(2L));
        makeBidirectionalEdgesAndAddToNodes(nodes.get(2L), nodes.get(3L));
        
        makeBidirectionalEdgesAndAddToNodes(nodes.get(4L), nodes.get(5L));
        makeBidirectionalEdgesAndAddToNodes(nodes.get(5L), nodes.get(6L));
        
        makeBidirectionalEdgesAndAddToNodes(nodes.get(2L), nodes.get(5L));
        
        List<Long> noRight = new ArrayList();
        noRight.add(3000002L);
        noRight.add(2000005L);
        TurnRestriction tr = new TurnRestriction(12345, TurnRestriction.TurnRestrictionType.NOT_ALLOWED, noRight);
        HashMap<Long,TurnRestriction> trMap = new HashMap();
        trMap.put(tr.getTurnRestrictionId(), tr);
        
        Node.sortNeighborListsAll(nodes.values());
        return new MapData(nodes,trMap);
    }
    
    /**
     * No right turn 3->2->5->6
     * <pre>
     *   1---4
     *   |   |
     *   2---5
     *   |   |
     *   3   6
     * </pre>
     */
    public static MapData makeTurnRestrictedA() {
        HashMap<Long,Node> nodes = new HashMap();
        for (long i=1 ; i<=6 ; i++) {
            nodes.put(i, new Node(i, 52f, 0f, Barrier.FALSE));
        }
        
        makeBidirectionalEdgesAndAddToNodes(nodes.get(1L), nodes.get(2L));
        makeBidirectionalEdgesAndAddToNodes(nodes.get(2L), nodes.get(3L));
        
        makeBidirectionalEdgesAndAddToNodes(nodes.get(4L), nodes.get(5L));
        makeBidirectionalEdgesAndAddToNodes(nodes.get(5L), nodes.get(6L));
        
        makeBidirectionalEdgesAndAddToNodes(nodes.get(2L), nodes.get(5L));
        makeBidirectionalEdgesAndAddToNodes(nodes.get(1L), nodes.get(4L));
        
        List<Long> noRight = new ArrayList();
        noRight.add(3000002L);
        noRight.add(2000005L);
        noRight.add(5000006L);
        TurnRestriction tr = new TurnRestriction(12345, TurnRestriction.TurnRestrictionType.NOT_ALLOWED, noRight);
        HashMap<Long,TurnRestriction> trMap = new HashMap();
        trMap.put(tr.getTurnRestrictionId(), tr);
        
        Node.sortNeighborListsAll(nodes.values());
        return new MapData(nodes,trMap);
    }
    
    /**
     * No turn 2->3->6
     * <pre>
     * 1
     * ↓
     * 2←→5
     * ↓  ↕
     * 3←→6
     * ↓
     * 4
     * </pre>
     */
    public static MapData makeTurnRestrictedThorn() {
        HashMap<Long,Node> nodes = new HashMap();
        for (long i=1 ; i<=6 ; i++) {
            nodes.put(i, new Node(i, 52f, 0f, Barrier.FALSE));
        }
        
        makeUnidirectionalEdgesAndAddToNodes(nodes.get(1L), nodes.get(2L));
        makeUnidirectionalEdgesAndAddToNodes(nodes.get(2L), nodes.get(3L));
        makeUnidirectionalEdgesAndAddToNodes(nodes.get(3L), nodes.get(4L));
        
        makeBidirectionalEdgesAndAddToNodes(nodes.get(2L), nodes.get(5L));
        makeBidirectionalEdgesAndAddToNodes(nodes.get(5L), nodes.get(6L));
        makeBidirectionalEdgesAndAddToNodes(nodes.get(3L), nodes.get(6L));
        
        List<Long> restriction = new ArrayList();
        restriction.add(edgeBetween(nodes.get(2L), nodes.get(3L)).edgeId);
        restriction.add(edgeBetween(nodes.get(3L), nodes.get(6L)).edgeId);
        
        TurnRestriction tr = new TurnRestriction(12345, TurnRestriction.TurnRestrictionType.NOT_ALLOWED, restriction);
        HashMap<Long,TurnRestriction> trMap = new HashMap();
        trMap.put(tr.getTurnRestrictionId(), tr);
        
        Node.sortNeighborListsAll(nodes.values());
        return new MapData(nodes,trMap);
    }
    
    private static DirectedEdge edgeBetween(Node from, Node to) {
        DirectedEdge result = null;
        for (DirectedEdge de : from.edgesFrom) {
            if (de.from==from && de.to==to) {
                if (result == null)
                    result = de;
                else
                    throw new RuntimeException("More than one edge between " + from + " and " + to);
            }
        }
        return result;
    }
    
    private static void makeBidirectionalEdgesAndAddToNodes(Node from, Node to ) {
        makeUnidirectionalEdgesAndAddToNodes(from,to);
        makeUnidirectionalEdgesAndAddToNodes(to,from);
    }
    
    private static void makeUnidirectionalEdgesAndAddToNodes(Node from, Node to ) {
        int driveTimeMs = 1000;
        makeEdgeAndAddToNodes(from.nodeId*1000000+to.nodeId, from, to, driveTimeMs, AccessOnly.FALSE);
    }
    
    private static DirectedEdge makeEdgeAndAddToNodes(long edgeId, Node from, Node to, int driveTimeMs, AccessOnly accessOnly) {
        Preconditions.checkNoneNull(from,to);
        DirectedEdge de = new DirectedEdge(edgeId, edgeId, from, to, driveTimeMs, accessOnly);
        de.addToToAndFromNodes();
        return de;
    }
    
    /**
     * Makes a row of three nodes, with the middle node marked as a barrier.
     */
    public static MapData makeGatedRow() {
        HashMap<Long,Node> result = makeRow(3);
        result.get(2L).barrier = Barrier.TRUE;
        return new MapData(result);
    }
    
    private static HashMap<Long,Node> makeRow(int numberOfNodes) {
        Preconditions.require(numberOfNodes > 0);
        HashMap<Long,Node> result = new HashMap();
        int edgeId = 1000;
        
        Node previous = null;
        for (long i=1 ; i<=numberOfNodes ; i++) {
            Node newNode = new Node(i, 52f, 0.1f+0.001f*i, Barrier.FALSE);
            result.put(i, newNode);
            if (previous != null) {
                makeEdgeAndAddToNodes(edgeId++,previous,newNode,1000, AccessOnly.FALSE);
                makeEdgeAndAddToNodes(edgeId++,newNode,previous,1000, AccessOnly.FALSE);
            }
            previous=newNode;
        }
        
        Node.sortNeighborListsAll(result.values());
        return result;
    }
    
}

