
package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


public class Util {
    
    public static boolean deepEquals(HashMap<Long,Node> a, HashMap<Long,Node> b, final boolean printDiff) {
        if (!a.keySet().equals(b.keySet())) {
            if (printDiff) System.out.println("Map keysets are different?!");
            return false;
        }
        
        for (Long key : a.keySet()) {
            if (!deepEquals(a.get(key), b.get(key), printDiff)) {
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean deepEquals(Node a, Node b, boolean printDiff) {
        if (!shallowEquals(a,b)) {
            if (printDiff) System.out.println("Shallow equals of " + a + " and " + b + " reports they're unequal.");
            if (printDiff) printShallowDiff(a,b);
            return false;
        } else {
            a.sortNeighborLists();
            b.sortNeighborLists();
            
            return compareEdgeArray(a.edgesFrom,b.edgesFrom,printDiff)
                    && compareEdgeArray(a.edgesTo,b.edgesTo,printDiff);
        }
    }
    
    private static boolean shallowEquals(Node a, Node b) {
        return ( a!=null && b!=null
                && a.nodeId == b.nodeId
                && a.lat == b.lat
                && a.lon == b.lon
                && a.contractionAllowed == b.contractionAllowed
                && a.contractionOrder == b.contractionOrder
                && a.edgesFrom.size() == b.edgesFrom.size()
                && a.edgesTo.size() == b.edgesTo.size());
    }
    
    private static void printShallowDiff(Node a, Node b) {
        System.out.println("Null check: " + (a!=null) +","+(b!=null));
        System.out.println("Node IDs: " + a.nodeId +","+b.nodeId);
        System.out.println("Lats: " + a.lat +","+ b.lat);
        System.out.println("Lons: " + a.lon +","+ b.lon);
        System.out.println("Contraction Allowed: " + a.contractionAllowed +","+ b.contractionAllowed);
        System.out.println("Edges from: " + a.edgesFrom.size() +","+ b.edgesFrom.size());
        System.out.println("Edges to: " + a.edgesTo.size() +","+ b.edgesTo.size());
        
        System.out.println("Edges from a:" + a.edgesFrom);
        System.out.println("Edges from b:" + b.edgesFrom);
        System.out.println("Edges to a:" + a.edgesTo);
        System.out.println("Edges to b:" + b.edgesTo);
    }
    
    private static boolean compareEdgeArray(ArrayList<DirectedEdge> a, ArrayList<DirectedEdge> b, final boolean printDiff) {
        for (int i=0 ; i<a.size() ; i++) {
            DirectedEdge deA = a.get(i);
            DirectedEdge deB = b.get(i);
            if (!edgesEqual(deA,deB)) {
                if (printDiff) System.out.println("While comparing edge arrays, " + deA + " and " + deB + " are unequal.");
                return false;
            }
        }
        return true;
    }
    
    private static boolean edgesEqual(DirectedEdge a, DirectedEdge b) {
        return (a.edgeId == b.edgeId
                && a.contractionDepth == b.contractionDepth
                && a.driveTimeMs == b.driveTimeMs
                && shallowEquals(a.from,b.from)
                && shallowEquals(a.to,b.to));
    }
    
    public static void sortEdgeArrays(ArrayList<DirectedEdge> toSort) {
        Comparator c = new Comparator<DirectedEdge>() {
            @Override
            public int compare(DirectedEdge o1, DirectedEdge o2) {
                return Long.compare(o1.edgeId, o2.edgeId);
            }
        };
        Collections.sort(toSort, c);
    }

}
