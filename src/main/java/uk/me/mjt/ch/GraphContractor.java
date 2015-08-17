package uk.me.mjt.ch;

import uk.me.mjt.ch.Dijkstra.Direction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphContractor {
    private final MapData allNodes;
    private long maxEdgeId;
    
    private final AtomicInteger findShortcutsCalls = new AtomicInteger();
    private long nodePreContractChecks = 0;
    private long nodePreContractChecksPassed = 0;
    private long millisSpentOnContractionOrdering = 0;
    
    private ExecutorService es = Executors.newFixedThreadPool(8);
    
    public GraphContractor(MapData allNodes) {
        this.allNodes = allNodes;
        this.maxEdgeId = 5000000000L;
    }

    private int getEdgeRemovedCount(Node n) {
        return n.getCountOutgoingUncontractedEdges() + n.getCountIncomingUncontractedEdges();
    }

    private int getBalanceOfEdgesRemoved(Node n) {
        int edgesRemoved = getEdgeRemovedCount(n);
        int shortcutsAdded = findShortcuts(n).size();
        return edgesRemoved-shortcutsAdded;
    }

    public void contractNode(Node n, int order, ArrayList<DirectedEdge> shortcuts) {
        for (DirectedEdge s : shortcuts) {
            DirectedEdge newShortcut = s.cloneWithEdgeId(maxEdgeId++);
            newShortcut.from.edgesFrom.add(newShortcut);
            newShortcut.to.edgesTo.add(newShortcut);
        }
        n.contractionOrder = order;
    }
    
    public ArrayList<DirectedEdge> findShortcuts(Node n) {
        findShortcutsCalls.incrementAndGet();
        ArrayList<DirectedEdge> shortcuts = new ArrayList<DirectedEdge>();
        
        HashSet<Node> destinationNodes = new HashSet<Node>();
        int maxOutTime = 0;
        for (DirectedEdge outgoing : n.edgesFrom) {
            if (!outgoing.to.isContracted()) {
                destinationNodes.add(outgoing.to);
                if (outgoing.driveTimeMs > maxOutTime)
                    maxOutTime = outgoing.driveTimeMs;
            }
        }
        
        for (DirectedEdge incoming : n.edgesTo) {
            Node startNode = incoming.from;
            if (startNode.isContracted())
                continue;
            
            List<DijkstraSolution> routed = Dijkstra.dijkstrasAlgorithm(
                    startNode,
                    new HashSet<>(destinationNodes),
                    incoming.driveTimeMs+maxOutTime,
                    Direction.FORWARDS);
            
            for (DijkstraSolution ds : routed) {
                if (ds.nodes.size() == 3 && ds.nodes.get(1)==n) {
                    shortcuts.add(new DirectedEdge(DirectedEdge.PLACEHOLDER_ID,
                            ds.getFirstNode(),
                            ds.getLastNode(),
                            ds.totalDriveTimeMs,
                            ds.edges.get(0),
                            ds.edges.get(1)));
                } else {
                    //System.out.println();
                }
            }
        }
        
        return shortcuts;
    }


    BidirectionalTreeMap<ContractionOrdering,Node> contractionOrder = new BidirectionalTreeMap<>();

    public void initialiseContractionOrder() {
        long orderingStart = System.currentTimeMillis();
        parallelInitContractionOrder();
        long orderingDuration = System.currentTimeMillis()-orderingStart;
        millisSpentOnContractionOrdering += orderingDuration;
        System.out.println("Generated contraction order for " + contractionOrder.size() + 
                " nodes in " + orderingDuration + " ms.");
    }
    
    private void parallelInitContractionOrder() {
        ArrayList<Callable<KeyValue>> callables = new ArrayList(allNodes.size());
        for (final Node n : allNodes.values()) {
            if (n.contractionAllowed && !n.isContracted()) {
                callables.add(new Callable<KeyValue>() {
                    public KeyValue call() throws Exception {
                        ContractionOrdering key = new ContractionOrdering(n,getBalanceOfEdgesRemoved(n));
                        return new KeyValue(key, n);
                    }
                });
            }
        }
            
        try {
            List<Future<KeyValue>> kvs = es.invokeAll(callables);

            contractionOrder.clear();
            for (Future<KeyValue> f : kvs) {
                KeyValue kv = f.get();
                contractionOrder.put(kv.key, kv.value);
            }
            
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void contractAll() {
        int contractionProgress = 1;
        
        long startTime = System.currentTimeMillis();
        long recentTime = System.currentTimeMillis();
        long recentPreContractChecks = 0;
        long recentPreContractChecksPassed = 0;
        long recentOrderingMillis = 0;

        Node n;
        while ((n = lazyContractNextNode(contractionProgress++, true)) != null) {
            reorderImmediateNeighbors(n);
            
            //System.out.println("Contracted " + n);
            if (contractionOrder.size() % 10000 == 0) {
                long now = System.currentTimeMillis();
                long runTimeSoFar = now-startTime;
                //long orderingTimeThisRun = millisSpentOnContractionOrdering-recentOrderingMillis;
                //float checksPerPass = (float)(nodePreContractChecks-recentPreContractChecks) / (float)(nodePreContractChecksPassed-recentPreContractChecksPassed);
                System.out.println(runTimeSoFar+"," + contractionOrder.size());
                //        "," + (now-recentTime) + "," + orderingTimeThisRun + 
                //        "," + checksPerPass);
                recentTime=now;
                recentPreContractChecks=nodePreContractChecks;
                recentPreContractChecksPassed=nodePreContractChecksPassed;
                recentOrderingMillis=millisSpentOnContractionOrdering;
            }
        }
        
        for (Node sortNode : allNodes.values()) {
            sortNode.sortNeighborLists();
        }
       
        System.out.println("findShortcutsCalls: "+findShortcutsCalls);
        System.out.println("nodePreContractChecks: "+nodePreContractChecks);
        System.out.println("nodePreContractChecksPassed: "+nodePreContractChecksPassed);
        System.out.println("millisSpentOnContractionOrdering: " + millisSpentOnContractionOrdering);
        
        es.shutdown();
    }

    private Node lazyContractNextNode(int contractionProgress, boolean includeUnprofitable) {
        Map.Entry<ContractionOrdering,Node> next = contractionOrder.pollFirstEntry();
        boolean recentlySorted = false;
        while (next != null) {
            ContractionOrdering oldOrder = next.getKey();
            Node n = next.getValue();
            
            ArrayList<DirectedEdge> shortcuts = findShortcuts(n);
            nodePreContractChecks++;
            
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
                    nodePreContractChecksPassed++;
                    return n;
                } else {
                    // Otherwise
                    contractionOrder.put(newOrder, n);
                }
                
            } else if (!recentlySorted) {
                contractionOrder.put(oldOrder, n);
                System.out.println("Reinitialising contraction order...");
                initialiseContractionOrder();
                recentlySorted = true;
            } else if (!includeUnprofitable) {
                System.out.println("Contraction became unprofitable with " + contractionOrder.size() + " nodes remaining.");
                return null;
            }
            next = contractionOrder.pollFirstEntry();
            if (recentlySorted && n.equals(next.getValue())) {
                System.out.println("Same thing's at the top of the contraction order.");
            }
        }
        return null;
    }
    
    private void reorderImmediateNeighbors(Node justContracted) {
        for (Node neighbor : justContracted.getNeighbors()) {
            neighbor.sortNeighborLists();
        }
        
        for (Node neighbor : justContracted.getNeighbors()) {
            reorderNodeIfNeeded(neighbor);
        }
    }
    
    private boolean reorderNodeIfNeeded(Node n) {
        ContractionOrdering oldOrder = contractionOrder.keyForValue(n);
        if (oldOrder == null) { // Already contracted (IIRC)
            return false;
        }
        
        ArrayList<DirectedEdge> shortcuts = findShortcuts(n);
        int balanceOfEdgesRemoved = getEdgeRemovedCount(n)-shortcuts.size();
        ContractionOrdering newOrder = new ContractionOrdering(n,balanceOfEdgesRemoved);
        
        if (oldOrder.compareTo(newOrder) != 0) {
            contractionOrder.remove(oldOrder);
            contractionOrder.put(newOrder, n);
            return true;
        } else {
            return false;
        }
    }

    public static int getMaxContractionDepth(List<DirectedEdge> de) {
        int maxContractionDepth = 0;
        for (DirectedEdge o : de) {
            maxContractionDepth = Math.max(maxContractionDepth, o.contractionDepth);
        }
        return maxContractionDepth;
    }
    
    class ContractionOrdering implements Comparable<ContractionOrdering> {
        final int edgeCountReduction;
        final int contractionDepth;
        final long idHash; // If everything else is equal we don't care about the order - but it's useful for it to be stable between runs.

        public ContractionOrdering(Node n, int edgeCountReduction) {
            this.edgeCountReduction = edgeCountReduction;
            contractionDepth = Math.max(getMaxContractionDepth(n.edgesFrom), getMaxContractionDepth(n.edgesTo));
            idHash = hashNodeId(n.nodeId);
        }
        
        private long hashNodeId(long nodeId) {
            // If nodes happen to go 1,2,3,4,5,6... we'd rather not contract
            // them in that order.
            return 6364136223846793005L * nodeId + 1442695040888963407L;
        }

        public int compareTo(ContractionOrdering o) {
            if (o == null) {
                return -1;
            } else if (this.edgeCountReduction>=0 && o.edgeCountReduction<0) {
                return -1;
            } else if (this.edgeCountReduction<0 && o.edgeCountReduction>=0) {
                return 1;
            } else if (this.contractionDepth != o.contractionDepth) {
                return Integer.compare(this.contractionDepth,o.contractionDepth);
            } else if (this.edgeCountReduction != o.edgeCountReduction) {
                return Integer.compare(o.edgeCountReduction,this.edgeCountReduction);
            } else if (this.idHash != o.idHash) {
                return Long.compare(this.idHash,o.idHash);
            } else {
                return 0;
            }
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 17 * hash + this.edgeCountReduction;
            hash = 17 * hash + this.contractionDepth;
            hash = 17 * hash + (int)(this.idHash ^ (this.idHash >>> 32));;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final ContractionOrdering other = (ContractionOrdering) obj;
            return (this.edgeCountReduction == other.edgeCountReduction
                    && this.contractionDepth == other.contractionDepth
                    && this.idHash == other.idHash);
        }

        @Override
        public String toString() {
            return "(edge reduction=" + edgeCountReduction + ", depth=" + contractionDepth + ", idHash=" + idHash + ')';
        }
    }
    
    private class KeyValue {
        final ContractionOrdering key;
        final Node value;

        public KeyValue(ContractionOrdering key, Node value) {
            this.key = key;
            this.value = value;
        }
    }
    
}
