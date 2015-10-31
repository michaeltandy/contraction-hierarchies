package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DirectedEdge implements Comparable<DirectedEdge>{
    private static final long PLACEHOLDER_ID = Long.MIN_VALUE;
    
    public final long edgeId;
    public final long sourceDataEdgeId;
    public final Node from;
    public final Node to;
    public final int driveTimeMs;
    public AccessOnly accessOnly;

    // Parameters for graph contraction:
    public final DirectedEdge first;
    public final DirectedEdge second;
    public final int contractionDepth;
    private final UnionList<DirectedEdge> uncontractedEdges;
    
    public DirectedEdge(long edgeId, long sourceDataEdgeId, Node from, Node to, int driveTimeMs, AccessOnly isAccessOnly) {
        this(checkId(edgeId), sourceDataEdgeId, from,to,driveTimeMs,isAccessOnly,null,null);
    }
    
    public DirectedEdge(long edgeId, long sourceDataEdgeId, DirectedEdge first, DirectedEdge second) {
        this(checkId(edgeId), sourceDataEdgeId, first.from, second.to, first.driveTimeMs+second.driveTimeMs, null, first, second);
    }
    
    public DirectedEdge(Node from, Node to, int driveTimeMs, DirectedEdge first, DirectedEdge second) {
        this(PLACEHOLDER_ID, PLACEHOLDER_ID, from, to, driveTimeMs, null, first, second);
    }

    private DirectedEdge(long edgeId, long sourceDateEdgeId, Node from, Node to, int driveTimeMs, AccessOnly accessOnly, DirectedEdge first, DirectedEdge second) {
        Preconditions.checkNoneNull(from, to);
        Preconditions.require(driveTimeMs >= 0);
        if (edgeId>0 && first!=null && second!=null) {
            // If this check starts failing, your edge IDs for shortcuts probably start too low.
            Preconditions.require(edgeId>first.edgeId, edgeId>second.edgeId);
        }
        this.edgeId = edgeId;
        this.sourceDataEdgeId = sourceDateEdgeId;
        this.from = from;
        this.to = to;
        this.driveTimeMs = driveTimeMs;
        this.first = first;
        this.second = second;
        if (first == null && second == null) {
            contractionDepth = 0;
            uncontractedEdges = null;
            Preconditions.checkNoneNull(accessOnly);
            this.accessOnly = accessOnly;
        } else if (first != null && second != null){
            contractionDepth = Math.max(first.contractionDepth, second.contractionDepth)+1;
            uncontractedEdges = new UnionList<>(first.getUncontractedEdges(),second.getUncontractedEdges());
            // Eliminate access only nodes edges before performing contraction.
            Preconditions.require(first.accessOnly==AccessOnly.FALSE,second.accessOnly==AccessOnly.FALSE); 
            this.accessOnly = AccessOnly.FALSE;
        } else {
            throw new IllegalArgumentException("Must have either both or neither child edges set. Instead had " + first + " and " + second);
        }
    }
    
    private static long checkId(long proposedId) {
        if (proposedId == PLACEHOLDER_ID) 
            throw new IllegalArgumentException("Attempt to create DirectedEdge with reserved ID, " + PLACEHOLDER_ID);
        return proposedId;
    }
    
    public boolean isShortcut() {
        return (contractionDepth != 0);
    }

    public List<DirectedEdge> getUncontractedEdges() {
        if (!isShortcut()) {
            return Collections.singletonList(this);
        } else {
            return uncontractedEdges;
        }
    }
    
    public DirectedEdge cloneWithEdgeId(long edgeId) {
        Preconditions.require(this.edgeId==PLACEHOLDER_ID, contractionDepth>0);
        return new DirectedEdge(edgeId,edgeId, from, to, driveTimeMs, accessOnly, first, second);
    }
    
    public DirectedEdge cloneWithEdgeIdAndFromToNodeAddingToLists(long newEdgeId, Node from, Node to) {
        return cloneWithEdgeIdAndFromToNodeAddingToLists(newEdgeId, from, to, accessOnly);
    }
    
    public DirectedEdge cloneWithEdgeIdAndFromToNodeAddingToLists(long newEdgeId, Node from, Node to, AccessOnly accessOnly) {
        Preconditions.require(edgeId!=PLACEHOLDER_ID, contractionDepth==0);
        DirectedEdge de = new DirectedEdge(newEdgeId, sourceDataEdgeId, from, to, driveTimeMs, accessOnly, first, second);
        de.addToToAndFromNodes();
        return de;
    }
    
    public static DirectedEdge makeZeroLengthEdgeAddingToLists(long newEdgeId, Node from, Node to, AccessOnly accessOnly) {
        DirectedEdge de = new DirectedEdge(newEdgeId, -1, from, to, 0, accessOnly);
        de.addToToAndFromNodes();
        return de;
    }
    
    public static DirectedEdge makeDelayEdge(Node delayAt, int delayLengthMs, AccessOnly accessOnly) {
        Preconditions.require(delayLengthMs > 0, accessOnly!=null);
        return new DirectedEdge(PLACEHOLDER_ID, PLACEHOLDER_ID, delayAt, delayAt, delayLengthMs, accessOnly, null, null);
    }
    
    public boolean isDelayEdge() {
        return (from==to && edgeId==PLACEHOLDER_ID);
    }
    
    public void addToToAndFromNodes() {
        if (!from.edgesFrom.contains(this)) {
            from.edgesFrom.add(this);
            from.sortNeighborLists();
        }
        if (!to.edgesTo.contains(this)) {
            to.edgesTo.add(this);
            to.sortNeighborLists();
        }
    }
    
    public void removeFromToAndFromNodes() {
        this.from.edgesFrom.remove(this);
        this.to.edgesTo.remove(this);
    }

    @Override
    public String toString() {
        return from.nodeId+"--"+driveTimeMs+"("+contractionDepth+")-->"+to.nodeId;
    }
    
    public String toDetailedString() {
        return "DirectedEdge{" + "edgeId=" + edgeId + ", from=" + from + ", to=" + to + ", driveTimeMs=" +
                driveTimeMs + ", accessOnly=" + accessOnly + ", first=" + first + ", second=" + second + 
                ", contractionDepth=" + contractionDepth + ", uncontractedEdges=" + uncontractedEdges + '}';
    }
    
    @Override
    public int compareTo(DirectedEdge o) {
        if (o==null) return -1;
        if (this.edgeId==PLACEHOLDER_ID || o.edgeId==PLACEHOLDER_ID) {
            throw new RuntimeException("Michael didn't write a very thorough comparator.");
        }
        return Long.compare(this.edgeId, o.edgeId);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + (int) (this.edgeId ^ (this.edgeId >>> 32));
        hash = 19 * hash + Objects.hashCode(this.from);
        hash = 19 * hash + Objects.hashCode(this.to);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DirectedEdge other = (DirectedEdge) obj;
        if (this.edgeId != other.edgeId
                || !Objects.equals(this.from, other.from)
                || !Objects.equals(this.to, other.to)) {
            return false;
        }
        return true;
    }
    
    
    
}
