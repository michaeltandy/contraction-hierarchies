
package uk.me.mjt.ch;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MapData {
    private final HashMap<Long,Node> nodesById;
    private final HashMap<Long,TurnRestriction> turnRestrictionsById;
    private final AtomicLong maxEdgeId = new AtomicLong();
    private final AtomicLong maxNodeId = new AtomicLong();
    private final Multimap<Long,Node> syntheticNodesByIdOfEquivalent = new Multimap<>();
    
    public MapData(Collection<Node> nodes) {
        this(indexNodesById(nodes), new HashMap());
    }
    
    public MapData(HashMap<Long,Node> nodesById) {
        this(nodesById, new HashMap());
    }
    
    public MapData(HashMap<Long,Node> nodesById, HashMap<Long,TurnRestriction> turnRestrictionsById) {
        Preconditions.checkNoneNull(nodesById, turnRestrictionsById);
        this.nodesById = nodesById;
        this.turnRestrictionsById = turnRestrictionsById;
        setMaxNodeAndEdgeId();
        indexSyntheticEquivalents();
    }
    
    private void setMaxNodeAndEdgeId() {
        for (Node n : nodesById.values()) {
            if (n.nodeId > maxNodeId.get()) {
                maxNodeId.set(n.nodeId);
            }
            for (DirectedEdge de : n.edgesFrom) {
                if (de.edgeId > maxEdgeId.get()) {
                    maxEdgeId.set(de.edgeId);
                }
            }
        }
    }
    
    private void indexSyntheticEquivalents() {
        for (Node n : nodesById.values()) {
            if (n.isSynthetic()) {
                syntheticNodesByIdOfEquivalent.add(n.sourceDataNodeId, n);
            }
        }
    }
    
    private static HashMap<Long,Node> indexNodesById(Collection<Node> nodes) {
        HashMap<Long,Node> hm = new HashMap(nodes.size());
        for (Node n : nodes) {
            hm.put(n.nodeId, n);
        }
        return hm;
    }
    
    public AtomicLong getEdgeIdCounter() {
        return maxEdgeId;
    }
    
    public AtomicLong getNodeIdCounter() {
        return maxNodeId;
    }
    
    public Node getNodeById(long nodeId) {
        return nodesById.get(nodeId);
    }
    
    public List<Node> getNodeByIdAndSyntheticEquivalents(long nodeId) {
        ArrayList<Node> result = new ArrayList<>();
        if (nodesById.containsKey(nodeId)) {
            result.add(nodesById.get(nodeId));
        }
        result.addAll(syntheticNodesByIdOfEquivalent.get(nodeId));
        return result;
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
        if (toAdd.isSynthetic()) {
            syntheticNodesByIdOfEquivalent.add(toAdd.sourceDataNodeId, toAdd);
        }
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
        if (remove.isSynthetic())
            syntheticNodesByIdOfEquivalent.removeValueForKey(remove.sourceDataNodeId, remove);
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
        validateEdgeIdsUnique();
        
        for (Long nodeId : nodesById.keySet()) {
            validateSingleNode(nodeId);
        }
    }
    
    private void validateEdgeIdsUnique() {
        int roughEstimateNodeCount = 3*nodesById.size();
        HashMap<Long,DirectedEdge> uniqueEdges = new HashMap(roughEstimateNodeCount);
        
        for (Node node : nodesById.values()) {
            for (DirectedEdge de : node.edgesFrom) {
                if (uniqueEdges.containsKey(de.edgeId)) {
                    DirectedEdge prevWithThisId = uniqueEdges.get(de.edgeId);
                    if (de != prevWithThisId) {
                        throw new InvalidMapDataException("Nonunique edge ID - " + de.edgeId +
                                " " + de.toDetailedString() + " vs " + prevWithThisId.toDetailedString());
                    }
                } else {
                    uniqueEdges.put(de.edgeId, de);
                }
            }
        }
    }
    
    private void validateSingleNode(long nodeId) {
        Node node = nodesById.get(nodeId);
        
        if (node.nodeId != nodeId)
            throw new InvalidMapDataException("Node IDs don't match - " + nodeId + " vs " + node.nodeId);
        
        if (nodeId > maxNodeId.get())
            throw new InvalidMapDataException("Node ID exceeds maxNodeId - " + maxNodeId.get() + " vs " + node);
        
        validateNeighborLists(node);
        
        for (DirectedEdge de : node.edgesFrom) {
            validateSingleEdge(de);
        }
    }
    
    private void validateNeighborLists(Node n) {
        ArrayList<DirectedEdge> fromBefore = new ArrayList<>(n.edgesFrom);
        ArrayList<DirectedEdge> toBefore = new ArrayList<>(n.edgesTo);
        n.sortNeighborLists();
        if (!fromBefore.equals(n.edgesFrom) || !toBefore.equals(n.edgesTo)) {
            throw new InvalidMapDataException("Neigbor lists were unsorted - Node " + n.nodeId);
        }
    }
    
    private void validateSingleEdge(DirectedEdge de) {
        if (de.edgeId > maxEdgeId.get())
            throw new InvalidMapDataException("DirectedEdge ID exceeds maxEdgeId - " + maxEdgeId.get() + " vs " + de.toDetailedString());
        
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
    
    public List<Node> nodesInBbox(double lat1, double lon1, double lat2, double lon2) {
        ArrayList<Node> result = new ArrayList();
        for (Node n : nodesById.values()) {
            if (lat1 <= n.lat && n.lat <= lat2 && lon1 <= n.lon && n.lon <= lon2) {
                result.add(n);
            }
        }
        return result;
    }
    
}

