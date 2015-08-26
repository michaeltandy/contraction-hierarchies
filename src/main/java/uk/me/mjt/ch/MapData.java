
package uk.me.mjt.ch;

import java.util.*;

public class MapData {
    private final HashMap<Long,Node> nodesById;
    private final HashMap<Long,TurnRestriction> turnRestrictionsById;
    
    public MapData(HashMap<Long,Node> nodesById) {
        this(nodesById, new HashMap());
    }
    
    public MapData(HashMap<Long,Node> nodesById, HashMap<Long,TurnRestriction> turnRestrictionsById) {
        Preconditions.checkNoneNull(nodesById, turnRestrictionsById);
        this.nodesById = nodesById;
        this.turnRestrictionsById = turnRestrictionsById;
    }
    
    public Node getNodeById(long nodeId) {
        return nodesById.get(nodeId);
    }
    
    public int getNodeCount() {
        return nodesById.size();
    }
    
    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodesById.values());
    }
    
    public Set<Long> getAllNodeIds() {
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
    
    public Set<TurnRestriction> allTurnRestrictions() {
        return Collections.unmodifiableSet(new HashSet<>(turnRestrictionsById.values()));
    }

}
