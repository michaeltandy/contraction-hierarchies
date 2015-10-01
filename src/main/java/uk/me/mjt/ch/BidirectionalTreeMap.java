
package uk.me.mjt.ch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class BidirectionalTreeMap<K extends Comparable<K>,V> {
    private int mutationCounter = 0;
    private static final int VALIDATION_PERIOD_MUTATIONS = 0;
    
    private final TreeMap<K,V> forwardMap = new TreeMap<>();
    private final HashMap<V,K> reverseMap = new HashMap<>();
    
    public int size() {
        return forwardMap.size();
    }

    public void clear() {
        forwardMap.clear();
        reverseMap.clear();
        checkIfInteresting(null,null);
    }

    public void put(K key, V value) {
        if (key==null || value==null) {
            throw new IllegalArgumentException("Neither key nor value may be null;");
        }
        
        boolean forwardContains = forwardMap.containsKey(key);
        boolean reverseContains = reverseMap.containsKey(value);
        
        if (forwardContains || reverseContains) {
            throw new IllegalArgumentException("One or both values are already in the map?"
                    + " K:" + key + " " + forwardContains 
                    + " V:" + value + " " + reverseContains);
        }
        
        forwardMap.put(key, value);
        reverseMap.put(value, key);
        
        if (!forwardMap.containsKey(key) || !reverseMap.containsKey(value)) {
            throw new IllegalArgumentException("Add-retrieve failed?");
        }
        
        checkIfInteresting(key,value);
    }

    public boolean isEmpty() {
        return forwardMap.isEmpty();
    }

    public K lastKey() {
        return forwardMap.lastKey();
    }
    
    public Map.Entry<K,V> pollFirstEntry() {
        Map.Entry<K,V> result = forwardMap.pollFirstEntry();
        if (result != null) {
            reverseMap.remove(result.getValue());
            checkIfInteresting(result.getKey(),result.getValue());
        }
        return result;
    }

    /*public Map.Entry<K,V> pollLastEntry() {
        Map.Entry<K,V> result = forwardMap.pollLastEntry();
        if (result != null) {
            reverseMap.remove(result.getValue());
        }
        if (validateEverything) validateMaps();
        return result;
    }*/
    
    public K keyForValue(V value) {
        return reverseMap.get(value);
    }
    
    public V remove(K key) {
        if (!forwardMap.containsKey(key)) {
            throw new IllegalArgumentException("Attempt to remove absent item?");
        }
        
        V value = forwardMap.remove(key);
        reverseMap.remove(value);
        
        checkIfInteresting(key,value);
        return value;
    }
    
    public String printFirst(int count) {
        return entriesToString(forwardMap, count);
    }
    
    public String printLast(int count) {
        return entriesToString(forwardMap.descendingMap(), count);
    }
    
    private String entriesToString(NavigableMap<K,V> entries, int count) {
        StringBuilder sb = new StringBuilder();
        Iterator<Entry<K,V>> iter = entries.entrySet().iterator();
        while (iter.hasNext() && count-- > 0) {
            Entry<K,V> entry = iter.next();
            sb.append(entry.getKey()).append("->").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
    
    private void checkIfInteresting(K key, V value) {
        mutationCounter++;
        if ( VALIDATION_PERIOD_MUTATIONS > 0 && mutationCounter%VALIDATION_PERIOD_MUTATIONS==0 ) {
            validateMaps();
        }
    }
    
    private void validateMaps() {
        if (forwardMap.size() != reverseMap.size()) {
            throw new IllegalStateException("Maps somehow became non-equal? "
                    + "Sizes don't match. Mutation Counter:" + mutationCounter);
        }
        
        for (Map.Entry<K,V> e : forwardMap.entrySet()) {
            K k1 = e.getKey();
            V v1 = k1==null?null:forwardMap.get(k1);
            K k2 = v1==null?null:reverseMap.get(v1);
            V v2 = k2==null?null:forwardMap.get(k2);
            
            if (k1==null || k2==null || v1==null || v2==null) {
                //System.out.println(forwardMap.getPathToKey(k1));
                //System.out.println(forwardMap.getPathFromEntry(e));
                throw new IllegalStateException("Maps somehow became non-equal?"
                        + " Mutation Counter:" + mutationCounter);
            }
            
            if (!k1.equals(k2) || !v1.equals(v2)) {
                throw new IllegalStateException("Maps somehow became non-equal?"
                        + " Mutation Counter:" + mutationCounter);
            }
        }
        
        
    }
    
    public String toString() {
        return forwardMap.toString();
    }
    
}
