package uk.me.mjt.ch;

import uk.me.mjt.ch.Dijkstra.Direction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author michael
 */
public class GraphContractor {
    private final HashMap<Long,Node> allNodes;
    private long maxEdgeId;

    public GraphContractor(HashMap<Long,Node> allNodes) {
        this.allNodes = allNodes;
        this.maxEdgeId = 4294967296L;
    }

    private int getEdgeRemovedCount(Node n) {
        return n.getCountOutgoingUncontractedEdges() + n.getCountIncomingUncontractedEdges();
    }

    public int getBalanceOfEdgesRemoved(Node n) {
        int edgesRemoved = getEdgeRemovedCount(n);
        int shortcutsAdded = findShortcuts(n).size();
        return edgesRemoved-shortcutsAdded;
    }

    public void contractNode(Node n, int order, ArrayList<DirectedEdge> shortcuts) {
        for (DirectedEdge se : shortcuts) {
            se.from.edgesFrom.add(se);
            se.to.edgesTo.add(se);
        }
        n.contractionOrder = order;
    }

    public ArrayList<DirectedEdge> findShortcuts(Node n) {
        ArrayList<DirectedEdge> shortcuts = new ArrayList<DirectedEdge>();

        HashSet<Node> destinationNodes = new HashSet<Node>();
        float maxOutDist = 0;
        for (DirectedEdge outgoing : n.edgesFrom) {
            if (!outgoing.to.isContracted()) {
                destinationNodes.add(outgoing.to);
                if (outgoing.distance > maxOutDist)
                    maxOutDist = outgoing.distance;
            }
        }

        for (DirectedEdge incoming : n.edgesTo) {
            Node startNode = incoming.from;
            if (startNode.isContracted())
                continue;

            List<DijkstraSolution> routed = Dijkstra.dijkstrasAlgorithm(allNodes,
                                                    startNode,
                                                    new HashSet<Node>(destinationNodes),
                                                    incoming.distance+maxOutDist,
                                                    Direction.FORWARDS);

            for (DijkstraSolution ds : routed) {
                //System.out.print(ds + ", " + ds.totalDistance);
                if (ds.nodes.size() == 3 && ds.nodes.get(1)==n) {
                    //System.out.println("  Shortcut " + ds + ", " + ds.totalDistance);
                    shortcuts.add(new DirectedEdge(maxEdgeId++,
                                                    ds.nodes.getFirst(),
                                                    ds.nodes.getLast(),
                                                    ds.totalDistance,
                                                    ds.edges.get(0),
                                                    ds.edges.get(1)));
                } else {
                    //System.out.println();
                }
            }
        }

        return shortcuts;
    }


    TreeMap<ContractionOrdering,Node> contractionOrder = new TreeMap<ContractionOrdering,Node>();

    public void initialiseContractionOrder() {
        for (Node n : allNodes.values()) {
            if (n.contractionAllowed && !n.isContracted()) {
                contractionOrder.put(new ContractionOrdering(n,getBalanceOfEdgesRemoved(n)), n);
            }
        }
    }
    
    public void reinitialiseContractionOrder() {
        System.out.println("Reinitialising contraction order...");
        ArrayList<Node> priorSet = new ArrayList<>(contractionOrder.values());
        contractionOrder.clear();
        for (Node n : priorSet) {
            if (n.contractionAllowed && !n.isContracted()) {
                contractionOrder.put(new ContractionOrdering(n,getBalanceOfEdgesRemoved(n)), n);
            }
        }
    }

    public void contractAll() {
        int contractionProgress = 1;

        Node n;
        while ((n = lazyContractNextNode(contractionProgress++, true)) != null) {
            //System.out.println("Contracted " + n);
        }
    }

    private Node lazyContractNextNode(int contractionProgress, boolean includeUnprofitable) {
        Map.Entry<ContractionOrdering,Node> next = contractionOrder.pollLastEntry();
        boolean recentlySorted = false;
        while (next != null) {
            ContractionOrdering oldOrder = next.getKey();
            Node n = next.getValue();

            ArrayList<DirectedEdge> shortcuts = findShortcuts(n);
            
            int balanceOfEdgesRemoved = getEdgeRemovedCount(n)-shortcuts.size();
            boolean profitable = balanceOfEdgesRemoved > -30; // OK, so our threshold for 'profitable' has a bit of margin on it.
            
            if (profitable || (recentlySorted && includeUnprofitable)) {
                recentlySorted = false;
                
                ContractionOrdering newOrder = new ContractionOrdering(n,balanceOfEdgesRemoved);
                if (contractionOrder.isEmpty() 
                        || newOrder.compareTo(oldOrder) >= 0 
                        || newOrder.compareTo(contractionOrder.lastKey()) >= 0) {
                    // If the ContractionOrdering is unchanged, or has changed but 
                    // not enough to move this node off the top spot, contract.
                    contractNode(n,contractionProgress,shortcuts);
                    return n;
                } else {
                    // Otherwise
                    contractionOrder.put(newOrder, n);
                }
                
            } else if (!recentlySorted) {
                contractionOrder.put(oldOrder, n);
                reinitialiseContractionOrder();
                recentlySorted = true;
            } else if (!includeUnprofitable) {
                System.out.println("Contraction became unprofitable with " + contractionOrder.size() + " nodes remaining.");
                return null;
            }
            next = contractionOrder.pollLastEntry();
        }
        return null;
    }

    public static int getMaxContractionDepth(List<DirectedEdge> de) {
        int maxContractionDepth = 0;
        for (DirectedEdge o : de) {
            maxContractionDepth = Math.max(maxContractionDepth, o.contractionDepth);
        }
        return maxContractionDepth;
    }

    private class ContractionOrdering implements Comparable<ContractionOrdering> {
        final int edgeCountReduction;
        final int contractionDepth;
        final int namehash; // If everything else is equal we don't care about the order - but it's useful for it to be stable between runs.

        public ContractionOrdering(Node n, int edgeCountReduction) {
            this.edgeCountReduction = edgeCountReduction;
            contractionDepth = Math.max(getMaxContractionDepth(n.edgesFrom), getMaxContractionDepth(n.edgesTo));
            namehash = improveHash(n.hashCode());
        }
        
        private int improveHash(int initialHash) {
            // If nodes happen to go 1,2,3,4,5,6... we'd rather not contract
            // them in that order.
            return 1103515245 * initialHash + 12345;
        }

        public int compareTo(ContractionOrdering o) {
            if (o == null) {
                return -1;
            } else if (this.contractionDepth != o.contractionDepth) {
                return o.contractionDepth-this.contractionDepth;
            } else if (this.edgeCountReduction != o.edgeCountReduction) {
                return this.edgeCountReduction-o.edgeCountReduction;
            } else if (this.namehash != o.namehash) {
                return this.namehash-o.namehash;
            } else {
                return 0;
            }
        }

    }

}
