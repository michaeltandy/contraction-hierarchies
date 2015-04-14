package uk.me.mjt.ch;

import uk.me.mjt.ch.Dijkstra.Direction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author michael
 */
public class GraphContractor {
    private final HashMap<Long,Node> allNodes;
    private long maxEdgeId;
    int contractionProgress = 1;
    
    private long findShortcutsCalls = 0;
    private long nodePreContractChecks = 0;
    private long nodePreContractChecksPassed = 0;
    private long millisSpentOnContractionOrdering = 0;
    
    private ExecutorService es = Executors.newFixedThreadPool(8);
    
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
        findShortcutsCalls++;
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
        
        int lastLog = Integer.MAX_VALUE;
        long startTime = System.currentTimeMillis();
        long recentTime = System.currentTimeMillis();
        long recentPreContractChecks = 0;
        long recentPreContractChecksPassed = 0;
        long recentOrderingMillis = 0;

        Node n;
        while (contractSomeNodes(false,false)>=0) {
            //System.out.println("Contracted " + n);
            if (lastLog-contractionOrder.size() > 10000) {
                long now = System.currentTimeMillis();
                long runTimeSoFar = now-startTime;
                long orderingTimeThisRun = millisSpentOnContractionOrdering-recentOrderingMillis;
                float checksPerPass = (float)(nodePreContractChecks-recentPreContractChecks) / (float)(nodePreContractChecksPassed-recentPreContractChecksPassed);
                System.out.println(runTimeSoFar+"," + contractionOrder.size() + 
                        "," + (now-recentTime) + "," + orderingTimeThisRun + 
                        "," + checksPerPass + "," + recentBlockSize);
                recentTime=now;
                recentPreContractChecks=nodePreContractChecks;
                recentPreContractChecksPassed=nodePreContractChecksPassed;
                recentOrderingMillis=millisSpentOnContractionOrdering;
                lastLog = contractionOrder.size();
            }
        }
       
        System.out.println("findShortcutsCalls: "+findShortcutsCalls);
        System.out.println("nodePreContractChecks: "+nodePreContractChecks);
        System.out.println("nodePreContractChecksPassed: "+nodePreContractChecksPassed);
        System.out.println("millisSpentOnContractionOrdering: " + millisSpentOnContractionOrdering);
        
        es.shutdown();
    }
    
    
    boolean recentlySorted = false;
    float recentBlockSize = 0;
    
    private int contractSomeNodes(boolean recentlySorted, boolean contractUnprofitable) {
        int nodesContracted = 0;
        boolean anyNodeProfitable = false;
        HashSet<Node> nodesToShortcut = new HashSet();
        
        while (nodesToShortcut.size() < 20000) {
            Map.Entry<ContractionOrdering,Node> next = contractionOrder.pollLastEntry();
            
            if (touchesAnyNode(nodesToShortcut,next.getValue())) {
                contractionOrder.put(next.getKey(), next.getValue());
                break;
            } else {
                Node n = next.getValue();
                nodesToShortcut.add(n);
                nodePreContractChecks++;
            }
        }
        
        recentBlockSize = 0.99f*recentBlockSize + 0.01f*nodesToShortcut.size();
        
        Map<Node,ArrayList<DirectedEdge>> shortcutSet = findShortcutsForAll(nodesToShortcut);
        ContractionOrdering requeueThreshold = (contractionOrder.isEmpty()?null:contractionOrder.lastKey());
        
        for (Map.Entry<Node,ArrayList<DirectedEdge>> e : shortcutSet.entrySet()) {
            Node n = e.getKey();
            ArrayList<DirectedEdge> shortcuts = e.getValue();
            
            int balanceOfEdgesRemoved = getEdgeRemovedCount(n)-shortcuts.size();
            boolean veryUnprofitable = balanceOfEdgesRemoved < -30;
            ContractionOrdering newOrder = new ContractionOrdering(n,balanceOfEdgesRemoved);
            
            if (veryUnprofitable) {
                if (contractUnprofitable && 
                        (requeueThreshold==null || newOrder.compareTo(requeueThreshold) >= 0)) {
                    contractNode(n,contractionProgress++,shortcuts);
                    nodesContracted++;
                    nodePreContractChecksPassed++;
                } else {
                    contractionOrder.put(newOrder, n);
                }
            } else {
                anyNodeProfitable = true;
                if (requeueThreshold==null || newOrder.compareTo(requeueThreshold) >= 0) {
                    contractNode(n,contractionProgress++,shortcuts);
                    nodesContracted++;
                    nodePreContractChecksPassed++;
                } else {
                    contractionOrder.put(newOrder, n);
                }
            }
        }
        
        if (!anyNodeProfitable) {
            if (recentlySorted && !contractUnprofitable) {
                System.out.println("Contraction became unprofitable with " + contractionOrder.size() + " nodes remaining.");
                return -1;
            } else { // FIXME work out logic when not leaving the last nodes uncontracted.
                System.out.println("Reinitialising contraction order (A)...");
                initialiseContractionOrder();
                return contractSomeNodes(true, contractUnprofitable);
            }
        } /*else if (nodesContracted < nodesToShortcut.size()/10) {
            System.out.println("Reinitialising contraction order (B)...");
            initialiseContractionOrder();
        }*/
        
        return nodesContracted;
    }
    
    
    
    private boolean touchesAnyNode(HashSet<Node> a, Node b) {
        if (a.contains(b))
            return true;
        for (DirectedEdge de : b.edgesFrom) {
            if (a.contains(de.to))
                return true;
        }
        for (DirectedEdge de : b.edgesTo) {
            if (a.contains(de.from))
                return true;
        }
        return false;
    }
    
    private Map<Node,ArrayList<DirectedEdge>> findShortcutsForAll(Collection<Node> nodes) {
        ArrayList<Node> nodeList = new ArrayList(nodes);
        ArrayList<Callable<ArrayList<DirectedEdge>>> callables = new ArrayList();
        for (final Node n : nodeList) {
            callables.add(new Callable<ArrayList<DirectedEdge>>() {
                public ArrayList<DirectedEdge> call() throws Exception {
                    return findShortcuts(n);
                }
            });
        }
        
        Map<Node,ArrayList<DirectedEdge>> shortcuts = new HashMap();
        
        try {
            List<Future<ArrayList<DirectedEdge>>> results = es.invokeAll(callables);
            
            for (int i=0 ; i<nodeList.size() ; i++) {
                Node n = nodeList.get(i);
                ArrayList<DirectedEdge> nodeShortcuts = results.get(i).get();
                shortcuts.put(n, nodeShortcuts);
            }
            
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        
        return shortcuts;
    }

    /*private Node lazyContractNextNode(int contractionProgress, boolean includeUnprofitable) {
        Map.Entry<ContractionOrdering,Node> next = contractionOrder.pollLastEntry();
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
            next = contractionOrder.pollLastEntry();
        }
        return null;
    }*/

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
    
    private class KeyValue {
        final ContractionOrdering key;
        final Node value;

        public KeyValue(ContractionOrdering key, Node value) {
            this.key = key;
            this.value = value;
        }
    }

}
