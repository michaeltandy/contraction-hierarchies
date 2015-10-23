package uk.me.mjt.ch;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import uk.me.mjt.ch.cache.BinaryCache;
import uk.me.mjt.ch.cache.CachedContractedDijkstra;
import uk.me.mjt.ch.cache.SimpleCache;
import uk.me.mjt.ch.loader.BinaryFormat;

public class BenchmarkUk {
    
    MapData allNodes;
    Node hatfield;
    
    public void loadAndCheckMapData() throws IOException {
        long startTime = System.currentTimeMillis();
        
        boolean loadedOk = attemptToLoad("great-britain-new-contracted-nodes.dat","great-britain-new-contracted-ways.dat");
        if (!loadedOk) {
            loadedOk = attemptToLoad("/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-ways.dat");
        }
        if (!loadedOk) {
            System.out.println("Couldn't find the map data to test with?");
            System.exit(3);
        }
        
        System.out.println("Data load complete in " + (System.currentTimeMillis() - startTime) + "ms.");
        CheckOsmRouting.checkContracted(allNodes);
    }
    
    public boolean attemptToLoad(String nodeFile, String wayFile) throws IOException {
        BinaryFormat bf = new BinaryFormat();
        
        if (new File(nodeFile).exists() && new File(wayFile).exists()) {
            System.out.println("Loading data from " + nodeFile + " and " + wayFile);
            allNodes=bf.read(nodeFile,wayFile);
            hatfield = allNodes.getNodeById(253199386L);
            return true;
        } else {
            return false;
        }
    }
    
    public void benchmarkPathing(int repetitions) {
        System.out.println("Benchmarking pathing. Warming up...");
        List<Node> testLocations = allNodes.chooseRandomNodes(4000);
        
        for (Node node : testLocations) {
            ContractedDijkstra.contractedGraphDijkstra(allNodes, hatfield, node);
            ContractedDijkstra.contractedGraphDijkstra(allNodes, node, hatfield);
        }
        
        System.out.println("Warming up complete, benchmarking...");
        long startTime = System.currentTimeMillis();
        for (int i=0 ; i<repetitions ; i++) {
            System.out.println("Iteration " + i);
            for (Node node : testLocations) {
                ContractedDijkstra.contractedGraphDijkstra(allNodes, hatfield, node);
                ContractedDijkstra.contractedGraphDijkstra(allNodes, node, hatfield);
            }
        }
        
        System.out.println(repetitions+" repetitions uncached pathing from hatfield to " +testLocations.size()+ " locations in "+ (System.currentTimeMillis() - startTime) + " ms.");
    }
    
    public void benchmarkParallelPathing(int repetitions) {
        System.out.println("Benchmarking parallel uncached pathing. Warming up...");
        List<Node> testLocations = allNodes.chooseRandomNodes(4000);
        ExecutorService es = Executors.newFixedThreadPool(2);
        
        for (Node node : testLocations) {
            ContractedDijkstra.contractedGraphDijkstra(allNodes, hatfield, node, es);
            ContractedDijkstra.contractedGraphDijkstra(allNodes, node, hatfield, es);
        }
        
        System.out.println("Warming up complete, benchmarking...");
        long startTime = System.currentTimeMillis();
        for (int i=0 ; i<repetitions ; i++) {
            System.out.println("Iteration " + i);
            for (Node node : testLocations) {
                ContractedDijkstra.contractedGraphDijkstra(allNodes, hatfield, node, es);
                ContractedDijkstra.contractedGraphDijkstra(allNodes, node, hatfield, es);
            }
        }
        
        System.out.println(repetitions+" repetitions parallel uncached pathing from hatfield to " +testLocations.size()+ " locations in "+ (System.currentTimeMillis() - startTime) + " ms.");
        es.shutdown();
    }
    
    public void benchmarkCachedPathing(int repetitions) {
        System.out.println("Benchmarking cached pathing. Warming up & populating cache...");
        List<Node> testLocations = allNodes.chooseRandomNodes(4000);
        BinaryCache cache = populateTestCache(testLocations);
        
        System.out.println("Warming up complete, benchmarking...");
        long startTime = System.currentTimeMillis();
        for (int i=0 ; i<repetitions ; i++) {
            System.out.println("Iteration " + i);
            for (Node node : testLocations) {
                CachedContractedDijkstra.contractedGraphDijkstra(allNodes, hatfield, node, cache);
                CachedContractedDijkstra.contractedGraphDijkstra(allNodes, node, hatfield, cache);
            }
        }
        
        System.out.println(repetitions+" repetitions cached pathing from hatfield to " +testLocations.size()+ " locations in "+ (System.currentTimeMillis() - startTime) + " ms.");
    }

    private BinaryCache populateTestCache(List<Node> testLocations) {
        BinaryCache cache = new BinaryCache();
        for (Node node : testLocations) {
            CachedContractedDijkstra.contractedGraphDijkstra(allNodes, hatfield, node, cache);
            CachedContractedDijkstra.contractedGraphDijkstra(allNodes, node, hatfield, cache);
        }
        return cache;
    }
    
    public void benchmarkParallelCachedPathing(int repetitions) throws Exception {
        System.out.println("Benchmarking parallel cached pathing. Warming up & populating cache...");
        List<Node> testLocations = allNodes.chooseRandomNodes(4000);
        BinaryCache cache = populateTestCache(testLocations);
        
        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println("Reported processors: " + processors + " (hyperthreaded cores count as 2)");
        ExecutorService es = Executors.newFixedThreadPool(processors);
        
        System.out.println("Warming up complete, benchmarking...");
        long startTime = System.currentTimeMillis();
        for (int i=0 ; i<repetitions ; i++) {
            System.out.println("Iteration " + i);
            
            List<Future<DijkstraSolution>> solutions = new ArrayList(testLocations.size()*2);
            for (Node node : testLocations) {
                solutions.add(es.submit(CachedContractedDijkstra.callableContractedGraphDijkstra(allNodes, hatfield, node, cache)));
                solutions.add(es.submit(CachedContractedDijkstra.callableContractedGraphDijkstra(allNodes, node, hatfield, cache)));
            }
            for (Future<DijkstraSolution> solution : solutions) {
                DijkstraSolution thisDs = solution.get();
            }
        }
        
        System.out.println(repetitions+" repetitions parallel cached pathing from hatfield to " +testLocations.size()+ " locations in "+ (System.currentTimeMillis() - startTime) + " ms.");
        es.shutdown();
    }
    
    public static void main(String[] args) {
        try {
            BenchmarkUk instance = new BenchmarkUk();
            instance.loadAndCheckMapData();
            System.gc(); // Hopefully start the map data on its journey to oldgen :)
            
            if (args.length >= 3 && "cachedonly".equals(args[1])) {
                int iterations = Integer.parseInt(args[2]);
                
                instance.benchmarkCachedPathing(iterations);
                instance.benchmarkParallelCachedPathing(iterations);
                
            } else {
                instance.benchmarkPathing(2);
                instance.benchmarkParallelPathing(2);
                instance.benchmarkCachedPathing(100);
                instance.benchmarkParallelCachedPathing(100);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
}
