
package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.HashMap;


public class CheckOsmRouting {
    private static final RoutingTestCase hatfieldToHerbal = new RoutingTestCase(253199386L,18670884L,2345154,   253199386L,247514L,195441L,868249730L, 18670884L);
    private static final RoutingTestCase tiberiusToLivia = new RoutingTestCase(1224756026L,1224756028L,33305,   1224756026L,1224757149L,1224756484L,1224756195L,1224757082L,1224757091L,1224756028L);
    private static final RoutingTestCase avoidMizenWay = new RoutingTestCase(1651312172L,13853101L,125124,      1651312172L,1651312118L,749962849L,13853101L);
    private static final RoutingTestCase expectedIntoMizenWay = new RoutingTestCase(1651312172L,200000001647192344L,42253, 1651312172L,200000000974087053L,200000001647192344L);
    private static final RoutingTestCase sunlightGardensAvoidGate = new RoutingTestCase(1581223475L,158758984L,155196,     1581223475L,158760624L,158760472L,158758984L);
    
    private static final ArrayList<RoutingTestCase> testCases = new ArrayList<RoutingTestCase>() {{
            add(hatfieldToHerbal); // Basic test case - check routing works.
            add(tiberiusToLivia); // Test case for oneway=false
            add(avoidMizenWay); // Test case for avoiding private roads
            add(expectedIntoMizenWay); // Test case for using private roads when required
            add(sunlightGardensAvoidGate); // Test case for not going through gates
        }};
    
    public static void checkUncontracted(MapData allNodes) {
        allNodes.validate();
        for (RoutingTestCase tc : testCases) {
            Node from = allNodes.getNodeById(tc.fromNode);
            Node to = allNodes.getNodeById(tc.toNode);
            
            DijkstraSolution computed = Dijkstra.dijkstrasAlgorithm(from, to, Dijkstra.Direction.FORWARDS);
            compare(tc, computed);
        }
    }
    
    public static void checkContracted(MapData allNodes) {
        allNodes.validate();
        for (RoutingTestCase tc : testCases) {
            Node from = allNodes.getNodeById(tc.fromNode);
            Node to = allNodes.getNodeById(tc.toNode);
            
            DijkstraSolution computed = ContractedDijkstra.contractedGraphDijkstra(allNodes, from, to);
            compare(tc, computed);
        }
    }
    
    private static void compare(RoutingTestCase testCase, DijkstraSolution solution) {
        if (solution == null || testCase.driveTimeMs != solution.totalDriveTimeMs || !testCase.waypointsMet(solution)) {
            String error = "OSM routing check failed for " + testCase;
            if (solution == null) {
                error += " solution was null?!";
            } else {
                if (testCase.driveTimeMs != solution.totalDriveTimeMs) {
                    error += " drive times didn't match - " + testCase.driveTimeMs + " vs " + solution.totalDriveTimeMs + " - and ";
                } else {
                    error += " drive times matched but";
                }
                if (!testCase.waypointsMet(solution)) {
                    error += " waypoints were not all met.";
                    error += " GeoJson:" + GeoJson.solution(solution);
                }
            }                        
            throw new RuntimeException(error);
        } else {
            System.out.println("Checked " + testCase + " OK");
        }
    }
    
    private static class RoutingTestCase {
        private final long fromNode;
        private final long toNode;
        private final int driveTimeMs;
        private final long[] waypoints;

        public RoutingTestCase(long fromNode, long toNode, int driveTimeMs, long... waypoints) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.driveTimeMs = driveTimeMs;
            this.waypoints = waypoints;
        }
        
        public String toString() {
            return fromNode + "->" + toNode;
        }
        
        public boolean waypointsMet(DijkstraSolution ds) {
            int lastIdx = -1;
            for (long toFind : waypoints) {
                int thisIdx = ds.nodes.indexOf(toFind);
                if (thisIdx<lastIdx)
                    return false;
                lastIdx = thisIdx;
            }
            return true;
        }
    }
    
}
