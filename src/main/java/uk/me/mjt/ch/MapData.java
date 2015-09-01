
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
        if (nodesById.containsKey(toAdd.nodeId)) {
            throw new RuntimeException("Map data already contains node with ID " + toAdd.nodeId);
        }
        
        
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
    
    public void clearAllTurnRestrictions() {
        turnRestrictionsById.clear();
    }
    
    public void validate() {
        HashMap<Long,DirectedEdge> uniqueEdges = new HashMap();
        
        for (Long nodeId : nodesById.keySet()) {
            Node node = nodesById.get(nodeId);
            
            if (node.nodeId != nodeId)
                throw new InvalidMapDataException("Node IDs don't match - " + nodeId + " vs " + node.nodeId);
            
            
            
            for (DirectedEdge de : node.edgesFrom) {
                validateSingleEdge(de);
                if (uniqueEdges.containsKey(de.edgeId)) {
                    DirectedEdge prevWithThisId = uniqueEdges.get(de.edgeId);
                    if (de != prevWithThisId) {
                        throw new InvalidMapDataException("Nonunique edge ID - " + de.edgeId);
                    }
                } else {
                    uniqueEdges.put(de.edgeId, de);
                }
            }
        }
    }
    
    private void validateNeighborLists(Node n) {
        ArrayList<DirectedEdge> fromBefore = new ArrayList<>(n.edgesFrom);
        ArrayList<DirectedEdge> toBefore = new ArrayList<>(n.edgesTo);
        n.sortNeighborLists();
        if (!fromBefore.equals(n.edgesFrom) || !toBefore.equals(n.edgesTo)) {
            throw new InvalidMapDataException("Neigbor lists were unsorted?");
        }
    }
    
    private void validateSingleEdge(DirectedEdge de) {
        Node from = nodesById.get(de.from.nodeId);
        Node to = nodesById.get(de.to.nodeId);
        if (from != de.from)
            throw new InvalidMapDataException("DirectedEdge from node isn't in nodesById - " + from.nodeId + " for de " + de.edgeId);
        if (to != de.to)
            throw new InvalidMapDataException("DirectedEdge to node isn't in nodesById - " + to.nodeId + " for de " + de.edgeId);
        if (!from.edgesFrom.contains(de))
            throw new InvalidMapDataException("From node doesn't have edge in edgesFrom - " + from.nodeId + " for de " + de.edgeId);
        if (!to.edgesTo.contains(de))
            throw new InvalidMapDataException("To node doesn't have edge in edgesTo - " + to.nodeId + " for de " + de.edgeId);
    }
    
    public static class InvalidMapDataException extends RuntimeException {
        private InvalidMapDataException(String reason) {
            super(reason);
        }
    }
    
}

