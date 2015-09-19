
package uk.me.mjt.ch;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import uk.me.mjt.ch.Dijkstra.Direction;

public enum AccessOnly {
    TRUE, FALSE;
    
    public static void stratifyMarkedAndImplicitAccessOnlyClusters(MapData allNodes, Node startPoint) {
        markImplicitlyAccessOnlyEdges(allNodes, startPoint);
        stratifyMarkedAccessOnlyClusters(allNodes);
    }
    
    /**
     * Mark edges that, while not tagged access only, you can only get to or
     * can only leave via access only edges.
     * https://www.openstreetmap.org/node/1653073939
     * https://www.openstreetmap.org/node/1499442487
     * https://www.openstreetmap.org/way/151879439
     */
    private static void markImplicitlyAccessOnlyEdges(MapData allNodes, Node startPoint) {
        if (!InaccessibleNodes.findNodesNotBidirectionallyAccessible(allNodes,startPoint).isEmpty()) {
            throw new IllegalArgumentException("We can only mark implicitly access-only edges for graphs "
                    + "where all nodes are bidirectionally accessible. This one doesn't seem to be!");
        }
        
        HashSet<DirectedEdge> accessibleForwards = accessibleEdgesFrom(startPoint, Direction.FORWARDS, AccessOnly.FALSE);
        HashSet<DirectedEdge> accessibleBackwards = accessibleEdgesFrom(startPoint, Direction.BACKWARDS, AccessOnly.FALSE);
        
        HashSet<DirectedEdge> implicitlyAccessOnly = new HashSet();
        for (Node n : allNodes.getAllNodes()) {
            for (DirectedEdge de : n.edgesFrom) {
                if (accessibleForwards.contains(de) && accessibleBackwards.contains(de)) {
                    // Looks fine to me!
                } else {
                    implicitlyAccessOnly.add(de);
                }
            }
        }
        
        for (DirectedEdge de : implicitlyAccessOnly) {
            if (de.accessOnly == AccessOnly.FALSE) {
                de.accessOnly = AccessOnly.TRUE;
            }
        }
        allNodes.validate();
    }
    
    private static HashSet<DirectedEdge> accessibleEdgesFrom(Node startPoint, Direction direction, AccessOnly accessOnly) {
        HashSet<DirectedEdge> result = new HashSet<>();
        HashSet<Node> visited = new HashSet<>();
        TreeSet<Node> toVisit = new TreeSet<>();
        toVisit.add(startPoint);
        
        while (!toVisit.isEmpty()) {
            Node visiting = toVisit.pollFirst();
            visited.add(visiting);
            
            
            Collection<DirectedEdge> toFollow;
            if (direction==Direction.FORWARDS) {
                toFollow=visiting.edgesFrom;
            } else if (direction==Direction.BACKWARDS) {
                toFollow=visiting.edgesTo;
            } else {
                toFollow=new UnionList<>(visiting.edgesFrom,visiting.edgesTo);
            }
            
            for (DirectedEdge de : toFollow ) {
                if (de.accessOnly == accessOnly) {
                    if (!visited.contains(de.to))
                        toVisit.add(de.to);
                    if (!visited.contains(de.from))
                        toVisit.add(de.from);
                    result.add(de);
                }
            }
        }
        
        return result;
    }
    
    public static void stratifyMarkedAccessOnlyClusters(MapData allNodes) {
        List<AccessOnlyCluster> clusters = findAccessOnlyClusters(allNodes.getAllNodes());
        
        for (AccessOnlyCluster cluster : clusters) {
            stratifyCluster(allNodes, cluster);
        }
    }
    
