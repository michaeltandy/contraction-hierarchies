package uk.me.mjt.ch;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DijkstraSolution  {

    public final int totalDriveTime;
    public final List<Node> nodes;
    public final List<DirectedEdge> edges;

    public DijkstraSolution(int totalDriveTime, List<Node> nodes, List<DirectedEdge> edges) {
        Preconditions.checkNoneNull(nodes,edges);
        this.totalDriveTime = totalDriveTime;
        this.nodes = Collections.unmodifiableList(nodes);
        this.edges = Collections.unmodifiableList(edges);
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
    
    
}
