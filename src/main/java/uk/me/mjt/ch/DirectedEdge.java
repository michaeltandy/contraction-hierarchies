package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DirectedEdge {

    public final long edgeId;
    public final Node from;
    public final Node to;
    
    // Parameters for graph contraction:
    //public final DirectedEdge first;
    //public final DirectedEdge second;
    public final int contractionDepth;
    
    // Transit time profile
    private static final int SEGMENT_WIDTH_MS = 15*60*1000;
    private static final int SEGMENT_MIDPOINT_MS = SEGMENT_WIDTH_MS/2;
    public static final int ARRAY_LENGTH_SEGMENTS = 24*4;
    private int[] transitDurationMs;
    //private Node[] viaPoints;
    
    public DirectedEdge(long edgeId, Node from, Node to, int distance) {
        this(edgeId, from, to, repeatInts(distance), 0);
    }

    DirectedEdge(long edgeId, Node from, Node to, int[] newDurationsMs, int contractionDepth) {
        Preconditions.checkNoneNull(from,to,newDurationsMs);
        validateArray(newDurationsMs);
        this.edgeId = edgeId;
        this.from = from;
        this.to = to;
        this.contractionDepth = contractionDepth;
        this.transitDurationMs = newDurationsMs;
        //this.viaPoints = newVia;
    }

    public boolean isShortcut() {
        return (contractionDepth != 0);
    }
    
    public boolean isIdentity() {
        return (to.equals(from) && getMaxTransitDuration()==0);
    }

    /*public List<DirectedEdge> getUncontractedEdges() {
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
    }*/
    
    public int getMaxTransitDuration() {
        int maxTransitDuration = Integer.MIN_VALUE;
        for (int duration : transitDurationMs) {
            maxTransitDuration = Math.max(duration, maxTransitDuration);
        }
        return maxTransitDuration;
    }
    
    public int getMinTransitDuration() {
        int min = Integer.MAX_VALUE;
        for (int duration : transitDurationMs) {
            min = Math.min(duration, min);
        }
        return min;
    }
    
    public boolean isEverLessThan(DirectedEdge other) {
        for (int i=0 ; i<transitDurationMs.length ; i++) {
            if (this.transitDurationMs[i] < other.transitDurationMs[i]) {
                return true;
            }
        }
        return false;
    }
    
    public int transitTimeAt(int milliseconds) {
        
        if (milliseconds < SEGMENT_MIDPOINT_MS) {
            return transitDurationMs[0]; 
        } else if (milliseconds > SEGMENT_WIDTH_MS*(ARRAY_LENGTH_SEGMENTS-1)) {
            return transitDurationMs[ARRAY_LENGTH_SEGMENTS-1];
        }
        
        int firstBlock = (milliseconds-SEGMENT_MIDPOINT_MS)/SEGMENT_WIDTH_MS;
        int remainder = (milliseconds-SEGMENT_MIDPOINT_MS)%SEGMENT_WIDTH_MS;
        
        int firstTransitDuration = transitDurationMs[firstBlock];
        int secondTransitDuration = transitDurationMs[firstBlock+1];
        
        long numerator = secondTransitDuration*(long)remainder + firstTransitDuration*(long)(SEGMENT_WIDTH_MS-remainder);
        return (int)(numerator/SEGMENT_WIDTH_MS);
    }
    
    /*public Node viaPointAt(int milliseconds) {
        
        if (milliseconds < SEGMENT_MIDPOINT_MS) {
            return viaPoints[0]; 
        } else if (milliseconds > SEGMENT_WIDTH_MS*(ARRAY_LENGTH_SEGMENTS-1)) {
            return viaPoints[ARRAY_LENGTH_SEGMENTS-1];
        }
        
        int blockIdx = (milliseconds)/SEGMENT_WIDTH_MS;
        return viaPoints[blockIdx];
    }*/
    
    public DirectedEdge plus(DirectedEdge after, long newEdgeId) {
        Preconditions.checkNoneNull(after);
        Preconditions.checkTrue(this.to.equals(after.from));
        
        if (this.isIdentity()) {
            return after;
        } else if (after.isIdentity()) {
            return this;
        }
        
        int[] newDurationsMs = new int[ARRAY_LENGTH_SEGMENTS];
        
        for (int i=0 ; i<newDurationsMs.length ; i++) {
            int secondSegmentEntryTime = i*SEGMENT_WIDTH_MS+SEGMENT_MIDPOINT_MS+transitDurationMs[i];
            int secondSegmentTransitTime = after.transitTimeAt(secondSegmentEntryTime);
            newDurationsMs[i] = transitDurationMs[i] + secondSegmentTransitTime;
        }
        
        int newContractionDepth = Math.max(this.contractionDepth,after.contractionDepth)+1;
        
        return new DirectedEdge(newEdgeId, this.from, after.to, newDurationsMs, newContractionDepth);
    }
    
    public DirectedEdge minWith(DirectedEdge other, long newEdgeId) {
        Preconditions.checkNoneNull(other);
        Preconditions.checkTrue(this.from.equals(other.from), this.to.equals(other.to));
        
        int[] newDurationsMs = new int[ARRAY_LENGTH_SEGMENTS];
        //Node[] newVia = new Node[ARRAY_LENGTH_SEGMENTS];
        
        for (int i=0 ; i<newDurationsMs.length ; i++) {
            int thisTime = this.transitDurationMs[i];
            int otherTime = other.transitDurationMs[i];
            if (thisTime <= otherTime) {
                newDurationsMs[i]=thisTime;
                //newVia[i] = this.viaPoints[i];
            } else {
                newDurationsMs[i]=otherTime;
                //newVia[i] = other.viaPoints[i];
            }
        }
        
        int newContractionDepth = Math.max(this.contractionDepth,other.contractionDepth)+1;
        
        return new DirectedEdge(newEdgeId, this.from, this.to, newDurationsMs, newContractionDepth);
    }
    
    public List<Node> getUncontractedNodesAt(int edgeEntryTime) {
        List<Node> result = getNodesRecursivelyAt(edgeEntryTime);
        result.add(to);
        return Collections.unmodifiableList(result);
    }
    
    private ArrayList<Node> getNodesRecursivelyAt(int edgeEntryTime) {
        return null;
    }
    
    
    private static void validateArray(int[] toCheck) {
        if (toCheck == null || toCheck.length != ARRAY_LENGTH_SEGMENTS)
            throw new IllegalArgumentException("Invalid array - null or wrong length");
        
        int lastVal = toCheck[0];
        for (int i=0 ; i<toCheck.length ; i++) {
            if (toCheck[i] < 0)
                throw new IllegalArgumentException("Invalid array - negative");
            
            int delta = toCheck[i]-lastVal;
            
            if (delta < -SEGMENT_WIDTH_MS) {
                throw new IllegalArgumentException("Invalid array - profile to steep");
            }
        }
    }
    
    static int[] repeatInts(int... sequence) {
        int[] data = new int[ARRAY_LENGTH_SEGMENTS];
        for (int i=0 ; i<data.length ; i++) {
            int ix = (i/4);
            data[i] = sequence[ix % sequence.length];
        }
        return data;
    }
    
    static Node[] repeatNodes(Node... sequence) {
        Node[] data = new Node[ARRAY_LENGTH_SEGMENTS];
        for (int i=0 ; i<data.length ; i++) {
            int ix = (i/4);
            data[i] = sequence[ix % sequence.length];
        }
        return data;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (int) (this.edgeId ^ (this.edgeId >>> 32));
        hash = 89 * hash + Objects.hashCode(this.from);
        hash = 89 * hash + Objects.hashCode(this.to);
        hash = 89 * hash + this.contractionDepth;
        hash = 89 * hash + Arrays.hashCode(this.transitDurationMs);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DirectedEdge other = (DirectedEdge) obj;
        return this.edgeId == other.edgeId
                && Objects.equals(this.from,other.from)
                && Objects.equals(this.to,other.to)
                && this.contractionDepth == other.contractionDepth
                && Arrays.equals(this.transitDurationMs, other.transitDurationMs);
    }

    @Override
    public String toString() {
        return "DirectedEdge{" +
                "edgeId=" + edgeId +
                ", from=" + from.nodeId + 
                ", to=" + to.nodeId + 
                ", transitDurationMs=" + Arrays.toString(transitDurationMs) + '}';
    }
    
    
}
