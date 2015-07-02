package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectedEdge {
    public static final long PLACEHOLDER_ID = -123456L;

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
        Preconditions.checkNoneNull(from, to);
        Preconditions.require(edgeId>0||edgeId==PLACEHOLDER_ID, driveTimeMs >= 0);
        if (edgeId>0 && first!=null && second!=null) {
            // If this check starts failing, your edge IDs for shortcuts probably start too low.
            Preconditions.require(edgeId > first.edgeId, edgeId>second.edgeId);
        }
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
    
    public DirectedEdge cloneWithEdgeId(long edgeId) {
        return new DirectedEdge(edgeId, from, to, driveTimeMs, first, second);
    }
    
    public String toString() {
        return from.nodeId+"--"+driveTimeMs+"("+contractionDepth+")-->"+to.nodeId;
    }
    
}
