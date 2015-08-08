
package uk.me.mjt.ch;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public enum AccessOnly {
    TRUE, FALSE;
    
    public static final long ACCESSONLY_START_NODE_ID_PREFIX = 0x4000000000000000L;
    public static final long ACCESSONLY_END_NODE_ID_PREFIX = 0x2000000000000000L;
    public static final long INITIAL_NEW_EDGE_ID = 1000000000L;
    
    public static void stratifyAllAccessOnlyClusters(HashMap<Long,Node> allNodes) {
        List<AccessOnlyCluster> clusters = findAccessOnlyClusters(allNodes.values());
        AtomicLong edgeIdCounter = new AtomicLong(INITIAL_NEW_EDGE_ID);
        
        for (AccessOnlyCluster cluster : clusters) {
            stratifyCluster(allNodes, cluster, edgeIdCounter);
        }
    }
    
    static List<AccessOnlyCluster> findAccessOnlyClusters(Collection<Node> allNodes) {
        Preconditions.checkNoneNull(allNodes);
        
        HashSet<Node> alreadyAssignedToCluster = new HashSet();
        ArrayList<AccessOnlyCluster> clusters = new ArrayList<>();
        
        for (Node n : allNodes) {
            if (n.anyEdgesAccessOnly() && !alreadyAssignedToCluster.contains(n)) {
                AccessOnlyCluster cluster = identifyCluster(n);
                alreadyAssignedToCluster.addAll(cluster.nodes);
                clusters.add(cluster);
            }
        }
        
        return Collections.unmodifiableList(clusters);
    }
    
    static AccessOnlyCluster identifyCluster(Node startPoint) {
        Preconditions.checkNoneNull(startPoint);
        
        AccessOnlyCluster cluster = new AccessOnlyCluster();
        TreeSet<Node> toVisit = new TreeSet<>();
        toVisit.add(startPoint);
        
        while (!toVisit.isEmpty()) {
            Node visiting = toVisit.pollFirst();
            cluster.nodes.add(visiting);
            
            for (DirectedEdge de : accessOnlyEdgesIn(visiting.edgesFrom,visiting.edgesTo) ) {
                if (!cluster.nodes.contains(de.to))
                    toVisit.add(de.to);
                if (!cluster.nodes.contains(de.from))
                    toVisit.add(de.from);
            }
        }
        
        return cluster;
    }
    
    static class AccessOnlyCluster {
        final HashSet<Node> nodes = new HashSet();
    }
    
    private static Collection<DirectedEdge> accessOnlyEdgesIn(List<DirectedEdge> a, List<DirectedEdge> b) {
        return accessOnlyEdgesIn(new UnionList<>(a,b));
    }
    
    private static Collection<DirectedEdge> accessOnlyEdgesIn(Collection<DirectedEdge> toFilter) {
        ArrayList<DirectedEdge> filtered = new ArrayList<>();
        for (DirectedEdge de : toFilter) {
            if (de.accessOnly == AccessOnly.TRUE) {
                filtered.add(de);
            }
        }
        return filtered;
    }
    
    private static void stratifyCluster(HashMap<Long,Node> allNodes, AccessOnlyCluster cluster, AtomicLong edgeIdCounter) {
        HashMap<Long,Node> startStrata = cloneNodesAndConnectionsAddingPrefix(cluster.nodes, ACCESSONLY_START_NODE_ID_PREFIX, edgeIdCounter);
        HashMap<Long,Node> endStrata = cloneNodesAndConnectionsAddingPrefix(cluster.nodes, ACCESSONLY_END_NODE_ID_PREFIX, edgeIdCounter);
        
        allNodes.putAll(startStrata);
        allNodes.putAll(endStrata);
        
        linkBordersAndStratas(cluster, startStrata, endStrata, edgeIdCounter);
        removeAccessOnlyEdgesThatHaveBeenReplaced(cluster);
        removeAccessOnlyNodesThatHaveBeenReplaced(allNodes, cluster);
        
        Node.sortNeighborListsAll(startStrata.values());
        Node.sortNeighborListsAll(endStrata.values());
        Node.sortNeighborListsAll(cluster.nodes);
    }
    
    static HashMap<Long,Node> cloneNodesAndConnectionsAddingPrefix(Collection<Node> toClone, long nodeIdPrefix, AtomicLong edgeIdCounter) {
        HashMap<Long,Node> clones = new HashMap<>();
        
        for (Node n : toClone) {
            long newId = n.nodeId+nodeIdPrefix;
            Node clone = new Node(newId, n.lat, n.lon);
            clones.put(newId, clone);
        }
        
        for (Node n : toClone) {
            for (DirectedEdge de : n.edgesFrom ) {
                if (toClone.contains(de.to)) {
                    long fromId = de.from.nodeId+nodeIdPrefix;
                    long toId = de.to.nodeId+nodeIdPrefix;
                    makeEdgeAndAddToNodes(edgeIdCounter.incrementAndGet(), clones.get(fromId), clones.get(toId), de.driveTimeMs);
                }
            }
        }
        
        Node.sortNeighborListsAll(clones.values());
        return clones;
    }
    
    private static void linkBordersAndStratas(AccessOnlyCluster cluster, HashMap<Long,Node> startStrata, HashMap<Long,Node> endStrata, AtomicLong edgeIdCounter) {
        for (Node n : cluster.nodes) {
            Node startStrataNode = startStrata.get(n.nodeId+ACCESSONLY_START_NODE_ID_PREFIX);
            Node endStrataNode = endStrata.get(n.nodeId+ACCESSONLY_END_NODE_ID_PREFIX);
            if (n.allEdgesAccessOnly()) { // Internal to the cluster
                makeEdgeAndAddToNodes(edgeIdCounter.incrementAndGet(), startStrataNode, endStrataNode, 0);
            } else { // Border to the cluster
                makeEdgeAndAddToNodes(edgeIdCounter.incrementAndGet(), startStrataNode, n, 0);
                makeEdgeAndAddToNodes(edgeIdCounter.incrementAndGet(), n, endStrataNode, 0);
            }
            
        }
    }
    
    private static DirectedEdge makeEdgeAndAddToNodes(long edgeId, Node from, Node to, int driveTimeMs) {
        Preconditions.checkNoneNull(from,to);
        DirectedEdge de = new DirectedEdge(edgeId, from, to, driveTimeMs, AccessOnly.FALSE);
        from.edgesFrom.add(de);
        to.edgesTo.add(de);
        return de;
    }
    
    private static void removeAccessOnlyEdgesThatHaveBeenReplaced(AccessOnlyCluster cluster) {
        for (Node n : cluster.nodes) {
            for (DirectedEdge toRemove : accessOnlyEdgesIn(n.edgesFrom,n.edgesTo)) {
                toRemove.from.edgesFrom.remove(toRemove);
                toRemove.to.edgesTo.remove(toRemove);
            }
        }
    }
    
    private static void removeAccessOnlyNodesThatHaveBeenReplaced(HashMap<Long,Node> allNodes, AccessOnlyCluster cluster) {
        for (Node n : cluster.nodes) {
            if (n.edgesFrom.isEmpty() && n.edgesTo.isEmpty()) {
                allNodes.remove(n.nodeId);
            }
        }
    }
}