    static List<AccessOnlyCluster> findAccessOnlyClusters(Collection<Node> allNodes) {
        Preconditions.checkNoneNull(allNodes);
        
        HashSet<Node> alreadyAssignedToCluster = new HashSet();
        ArrayList<AccessOnlyCluster> clusters = new ArrayList<>();
        
        for (Node n : allNodes) {
            if (n.anyEdgesAccessOnly() && !alreadyAssignedToCluster.contains(n)) {
                AccessOnlyCluster cluster = identifyCluster(n);
                for (Node cn : cluster.nodes) {
                    if (alreadyAssignedToCluster.contains(cn)) {
                        throw new RuntimeException("New cluster starting at " + n + " contains nodes already in a cluster, " + cn);
                    }
                }
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
        
        boolean printCluster = false;
        
        while (!toVisit.isEmpty()) {
            Node visiting = toVisit.pollFirst();
            cluster.nodes.add(visiting);
            
            if (visiting.nodeId == 443312112L) {
                printCluster = true;
            }
            
            for (DirectedEdge de : accessOnlyEdgesIn(visiting.edgesFrom,visiting.edgesTo) ) {
                if (!cluster.nodes.contains(de.to))
                    toVisit.add(de.to);
                if (!cluster.nodes.contains(de.from))
                    toVisit.add(de.from);
            }
        }
        
        if (printCluster) {
            System.out.println("From " + startPoint + "\n" + Puml.forNodes(cluster.nodes));
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
    
    private static void stratifyCluster(MapData allNodes, AccessOnlyCluster cluster) {
        HashMap<Long,Node> startStrata = cloneNodesAndConnectionsRenumbering(cluster.nodes, allNodes.getNodeIdCounter(), allNodes.getEdgeIdCounter());
        HashMap<Long,Node> endStrata = cloneNodesAndConnectionsRenumbering(cluster.nodes, allNodes.getNodeIdCounter(), allNodes.getEdgeIdCounter());
        
        allNodes.addAllSynthetic(startStrata);
        allNodes.addAllSynthetic(endStrata);
        
        linkBordersAndStratas(cluster, startStrata, endStrata, allNodes.getEdgeIdCounter());
        removeAccessOnlyEdgesThatHaveBeenReplaced(cluster);
        removeAccessOnlyNodesThatHaveBeenReplaced(allNodes, cluster);
        
        Node.sortNeighborListsAll(startStrata.values());
        Node.sortNeighborListsAll(endStrata.values());
        Node.sortNeighborListsAll(cluster.nodes);
    }
    
    static HashMap<Long,Node> cloneNodesAndConnectionsRenumbering(Collection<Node> toClone, AtomicLong nodeIdCounter, AtomicLong edgeIdCounter) {
        HashMap<Long,Node> clonesByOldId = new HashMap<>();
        
        for (Node n : toClone) {
            long newId = nodeIdCounter.incrementAndGet();
            Node clone = new Node(newId, n.lat, n.lon, Barrier.FALSE);
            clonesByOldId.put(n.nodeId, clone);
        }
        
        for (Node n : toClone) {
            for (DirectedEdge de : n.edgesFrom ) {
                if (toClone.contains(de.to)) {
                    Node fromReplacement = clonesByOldId.get(de.from.nodeId);
                    Node toReplacement = clonesByOldId.get(de.to.nodeId);
                    makeEdgeAndAddToNodes(edgeIdCounter.incrementAndGet(), fromReplacement, toReplacement, de.driveTimeMs);
                }
            }
        }
        
        Node.sortNeighborListsAll(clonesByOldId.values());
        return clonesByOldId;
    }
    
    private static void linkBordersAndStratas(AccessOnlyCluster cluster, HashMap<Long,Node> startStrata, HashMap<Long,Node> endStrata, AtomicLong edgeIdCounter) {
        for (Node n : cluster.nodes) {
            Node startStrataNode = startStrata.get(n.nodeId);
            Node endStrataNode = endStrata.get(n.nodeId);
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
    
    private static void removeAccessOnlyNodesThatHaveBeenReplaced(MapData allNodes, AccessOnlyCluster cluster) {
        for (Node n : cluster.nodes) {
            if (n.edgesFrom.isEmpty() && n.edgesTo.isEmpty()) {
                allNodes.removeNodeAndConnectedEdges(n);
            }
        }
    }
}
