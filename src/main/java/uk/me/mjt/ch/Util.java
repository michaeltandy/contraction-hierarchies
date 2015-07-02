
package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


public class Util {
    
    public static boolean deepEquals(HashMap<Long,Node> a, HashMap<Long,Node> b) {
        if (!a.keySet().equals(b.keySet()))
            return false;
        
        for (Long key : a.keySet()) {
            if (!deepEquals(a.get(key), b.get(key))) {
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean deepEquals(Node a, Node b) {
        if (!shallowEquals(a,b)) {
            return false;
        } else {
            a.sortNeighborLists();
            b.sortNeighborLists();
            
            return compareEdgeArray(a.edgesFrom,b.edgesFrom)
                    && compareEdgeArray(a.edgesTo,b.edgesTo);
        }
    }
    
    private static boolean shallowEquals(Node a, Node b) {
        return ( a!=null && b!=null
                && a.nodeId == b.nodeId
                && a.lat == b.lat
                && a.lon == b.lon
                && a.contractionAllowed == b.contractionAllowed
                && a.nodeId == b.nodeId
                && a.contractionAllowed == b.contractionAllowed
                && a.edgesFrom.size() == b.edgesFrom.size()
                && a.edgesTo.size() == b.edgesTo.size());
    }
    
    private static boolean compareEdgeArray(ArrayList<DirectedEdge> a, ArrayList<DirectedEdge> b) {
        for (int i=0 ; i<a.size() ; i++) {
            DirectedEdge deA = a.get(i);
            DirectedEdge deB = b.get(i);
            if (!edgesEqual(deA,deB)) {
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
