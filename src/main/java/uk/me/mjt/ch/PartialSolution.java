
package uk.me.mjt.ch;

import java.util.List;

public abstract class PartialSolution {
    
    private final Node nodeOfInterest;
    private final List<DijkstraSolution> individualNodeSolutions;
    private final Dijkstra.Direction direction;
    
    private PartialSolution(Node nodeOfInterest, List<DijkstraSolution> individualNodeSolutions, Dijkstra.Direction direction) {
        Preconditions.checkNoneNull(nodeOfInterest,individualNodeSolutions,direction);
        this.nodeOfInterest = nodeOfInterest;
        this.individualNodeSolutions = individualNodeSolutions;
        this.direction = direction;
    }

    public List<DijkstraSolution> getIndividualNodeSolutions() {
        return individualNodeSolutions;
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
