package uk.me.mjt.ch;

import java.util.ArrayList;

public class Node {

    public final String name;
    public final ArrayList<DirectedEdge> edgesFrom = new ArrayList<DirectedEdge>();
    public final ArrayList<DirectedEdge> edgesTo = new ArrayList<DirectedEdge>();

    public int contractionOrder = Integer.MAX_VALUE;
    
    public Node(String nodeName) {
        if (nodeName == null) throw new IllegalArgumentException("Can't have nodes with null names, sorry!");
        this.name = nodeName;
    }

    int getCountOutgoingUncontractedEdges() {
        int count = 0;
        for (DirectedEdge de : edgesFrom) {
            if (de.to.contractionOrder == Integer.MAX_VALUE)
                count++;
        }
        return count;
    }

    int getCountIncomingUncontractedEdges() {
        int count = 0;
        for (DirectedEdge de : edgesTo) {
            if (de.from.contractionOrder  == Integer.MAX_VALUE)
                count++;
        }
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Node other = (Node) obj;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
