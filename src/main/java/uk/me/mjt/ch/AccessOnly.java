
package uk.me.mjt.ch;

import java.util.*;

public enum AccessOnly {
    TRUE, FALSE;
    
    
    
    public static List<AccessOnlyCluster> findAccessOnlyClusters(Collection<Node> allNodes) {
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
    
    public static AccessOnlyCluster identifyCluster(Node startPoint) {
        Preconditions.checkNoneNull(startPoint);
        
        AccessOnlyCluster cluster = new AccessOnlyCluster();
        TreeSet<Node> toVisit = new TreeSet<>();
        toVisit.add(startPoint);
        
        while (!toVisit.isEmpty()) {
            Node visiting = toVisit.pollFirst();
            cluster.nodes.add(visiting);
            
            for (DirectedEdge de : new UnionList<>(visiting.edgesFrom,visiting.edgesTo) ) {
                if (de.accessOnly == AccessOnly.TRUE) {
                    if (!cluster.nodes.contains(de.to))
                        toVisit.add(de.to);
                    if (!cluster.nodes.contains(de.from))
                        toVisit.add(de.from);
                    cluster.edges.add(de);
                }
            }
        }
        
        return cluster;
    }
    
    public static class AccessOnlyCluster {
        final HashSet<Node> nodes = new HashSet();
        final HashSet<DirectedEdge> edges = new HashSet();
    }
    
}
