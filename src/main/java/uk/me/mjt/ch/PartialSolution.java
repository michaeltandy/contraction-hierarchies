
package uk.me.mjt.ch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;

public abstract class PartialSolution {
    private static final long START_NODE_TO_START_NODE_PATH = Long.MIN_VALUE;
    
    private final int recordCount;
    private final ByteBuffer bb;
    
    private PartialSolution(List<DijkstraSolution> individualNodeSolutions) {
        Preconditions.checkNoneNull(individualNodeSolutions);
        sortByContractionOrder(individualNodeSolutions);
        
        recordCount = individualNodeSolutions.size();
        bb = makeCompactFormat(individualNodeSolutions);
    }
    
    private PartialSolution(ByteBuffer bb) {
        Preconditions.checkNoneNull(bb);
        Preconditions.require(bb.position()==0, bb.limit()==bb.capacity(), bb.isDirect(), bb.order()==ByteOrder.LITTLE_ENDIAN);
        recordCount = bb.getInt(0);
        int expectedCapacity = 24*recordCount + 4;
        Preconditions.require(bb.capacity()==expectedCapacity);
        this.bb = bb;
    }

    public DijkstraSolution getDijkstraSolution(MapData md, int index) {
        LinkedList<DirectedEdge> edges = new LinkedList();
        LinkedList<Node> nodes = new LinkedList();
        ContractionOrderList contractionOrderList = new ContractionOrderList();
        
        int currentIdx = index;
        Node thisNode = md.getNodeById(getNodeId(currentIdx));
        final boolean from = searchInFromDirection(thisNode, getViaEdge(currentIdx));
        
        while (currentIdx >= 0) {
            nodes.addFirst(thisNode);
            long viaEdgeId = getViaEdge(currentIdx);
            if (viaEdgeId != START_NODE_TO_START_NODE_PATH) {
                DirectedEdge viaEdge = findEdgeWithId(thisNode, viaEdgeId, from);
                edges.addFirst(viaEdge);
                Node nextNode = (from?viaEdge.to:viaEdge.from);
                if (nextNode.contractionOrder > thisNode.contractionOrder) {
                    throw new RuntimeException("Unexpectedly following edge from earlier-contracted node towards "
                            + "later-contracted node? " + thisNode + " vs " + nextNode);
                }
                thisNode = nextNode;
                currentIdx = Collections.binarySearch(contractionOrderList, nextNode.contractionOrder);
            } else {
                break;
            }
        }
        
        DijkstraSolution result = new DijkstraSolution(getTotalDriveTime(index), nodes, edges);
        return result;
    }
    
    private boolean searchInFromDirection(Node node, long edgeId) {
        if (edgeId == START_NODE_TO_START_NODE_PATH)
            return true; // Doesn't matter.
        
        try {
            findEdgeWithId(node, edgeId, true);
            return true;
        } catch (EdgeNotFoundException e) {
            findEdgeWithId(node, edgeId, false);
            return false;
        }
    }
    
    private DirectedEdge findEdgeWithId(Node node, long edgeId, boolean from) {
        for (DirectedEdge de : (from?node.edgesFrom:node.edgesTo)) {
            if (de.edgeId==edgeId)
                return de;
        }
        throw new EdgeNotFoundException();
    }
    
    private class EdgeNotFoundException extends RuntimeException {
        public EdgeNotFoundException() {
            super("Couldn't find requested edge, which shouldn't happen?");
        }
    }
    
    private static void sortByContractionOrder(List<DijkstraSolution> individualNodeSolutions) {
        Collections.sort(individualNodeSolutions, new Comparator<DijkstraSolution>() {
            @Override
            public int compare(DijkstraSolution a, DijkstraSolution b) {
                int aco = a.getLastNode().contractionOrder;
                int bco = b.getLastNode().contractionOrder;
                if (aco != bco) {
                    return Integer.compare(aco,bco);
                } else {
                    throw new RuntimeException("Two solutions with the same contraction order?!");
                }
            }
        });
    }
    
