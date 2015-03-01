package uk.me.mjt.ch;

import java.util.LinkedList;

public class DijkstraSolution  {

    public float totalDistance = 0;
    public LinkedList<Node> nodes = new LinkedList<Node>();
    public LinkedList<DirectedEdge> edges = new LinkedList<DirectedEdge>();

    @Override
    public String toString() {
        if (nodes.isEmpty()) {
            return "Empty NodeList, length " + totalDistance;
        } else {
            StringBuilder sb = new StringBuilder();
            for (Node n : nodes) {
                sb.append(n.name).append(",");
            }
            sb.append(String.format(" length %.3f", totalDistance));
            return sb.toString();
        }
    }
}
