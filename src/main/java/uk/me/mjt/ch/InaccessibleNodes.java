
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
    public static void removeNodesNotBidirectionallyAccessible(HashMap<Long,Node> allNodes, Node startNode) {
        System.out.println("Before removal, " + allNodes.size() + " nodes.");
        Set<Node> inaccessibleForwards = findInaccessibleByDirection(allNodes, startNode, Direction.FORWARDS);
        Set<Node> inaccessibleBackwards = findInaccessibleByDirection(allNodes, startNode, Direction.BACKWARDS);
        
        HashSet<Node> toRemove = new HashSet(inaccessibleForwards);
        toRemove.addAll(inaccessibleBackwards);
        
        System.out.println("Removing " + toRemove.size() + " nodes; " +
                inaccessibleForwards.size() + " of which can't be accessed forwards and " + 
                inaccessibleBackwards.size() + " of which can't be accessed backwards.");
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
    
    public static Set<Node> findNodesNotUnidirectionallyAccessible(HashMap<Long,Node> allNodes, Node startNode) {
        Set<Node> inaccessibleForwards = findInaccessibleByDirection(allNodes, startNode, Direction.FORWARDS);
        Set<Node> inaccessibleBackwards = findInaccessibleByDirection(allNodes, startNode, Direction.BACKWARDS);
        
        HashSet<Node> inaccessible = new HashSet<>();
        for (Node n : inaccessibleForwards) {
            if (inaccessibleBackwards.contains(n)) {
                inaccessible.add(n);
            }
        }
        
        return inaccessible;
    }
    
    private static HashSet<Node> findInaccessibleByDirection(HashMap<Long,Node> allNodes, Node startNode, Direction direction) {
        List<DijkstraSolution> solutionsThisDirection = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, null, Integer.MAX_VALUE, direction);
        
        HashSet<Node> accessible = new HashSet<>(solutionsThisDirection.size());
        for (DijkstraSolution ds : solutionsThisDirection) {
            accessible.add(ds.getLastNode());
        }
        
        HashSet<Node> inaccessible = new HashSet<>(allNodes.values());
        inaccessible.removeAll(accessible);
        return new HashSet<>(inaccessible);
    }

}