    /**
     * This compact format is intended to speed up mergeUpwardAndDownwardSolutions.
     * The ideas are:
     *  1. No need to dereference any object references
     *  2. Uses contiguous memory, which should allow efficient use of main memory bandwidth.
     *  3. Small enough to fit into L2 cache - or maybe even L1 cache!
     */
    private static ByteBuffer makeCompactFormat(List<DijkstraSolution> individualNodeSolutions) {
        int recordCount = individualNodeSolutions.size();
        
        int requiredCapacity = 24*recordCount + 4;
        ByteBuffer bb = ByteBuffer.allocateDirect(requiredCapacity);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        
        bb.putInt(0,recordCount);
        
        int contractionOrderOffset = 4;
        int nodeIdOffset = contractionOrderOffset + 4*recordCount;
        int totalDriveTimeOffset = nodeIdOffset + 8*recordCount;
        int viaEdgesOffset = totalDriveTimeOffset + 4*recordCount;
        
        for (int i=0 ; i<individualNodeSolutions.size() ; i++) {
            DijkstraSolution ds = individualNodeSolutions.get(i);
            Node n = ds.getLastNode();
            bb.putLong(nodeIdOffset+8*i, n.nodeId);
            bb.putInt(contractionOrderOffset+4*i, n.contractionOrder);
            bb.putInt(totalDriveTimeOffset+4*i, ds.totalDriveTimeMs);
            
            List<DirectedEdge> directedEdges = ds.getDeltaEdges();

            if (directedEdges.size() == 1) {
                DirectedEdge de = directedEdges.get(0);
                bb.putLong(viaEdgesOffset+8*i, de.edgeId);
            } else if (ds.getFirstNode().equals(ds.getLastNode()) && directedEdges.isEmpty()) {
                bb.putLong(viaEdgesOffset+8*i, START_NODE_TO_START_NODE_PATH);
            } else {
                throw new RuntimeException("Delta edge length isn't 1?");
            }
        }
        bb.position(0);
        bb.limit(bb.capacity());
        return bb;
    }

    public int getSize() {
        return recordCount;
    }
    
    public ByteBuffer getUnderlyingBuffer() {
        return bb.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }
    
    public int getContractionOrder(int idx) {
        int offset = 4 + 4*idx;
        return bb.getInt(offset);
    }
    
    private long getNodeId(int idx) {
        int offset = 4 + 4*recordCount + 8*idx;
        return bb.getLong(offset);
    }
    
    private int getTotalDriveTime(int idx) {
        int offset = 4 + 12*recordCount + 4*idx;
        return bb.getInt(offset);
    }
    
    private long getViaEdge(int idx) {
        int offset = 4 + 16*recordCount + 8*idx;
        return bb.getLong(offset);
    }
    
    public IntBuffer getTotalDriveTimeBuffer() {
        ByteBuffer bbDupe = bb.duplicate();
        bbDupe.position(4 + 12*recordCount);
        bbDupe.limit(4 + 16*recordCount);
        ByteBuffer view = bbDupe.slice().order(ByteOrder.LITTLE_ENDIAN);
        return view.asIntBuffer();
    }
    
    public IntBuffer getContractionOrderBuffer() {
        ByteBuffer bbDupe = bb.duplicate();
        bbDupe.position(4);
        bbDupe.limit(4 + 4*recordCount);
        ByteBuffer view = bbDupe.slice().order(ByteOrder.LITTLE_ENDIAN);
        return view.asIntBuffer();
    }
    
    private class ContractionOrderList extends AbstractList<Integer> {
        @Override
        public Integer get(int index) {
            return getContractionOrder(index);
        }

        @Override
        public int size() {
            return recordCount;
        }
    }
    
    
    public static class UpwardSolution extends PartialSolution {
        public UpwardSolution(List<DijkstraSolution> ds) {
            super(ds);
        }
        public UpwardSolution(ByteBuffer bb) {
            super(bb);
        }
    }
    
    public static class DownwardSolution extends PartialSolution {
        public DownwardSolution(List<DijkstraSolution> ds) {
            super(ds);
        }
        public DownwardSolution(ByteBuffer bb) {
            super(bb);
        }
    }

}

