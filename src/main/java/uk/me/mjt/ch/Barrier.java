
package uk.me.mjt.ch;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


public enum Barrier {
    TRUE, FALSE;
    
    public static final long BARRIER_NODE_ID_PREFIX = 100000000000000000L;
    public static final long INITIAL_NEW_EDGE_ID = 2000000000L;
    
    public static void replaceBarriersWithAccessOnlyEdges(HashMap<Long,Node> allNodes) {
        Preconditions.checkNoneNull(allNodes);
        
        List<Node> barrierNodes = barrierNodesIn(allNodes.values());
        AtomicLong edgeIdCounter = new AtomicLong(INITIAL_NEW_EDGE_ID);
        
        for (Node n : barrierNodes) {
            replaceBarrierNodeWithAccessOnlyEdge(allNodes, n, edgeIdCounter);
        }
    }
    
    private static List<Node> barrierNodesIn(Collection<Node> toFilter) {
        ArrayList<Node> barrierNodes = new ArrayList<>();
        for (Node n : toFilter) {
            if (n.barrier==Barrier.TRUE) {
                barrierNodes.add(n);
            }
        }
        return barrierNodes;
    }
    
    private static void replaceBarrierNodeWithAccessOnlyEdge(HashMap<Long,Node> allNodes, Node n, AtomicLong edgeIdCounter) {
        Preconditions.checkNoneNull(allNodes, n);
        Preconditions.require(n.barrier==Barrier.TRUE);
        
        List<Node> neigbors = n.getNeighbors();
        int neighborCount = neigbors.size();
        if (neighborCount == 0 || neighborCount > 2) {
            // There are about 260 of these in the UK OSM data. 
            System.out.println("Ignoring barrier with " + neighborCount + " neighbors " + n);
            n.barrier = Barrier.FALSE;
        } else if (n.getNeighbors().size() == 1) {
            // Barrier at the end of a road (e.g. transition from a road to a footpath)
            n.barrier = Barrier.FALSE;
        } else {
            Node newNode = makeNewNodeLinkedByAccessOnlyEdges(n, edgeIdCounter);
            allNodes.put(newNode.nodeId, newNode);
            
            Node firstNeighbor = neigbors.get(0);
            
            for (DirectedEdge de : new ArrayList<>(n.edgesFrom)) {
                if (de.to == firstNeighbor) {
                    removeEdge(de);
                    makeEdgeAndAddToNodes(de.edgeId, newNode, de.to, de.driveTimeMs, de.accessOnly);
                }
            }
            
            for (DirectedEdge de : new ArrayList<>(n.edgesTo)) {
                if (de.from == firstNeighbor) {
                    removeEdge(de);
                    makeEdgeAndAddToNodes(de.edgeId, de.from, newNode, de.driveTimeMs, de.accessOnly);
                }
            }
            
            n.sortNeighborLists();
            newNode.sortNeighborLists();
            Node.sortNeighborListsAll(neigbors);
            
            n.barrier = Barrier.FALSE;
        }
    }
    
    private static Node makeNewNodeLinkedByAccessOnlyEdges(Node n, AtomicLong edgeIdCounter) {
        long newId = n.nodeId+BARRIER_NODE_ID_PREFIX;
        Node clone = new Node(newId, n.lat, n.lon, Barrier.FALSE);
        int driveTimeMs = 0;
        makeEdgeAndAddToNodes(edgeIdCounter.incrementAndGet(), n, clone, driveTimeMs, AccessOnly.TRUE);
        makeEdgeAndAddToNodes(edgeIdCounter.incrementAndGet(), clone, n, driveTimeMs, AccessOnly.TRUE);
        return clone;
    }
    
    private static DirectedEdge makeEdgeAndAddToNodes(long edgeId, Node from, Node to, int driveTimeMs, AccessOnly accessOnly) {
        Preconditions.checkNoneNull(from,to, accessOnly);
        DirectedEdge de = new DirectedEdge(edgeId, from, to, driveTimeMs, accessOnly);
        from.edgesFrom.add(de);
        to.edgesTo.add(de);
        return de;
    }
    
    private static void removeEdge(DirectedEdge de) {
        Preconditions.checkNoneNull(de);
        de.from.edgesFrom.remove(de);
        de.to.edgesTo.remove(de);
    }

}
