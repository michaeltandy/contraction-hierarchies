
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
        return String.format("(Node %d) --> (Node %d) : Edge %d\\ncost %d ms%s\n",
                de.from.nodeId, de.to.nodeId,
                de.edgeId, de.driveTimeMs,
                (de.accessOnly==AccessOnly.TRUE?"\\naccess only":""));
    }

}
