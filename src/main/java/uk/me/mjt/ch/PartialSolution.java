
package uk.me.mjt.ch;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import uk.me.mjt.ch.Dijkstra.Direction;

public abstract class PartialSolution {
    
    private final Node nodeOfInterest;
    private final List<DijkstraSolution> individualNodeSolutions;
    private final Dijkstra.Direction direction;
    
    private PartialSolution(Node nodeOfInterest, List<DijkstraSolution> individualNodeSolutions, Dijkstra.Direction direction) {
        Preconditions.checkNoneNull(nodeOfInterest,individualNodeSolutions,direction);
        this.nodeOfInterest = nodeOfInterest;
        this.individualNodeSolutions = individualNodeSolutions;
        this.direction = direction;
        sortByContractionOrder();
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
    
    
    public static class UpwardSolution extends PartialSolution {
        public UpwardSolution(Node nodeOfInterest, List<DijkstraSolution> ds) {
            super(nodeOfInterest,ds,Dijkstra.Direction.FORWARDS);
        }
    }
    
    public static class DownwardSolution extends PartialSolution {
        public DownwardSolution(Node nodeOfInterest, List<DijkstraSolution> ds) {
            super(nodeOfInterest,ds,Dijkstra.Direction.BACKWARDS);
        }
    }

}
