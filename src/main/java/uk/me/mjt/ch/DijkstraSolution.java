package uk.me.mjt.ch;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DijkstraSolution  {

    public final int totalDriveTime;
    public final List<Node> nodes;
    public final List<DirectedEdge> edges;
    
    private final DijkstraSolution preceding; // Can we remove this?
    
    public DijkstraSolution(int totalDriveTime, List<Node> nodes, List<DirectedEdge> edges) {
        this(totalDriveTime, nodes, edges, null);
    }

    public DijkstraSolution(int totalDriveTime, List<Node> nodes, List<DirectedEdge> edges, DijkstraSolution preceding) {
        Preconditions.checkNoneNull(nodes,edges);
        this.totalDriveTime = totalDriveTime;
        if (preceding == null) {
            this.nodes = Collections.unmodifiableList(nodes);
            this.edges = Collections.unmodifiableList(edges);
            this.preceding = null;
        } else {
            this.nodes = new UnionList<>(preceding.nodes,nodes);
            this.edges = new UnionList<>(preceding.edges,edges);
            this.preceding = preceding;
        }
    }
    
    @Override
    public String toString() {
        if (nodes.isEmpty()) {
            return "Empty NodeList, length " + totalDriveTime;
        } else {
            StringBuilder sb = new StringBuilder();
            for (Node n : nodes) {
                sb.append(n.nodeId).append(",");
            }
            sb.append(String.format(" Duration %.2f secs (%.2f mins)", totalDriveTime/1000.0, totalDriveTime/60000.0));
            return sb.toString();
        }
    }
    
    public Node getFirstNode() {
        return nodes.get(0);
    }
    
    public Node getLastNode() {
        return nodes.get(nodes.size()-1);
    }
    
    public List<DirectedEdge> getDeltaEdges() {
        if (edges instanceof UnionList) {
            return ((UnionList)edges).getSecondSublist();
        } else {
            return edges;
        }
    }

    public DijkstraSolution getPreceding() {
        return preceding;
    }
}
