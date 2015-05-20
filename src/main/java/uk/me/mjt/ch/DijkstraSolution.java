package uk.me.mjt.ch;

import java.util.LinkedList;

public class DijkstraSolution  {

    public int totalDriveTime = 0;
    public LinkedList<Node> nodes = new LinkedList<Node>();
    public LinkedList<DirectedEdge> edges = new LinkedList<DirectedEdge>();

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
    
    
}
