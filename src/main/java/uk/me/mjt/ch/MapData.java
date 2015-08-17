
package uk.me.mjt.ch;

import java.util.*;

public class MapData {
    private final HashMap<Long,Node> nodesById;
    
    public MapData(HashMap<Long,Node> nodesById) {
        Preconditions.checkNoneNull(nodesById);
        this.nodesById = nodesById;
    }
    
    public Node get(long nodeId) {
        return nodesById.get(nodeId);
    }
    
    public int size() {
        return nodesById.size();
    }
    
    public Collection<Node> values() {
        return Collections.unmodifiableCollection(nodesById.values());
    }
    
    public Set<Long> keySet() {
        return Collections.unmodifiableSet(nodesById.keySet());
    }
    
    public void add(Node toAdd) {
        Preconditions.checkNoneNull(toAdd);
        Preconditions.require(!nodesById.containsKey(toAdd.nodeId));
        
        nodesById.put(toAdd.nodeId, toAdd);
    }
    
    public void addAll(Collection<Node> toAdd) {
        for (Node n : toAdd) {
            add(n);
        }
    }
    
    public List<Node> chooseRandomNodes(int howMany) {
        Preconditions.require(howMany>0);
        ArrayList<Node> n = new ArrayList<>(nodesById.values());
        Collections.shuffle(n, new Random(12345));
        return new ArrayList(n.subList(0, howMany));
    }
    
    public void removeNodeAndConnectedEdges(Node remove) {
        Preconditions.require(nodesById.containsKey(remove.nodeId), nodesById.get(remove.nodeId).equals(remove));
        for (DirectedEdge de : remove.edgesTo) {
            de.from.edgesFrom.remove(de);
        }

        for (DirectedEdge de : remove.edgesFrom) {
            de.to.edgesTo.remove(de);
        }

        nodesById.remove(remove.nodeId);
    }
    
    public void removeAll(Collection<Node> nodes) {
        for (Node n : nodes) {
            removeNodeAndConnectedEdges(n);
        }
    }

}
