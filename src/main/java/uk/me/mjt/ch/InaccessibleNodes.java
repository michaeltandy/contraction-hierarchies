
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
    public static void removeNodesNotBidirectionallyAccessible(MapData allNodes, Node startNode) {
        System.out.println("Before removal, " + allNodes.getNodeCount() + " nodes.");
        Set<Node> toRemove = findNodesNotBidirectionallyAccessible(allNodes,startNode);
        
        System.out.println("Removing " + toRemove.size() + " nodes.");
        allNodes.removeAll(toRemove);
        
        System.out.println("After removal, " + allNodes.getNodeCount() + " nodes.");
    }
    
    public static Set<Node> findNodesNotBidirectionallyAccessible(MapData allNodes, Node startNode) {
        Set<Node> inaccessibleForwards = findInaccessibleByDirection(allNodes, startNode, Direction.FORWARDS);
        Set<Node> inaccessibleBackwards = findInaccessibleByDirection(allNodes, startNode, Direction.BACKWARDS);
        
        HashSet<Node> notBidirectionallyAccessible = new HashSet(inaccessibleForwards);
        notBidirectionallyAccessible.addAll(inaccessibleBackwards);
        return notBidirectionallyAccessible;
    }
    
    /*public static Set<Node> findNodesNotUnidirectionallyAccessible(HashMap<Long,Node> allNodes, Node startNode) {
        Set<Node> inaccessibleForwards = findInaccessibleByDirection(allNodes, startNode, Direction.FORWARDS);
        Set<Node> inaccessibleBackwards = findInaccessibleByDirection(allNodes, startNode, Direction.BACKWARDS);
        
        HashSet<Node> inaccessible = new HashSet<>();
        for (Node n : inaccessibleForwards) {
            if (inaccessibleBackwards.contains(n)) {
                inaccessible.add(n);
            }
        }
        
        return inaccessible;
    }*/
    
    private static HashSet<Node> findInaccessibleByDirection(MapData allNodes, Node startNode, Direction direction) {
        List<DijkstraSolution> solutionsThisDirection = Dijkstra.dijkstrasAlgorithm(startNode, null, Integer.MAX_VALUE, direction);
        
        HashSet<Node> accessible = new HashSet<>(solutionsThisDirection.size());
        for (DijkstraSolution ds : solutionsThisDirection) {
            accessible.add(ds.getLastNode());
        }
        
        HashSet<Node> inaccessible = new HashSet<>(allNodes.getAllNodes());
        inaccessible.removeAll(accessible);
        return new HashSet<>(inaccessible);
    }

}
