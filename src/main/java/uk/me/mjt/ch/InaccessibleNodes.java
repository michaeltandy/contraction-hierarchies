
package uk.me.mjt.ch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uk.me.mjt.ch.Dijkstra.Direction;


public class InaccessibleNodes {
    
    /**
     * This only works on the uncontracted graph.
     */
    public static void removeInaccessibleNodes(HashMap<Long,Node> allNodes, Node startNode) {
        System.out.println("Before removal, " + allNodes.size() + " nodes.");
        Set<Node> toRemove = findInaccessibleNodes(allNodes, startNode);
        
        System.out.println("Removing " + toRemove.size() + " nodes.");
        removeNodes(allNodes, toRemove);
        
        System.out.println("After removal, " + allNodes.size() + " nodes.");
    }
    
    private static void removeNodes(HashMap<Long,Node> allNodes, Set<Node> toRemove) {
        for (Node remove : toRemove) {
            for (DirectedEdge de : remove.edgesTo) {
                de.from.edgesFrom.remove(de);
            }
            
            for (DirectedEdge de : remove.edgesFrom) {
                de.to.edgesTo.remove(de);
            }
            
            allNodes.remove(remove.nodeId);
        }
    }
    
    public static Set<Node> findInaccessibleNodes(HashMap<Long,Node> allNodes, Node startNode) {
        HashSet<Node> forwards = findAccessibleByDirection(allNodes, startNode, Direction.FORWARDS);
        HashSet<Node> backwards = findAccessibleByDirection(allNodes, startNode, Direction.BACKWARDS);
        
        HashSet<Node> inaccessible = new HashSet<>(forwards.size());
        
        for (Node ds : allNodes.values()) {
            if (!backwards.contains(ds) || !forwards.contains(ds)) {
                inaccessible.add(ds);
            }
        }
        
        return inaccessible;
    }
    
    private static HashSet<Node> findAccessibleByDirection(HashMap<Long,Node> allNodes, Node startNode, Direction direction) {
        List<DijkstraSolution> solutionsThisDirection = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, null, Float.POSITIVE_INFINITY, direction);
        
        HashSet<Node> accessible = new HashSet<>(solutionsThisDirection.size());
        for (DijkstraSolution ds : solutionsThisDirection) {
            accessible.add(ds.getLastNode());
        }
        return accessible;
    }

}
