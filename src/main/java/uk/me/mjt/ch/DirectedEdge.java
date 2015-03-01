package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectedEdge {

    public final Node from;
    public final Node to;
    public final float distance;

    // Parameters for graph contraction:
    public final DirectedEdge first;
    public final DirectedEdge second;
    public final int contractionDepth;
    boolean isShortcut;

    public DirectedEdge(Node from, Node to, float distance) {
        this(from,to,distance,null,null);
    }

    public DirectedEdge(Node from, Node to, float distance, DirectedEdge first, DirectedEdge second) {
        this.from = from;
        this.to = to;
        this.distance = distance;
        this.first = first;
        this.second = second;
        if (first == null && second == null) {
            contractionDepth = 0;
            isShortcut = false;
        } else if (first != null && second != null){
            contractionDepth = Math.max(first.contractionDepth, second.contractionDepth);
            isShortcut = true;
        } else {
            throw new IllegalArgumentException("Must have either both or neither child edges set. Instead had " + first + " and " + second);
        }
    }

    public List<DirectedEdge> getUncontractedEdges() {
        if (!isShortcut) {
            return Collections.singletonList(this);
        } else {
            List<DirectedEdge> a = first.getUncontractedEdges();
            List<DirectedEdge> b = second.getUncontractedEdges();
            List<DirectedEdge> l = new ArrayList<DirectedEdge>(a.size()+b.size());
            l.addAll(a);
            l.addAll(b);
            return l;
        }
    }
}
