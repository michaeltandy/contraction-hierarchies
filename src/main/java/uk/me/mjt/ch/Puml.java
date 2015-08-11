
package uk.me.mjt.ch;

import java.util.Collection;

/**
 * Generates UML you can feed into PlantUML http://plantuml.com/ to generate
 * abstract diagrams of the graph. Useful for debugging access only / barrier 
 * code where there are multiple nodes in the same place, and zero-length links.
 */
public class Puml {
    
    public static String forGraph(Collection<Node> allNodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        
        for (Node n : allNodes) {
            for (DirectedEdge de : n.edgesFrom) {
                sb.append(edgeToPuml(de));
            }
        }
        
        sb.append("@enduml");
        return sb.toString();
    }
    
    private static String edgeToPuml(DirectedEdge de) {
        return String.format("(Node %d) --> (Node %d) : Edge %d\\ncost %d ms%s\n",
                de.from.nodeId, de.to.nodeId,
                de.edgeId, de.driveTimeMs,
                (de.accessOnly==AccessOnly.TRUE?"\\naccess only":""));
    }

}
