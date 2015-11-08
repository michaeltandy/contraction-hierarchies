
package uk.me.mjt.ch;

import java.util.Collection;
import java.util.HashSet;

/**
 * Generates UML you can feed into PlantUML http://plantuml.com/ to generate
 * abstract diagrams of the graph. Useful for debugging access only / barrier 
 * code where there are multiple nodes in the same place, and zero-length links.
 */
public class Puml {
    
    public static String forNodes(Collection<Node> allNodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        
        HashSet<DirectedEdge> alreadyVisited = new HashSet();
        
        for (Node n : allNodes) {
            for (DirectedEdge de : n.getEdgesFromAndTo()) {
                if (!alreadyVisited.contains(de)) {
                    sb.append(edgeToPuml(de));
                    alreadyVisited.add(de);
                }
            }
        }
        
        sb.append("@enduml");
        return sb.toString();
    }
    
    private static String edgeToPuml(DirectedEdge de) {
        return String.format("%s --> %s : %s\n",
                nodeToString(de.from),
                nodeToString(de.to),
                edgeToString(de));
    }
    
    private static String nodeToString(Node n) {
        if (n.nodeId==n.sourceDataNodeId) {
            return "(Node "+n.nodeId+")";
        } else {
            return "(Node "+n.nodeId+ "\\nOriginally "+n.sourceDataNodeId+")";
        }
    }
    
    private static String edgeToString(DirectedEdge de) {
        String s;
        if (de.edgeId==de.sourceDataEdgeId) {
            s="Edge " + de.edgeId;
        } else {
            s="Edge " + de.edgeId + "\\nOriginally " + de.sourceDataEdgeId;
        }
        return s+ "\\ncost " + de.driveTimeMs + " ms" + (de.accessOnly==AccessOnly.TRUE?"\\naccess only":"");
    }

}
