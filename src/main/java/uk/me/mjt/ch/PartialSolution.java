
package uk.me.mjt.ch;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import uk.me.mjt.ch.Dijkstra.Direction;

public abstract class PartialSolution {
    private static final long START_NODE_TO_START_NODE_PATH = Long.MIN_VALUE;
    
    private final Node nodeOfInterest;
    private final List<DijkstraSolution> individualNodeSolutions;
    private final Dijkstra.Direction direction;
    
    private final long[] nodeIds;
    private final long[] contractionOrders;
    private final int[] totalDriveTimes;
    private final long[] viaEdges;
    
    private PartialSolution(Node nodeOfInterest, List<DijkstraSolution> individualNodeSolutions, Dijkstra.Direction direction) {
        Preconditions.checkNoneNull(nodeOfInterest,individualNodeSolutions,direction);
        this.nodeOfInterest = nodeOfInterest;
        this.individualNodeSolutions = individualNodeSolutions;
        this.direction = direction;
        sortByContractionOrder();
        
        nodeIds = new long[individualNodeSolutions.size()];
        contractionOrders = new long[individualNodeSolutions.size()];
        totalDriveTimes = new int[individualNodeSolutions.size()];
        viaEdges = new long[individualNodeSolutions.size()];
        
        makeCompactFormat();
    }

    public List<DijkstraSolution> getIndividualNodeSolutions() {
        return individualNodeSolutions;
    }
    
    private void sortByContractionOrder() {
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
    private void makeCompactFormat() {
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
    
    public String toString() {
        return individualNodeSolutions.toString();
    }
    
    public static class UpwardSolution extends PartialSolution {
        public UpwardSolution(Node nodeOfInterest, List<DijkstraSolution> ds) {
            super(nodeOfInterest,ds,Dijkstra.Direction.FORWARDS);
        }
        
        public Node getStartNode() {
            return getNodeOfInterest();
        }
    }
    
    public static class DownwardSolution extends PartialSolution {
        public DownwardSolution(Node nodeOfInterest, List<DijkstraSolution> ds) {
            super(nodeOfInterest,ds,Dijkstra.Direction.BACKWARDS);
        }
        
        public Node getEndNode() {
            return getNodeOfInterest();
        }
    }

}

