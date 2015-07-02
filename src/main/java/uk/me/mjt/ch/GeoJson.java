
package uk.me.mjt.ch;

import java.util.Collection;
import java.util.List;


public class GeoJson {
    
    public static String linksInBbox(Collection<Node> allNodes, double lat1, double lon1, double lat2, double lon2) {
        return linksConditionally(allNodes, new BboxEdges(lat1, lon1, lat2, lon2));
    }
    
    private static class BboxEdges extends EdgePrintCondition {
        private final double lat1, lon1, lat2, lon2;

        public BboxEdges(double lat1, double lon1, double lat2, double lon2) {
            this.lat1 = lat1; this.lon1 = lon1; this.lat2 = lat2; this.lon2 = lon2;
        }
        
        boolean shouldPrintEdge(DirectedEdge de) {
            return nodeInBbox(de.from) || nodeInBbox(de.to);
        }
        
        boolean nodeInBbox(Node n) {
            return (lat1 <= n.lat && n.lat <= lat2 && lon1 <= n.lon && n.lon <= lon2);
        }
    }
    
    public static String allLinks(Collection<Node> allNodes) {
        return linksConditionally(allNodes, new PrintAllEdges());
    }
    
    public static String allDirectedEdges(Collection<DirectedEdge> deList) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"type\": \"FeatureCollection\", \"features\": [\n");
        
        for (DirectedEdge de : deList) {
            sb.append(directedEdgeToFeature(de)).append(",\n");
        }
        
        if (sb.toString().endsWith(",\n"))
            sb.deleteCharAt(sb.length()-2);
        sb.append("]}");
        return sb.toString();
    }
    
    private static class PrintAllEdges extends EdgePrintCondition {
        boolean shouldPrintEdge(DirectedEdge de) { return true; }
    }
    
    private static String linksConditionally(Collection<Node> allNodes, EdgePrintCondition condition) {
        Preconditions.checkNoneNull(allNodes,condition);
        
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"type\": \"FeatureCollection\", \"features\": [\n");
        
        for (Node n : allNodes) {
            for (DirectedEdge de : n.edgesFrom) {
                if (condition.shouldPrintEdge(de)) {
                    sb.append(directedEdgeToFeature(de)).append(",\n");
                }
            }
        }
        if (sb.toString().endsWith(",\n"))
            sb.deleteCharAt(sb.length()-2);
        sb.append("]}");
        return sb.toString();
    }
    
    
    
    private static String directedEdgeToFeature(DirectedEdge de) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"Feature\",\"properties\": {},\"geometry\": {\"type\": \"LineString\",\"coordinates\": [");
        sb.append(String.format("[%.6f,%.6f],[%.6f,%.6f]", de.from.lon, de.from.lat, de.to.lon, de.to.lat));
        sb.append("]}}");
        return sb.toString();
    }
    
    public static String solution(DijkstraSolution solution) {
        List<Node> nodes = solution.nodes;
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
    
    private static abstract class EdgePrintCondition {
        abstract boolean shouldPrintEdge(DirectedEdge de);
    }

}
