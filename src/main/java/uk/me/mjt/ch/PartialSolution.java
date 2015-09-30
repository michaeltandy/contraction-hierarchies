
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
        while (currentIdx >= 0) {
            long nodeId = nodeIds[currentIdx];
            Node thisNode = md.getNodeById(nodeId);
            nodes.addFirst(thisNode);
            long viaEdgeId = viaEdges[currentIdx];
            if (viaEdgeId != START_NODE_TO_START_NODE_PATH) {
                DirectedEdge viaEdge = null;
                for (DirectedEdge de : thisNode.getEdgesFromAndTo()) {
                    if (de.edgeId == viaEdgeId)
                        viaEdge = de;
                }
                edges.addFirst(viaEdge);
                Node fromNode = (viaEdge.to==thisNode?viaEdge.from:viaEdge.to);
                currentIdx = Arrays.binarySearch(contractionOrders, fromNode.contractionOrder);
            } else {
                currentIdx = -1;
            }
        }
        
        DijkstraSolution result = new DijkstraSolution(totalDriveTimes[index], nodes, edges);
        return result;
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

