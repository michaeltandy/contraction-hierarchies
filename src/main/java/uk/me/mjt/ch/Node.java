package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Node {
    public static final long UNCONTRACTED = Long.MAX_VALUE;

    public final long nodeId;
    public final ArrayList<DirectedEdge> edgesFrom = new ArrayList<DirectedEdge>();
    public final ArrayList<DirectedEdge> edgesTo = new ArrayList<DirectedEdge>();
    public final float lat;
    public final float lon;
    public boolean contractionAllowed = true;

    public long contractionOrder = UNCONTRACTED;
    
    public Node(long nodeId, float lat, float lon) {
        this.nodeId = nodeId;
        this.lat = lat;
        this.lon = lon;
    }

    int getCountOutgoingUncontractedEdges() {
        int count = 0;
        for (DirectedEdge de : edgesFrom) {
            if (de.to.contractionOrder == UNCONTRACTED)
                count++;
        }
        return count;
    }

    int getCountIncomingUncontractedEdges() {
        int count = 0;
        for (DirectedEdge de : edgesTo) {
            if (de.from.contractionOrder  == UNCONTRACTED)
                count++;
        }
        return count;
    }
    
    public boolean isContracted() {
        return contractionOrder!=UNCONTRACTED;
    }
    
    public ArrayList<Node> getNeighbors() {
        ArrayList<Node> neighbors = new ArrayList<>();
        for (DirectedEdge de : edgesFrom) {
            neighbors.add(de.to);
        }
        for (DirectedEdge de : edgesTo) {
            neighbors.add(de.from);
        }
        return neighbors;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Node other = (Node) obj;
        return this.nodeId==other.nodeId;
    }

    @Override
    public int hashCode() {
        return (int) (this.nodeId ^ (this.nodeId >>> 32));
    }
    
    public String toString() {
        return nodeId + "@" + lat + "," + lon;
    }
    
    /**
     * Sort incoming and outgoing lists of edges. Follows the following rules:
     * 1. Higher contraction order first. We do this so, if we want to find 
     * connected nodes with a higher contraction order than this one, they'll
     * be at the start of the list. When all nodes are contracted, every node
     * will have a different contraction order.
     * 2. Shorter distance first. If contraction orders are equal for two edges,
     * it means either the node on the other end is uncontracted. Shorter edges
     * are usually more interesting, and we want the sort order to be 
     * unambiguous, so this is our second means of ordering.
     * 3. If results are equal for both those tests, sort by edge ID, which 
     * should always be unique, to give us an unambiguous ordering.
     */
    
    public void sortNeighborLists() {
        Collections.sort(edgesFrom, new Comparator<DirectedEdge>() {
            @Override
            public int compare(DirectedEdge t, DirectedEdge t1) {
                if (t1.to.contractionOrder != t.to.contractionOrder) {
                    return Long.compare(t1.to.contractionOrder, t.to.contractionOrder);
                } else if (t1.driveTimeMs != t.driveTimeMs) {
                    return Long.compare(t.driveTimeMs, t1.driveTimeMs);
                } else {
                    return Long.compare(t.edgeId, t1.edgeId);
                }
            }
        });
        
        Collections.sort(edgesTo, new Comparator<DirectedEdge>() {
            @Override
            public int compare(DirectedEdge t, DirectedEdge t1) {
                if (t1.from.contractionOrder != t.from.contractionOrder) {
                    return Long.compare(t1.from.contractionOrder, t.from.contractionOrder);
                } else if (t1.driveTimeMs != t.driveTimeMs) {
                    return Long.compare(t.driveTimeMs, t1.driveTimeMs);
                } else {
                    return Long.compare(t.edgeId, t1.edgeId);
                }
            }
        });
    }
    
    public boolean anyEdgesAccessOnly() {
        for (DirectedEdge de : edgesFrom) {
            if (de.accessOnly == AccessOnly.TRUE) {
                return true;
            }
        }
        for (DirectedEdge de : edgesTo) {
            if (de.accessOnly == AccessOnly.TRUE) {
                return true;
            }
        }
        return false;
    }
    
    public boolean allEdgesAccessOnly() {
        for (DirectedEdge de : edgesFrom) {
            if (de.accessOnly == AccessOnly.FALSE) {
                return false;
            }
        }
        for (DirectedEdge de : edgesTo) {
            if (de.accessOnly == AccessOnly.FALSE) {
                return false;
            }
        }
        return true;
    }
    
}
