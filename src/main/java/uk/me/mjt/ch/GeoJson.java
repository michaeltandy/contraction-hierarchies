
package uk.me.mjt.ch;

import java.util.Collection;


public class GeoJson {
    
    public static String allLinks(Collection<Node> allNodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"type\": \"FeatureCollection\", \"features\": [\n");
        
        for (Node n : allNodes) {
            for (DirectedEdge de : n.edgesFrom) {
                sb.append(directedEdgeToFeature(de)).append(",\n");
            }
        }
        sb.deleteCharAt(sb.length()-2);
        sb.append("]}");
        return sb.toString();
    }
    
    public static String directedEdgeToFeature(DirectedEdge de) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"Feature\",\"properties\": {},\"geometry\": {\"type\": \"LineString\",\"coordinates\": [");
        sb.append(String.format("[%.6f,%.6f],[%.6f,%.6f]", de.from.lon, de.from.lat, de.to.lon, de.to.lat));
        sb.append("]}}");
        return sb.toString();
    }

}
