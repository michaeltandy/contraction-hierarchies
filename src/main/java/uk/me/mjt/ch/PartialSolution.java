
package uk.me.mjt.ch;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import uk.me.mjt.ch.Dijkstra.Direction;

public abstract class PartialSolution {
    
    private final Node nodeOfInterest;
    private final List<DijkstraSolution> individualNodeSolutions;
    private final Dijkstra.Direction direction;
    private final long[] compactFormat;
    
    private PartialSolution(Node nodeOfInterest, List<DijkstraSolution> individualNodeSolutions, Dijkstra.Direction direction) {
        Preconditions.checkNoneNull(nodeOfInterest,individualNodeSolutions,direction);
        this.nodeOfInterest = nodeOfInterest;
        this.individualNodeSolutions = individualNodeSolutions;
        this.direction = direction;
        sortByContractionOrder();
        compactFormat = new long[individualNodeSolutions.size()*4];
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
            compactFormat[4*i]=n.nodeId;
            compactFormat[4*i+1]=n.contractionOrder;
            compactFormat[4*i+2]=ds.totalDriveTime;
            
            List<DirectedEdge> directedEdges = ds.getDeltaEdges();

            if (directedEdges.size() == 1) {
                compactFormat[4*i+3] = directedEdges.get(0).from.nodeId; // REVISIT is this the same for both directions?
            } else if (ds.getFirstNode().equals(ds.getLastNode()) && directedEdges.isEmpty()) {
                compactFormat[4*i+3] = -1;
            } else {
                throw new RuntimeException("Delta edge length isn't 1?");
            }
        }
    }
    
    public long[] getCompactFormat() {
        return compactFormat;
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

