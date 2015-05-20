package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectedEdge {

    public final long edgeId;
    public final Node from;
    public final Node to;
    public final int driveTimeMs;

    // Parameters for graph contraction:
    public final DirectedEdge first;
    public final DirectedEdge second;
    public final int contractionDepth;
    
    public DirectedEdge(long edgeId, Node from, Node to, int driveTimeMs) {
        this(edgeId,from,to,driveTimeMs,null,null);
    }

    public DirectedEdge(long edgeId, Node from, Node to, int driveTimeMs, DirectedEdge first, DirectedEdge second) {
        this.edgeId = edgeId;
        this.from = from;
        this.to = to;
        this.driveTimeMs = driveTimeMs;
        this.first = first;
        this.second = second;
        if (first == null && second == null) {
            contractionDepth = 0;
        } else if (first != null && second != null){
            contractionDepth = Math.max(first.contractionDepth, second.contractionDepth)+1;
        } else {
            throw new IllegalArgumentException("Must have either both or neither child edges set. Instead had " + first + " and " + second);
        }
    }
    
    public boolean isShortcut() {
        return (contractionDepth != 0);
    }

    public List<DirectedEdge> getUncontractedEdges() {
        if (!isShortcut()) {
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
