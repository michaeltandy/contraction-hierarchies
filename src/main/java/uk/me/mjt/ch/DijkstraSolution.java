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
                sb.append(n.nodeId).append(",");
            }
            sb.append(String.format(" length %.2f", totalDistance));
            return sb.toString();
        }
    }
    
    public String toGeoJson() {
        if (nodes.isEmpty()) {
            // TODO decide what's a sensible thing to do here.
            return "{\"type\": \"FeatureCollection\",\"features\": []}";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\": \"LineString\",\"coordinates\": [");
            for (Node n : nodes) {
                sb.append(String.format("[%.6f,%.6f],", n.lon,n.lat));
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append("]}");
            return sb.toString();
        }
    }
    
}
