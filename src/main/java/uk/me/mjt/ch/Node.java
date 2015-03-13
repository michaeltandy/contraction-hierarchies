package uk.me.mjt.ch;

import java.util.ArrayList;

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
}
