
package uk.me.mjt.ch;

import java.util.*;

public abstract class PartialSolution {
    private static final long START_NODE_TO_START_NODE_PATH = Long.MIN_VALUE;
    
    private final Node nodeOfInterest;
    
    private final long[] nodeIds;
    private final long[] contractionOrders;
    private final int[] totalDriveTimes;
    private final long[] viaEdges;
    
    private PartialSolution(Node nodeOfInterest, List<DijkstraSolution> individualNodeSolutions) {
        Preconditions.checkNoneNull(nodeOfInterest,individualNodeSolutions);
        this.nodeOfInterest = nodeOfInterest;
        
        nodeIds = new long[individualNodeSolutions.size()];
        contractionOrders = new long[individualNodeSolutions.size()];
        totalDriveTimes = new int[individualNodeSolutions.size()];
        viaEdges = new long[individualNodeSolutions.size()];
        
        sortByContractionOrder(individualNodeSolutions);
        makeCompactFormat(individualNodeSolutions);
    }

    public DijkstraSolution getDijkstraSolution(MapData md, int index) {
        LinkedList<DirectedEdge> edges = new LinkedList();
        LinkedList<Node> nodes = new LinkedList();
        
        int currentIdx = index;
        Node thisNode = md.getNodeById(nodeIds[currentIdx]);
        final boolean from = searchInFromDirection(thisNode, viaEdges[currentIdx]);
        
        while (currentIdx >= 0) {
            nodes.addFirst(thisNode);
            long viaEdgeId = viaEdges[currentIdx];
            if (viaEdgeId != START_NODE_TO_START_NODE_PATH) {
                DirectedEdge viaEdge = findEdgeWithId(thisNode, viaEdgeId, from);
                edges.addFirst(viaEdge);
                Node nextNode = (from?viaEdge.to:viaEdge.from);
                if (nextNode.contractionOrder > thisNode.contractionOrder) {
                    throw new RuntimeException("Unexpectedly following edge from earlier-contracted node towards "
                            + "later-contracted node? " + thisNode + " vs " + nextNode);
                }
                thisNode = nextNode;
                currentIdx = Arrays.binarySearch(contractionOrders, 0, currentIdx, nextNode.contractionOrder);
            } else {
                break;
            }
        }
        
        DijkstraSolution result = new DijkstraSolution(totalDriveTimes[index], nodes, edges);
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
    
    private void sortByContractionOrder(List<DijkstraSolution> individualNodeSolutions) {
        Collections.sort(individualNodeSolutions, new Comparator<DijkstraSolution>() {
            @Override
            public int compare(DijkstraSolution a, DijkstraSolution b) {
                long aco = a.getLastNode().contractionOrder;
                long bco = b.getLastNode().contractionOrder;
                if (aco != bco) {
                    return Long.compare(aco,bco);
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
    private void makeCompactFormat(List<DijkstraSolution> individualNodeSolutions) {
        for (int i=0 ; i<individualNodeSolutions.size() ; i++) {
            DijkstraSolution ds = individualNodeSolutions.get(i);
            Node n = ds.getLastNode();
            nodeIds[i] = n.nodeId;
            contractionOrders[i] = n.contractionOrder;
            totalDriveTimes[i] = ds.totalDriveTimeMs;
            
            List<DirectedEdge> directedEdges = ds.getDeltaEdges();

            if (directedEdges.size() == 1) {
                DirectedEdge de = directedEdges.get(0);
                viaEdges[i] = de.edgeId;
            } else if (ds.getFirstNode().equals(ds.getLastNode()) && directedEdges.isEmpty()) {
                viaEdges[i] = START_NODE_TO_START_NODE_PATH;
            } else {
                throw new RuntimeException("Delta edge length isn't 1?");
            }
        }
    }

    public long[] getNodeIds() {
        return nodeIds;
    }

    public long[] getContractionOrders() {
        return contractionOrders;
    }

    public int[] getTotalDriveTimes() {
        return totalDriveTimes;
    }
    
    Node getNodeOfInterest() {
        return nodeOfInterest;
    }
    
    public static class UpwardSolution extends PartialSolution {
        public UpwardSolution(Node nodeOfInterest, List<DijkstraSolution> ds) {
            super(nodeOfInterest,ds);
        }
        
        public Node getStartNode() {
            return getNodeOfInterest();
        }
    }
    
    public static class DownwardSolution extends PartialSolution {
        public DownwardSolution(Node nodeOfInterest, List<DijkstraSolution> ds) {
            super(nodeOfInterest,ds);
        }
        
        public Node getEndNode() {
            return getNodeOfInterest();
        }
    }

}

