
package uk.me.mjt.ch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class BidirectionalTreeMap<K extends Comparable<K>,V> {
    private final TreeMap<K,V> forwardMap = new TreeMap<>();
    private final HashMap<V,K> reverseMap = new HashMap<>();
    
    public int size() {
        return forwardMap.size();
    }

    public void clear() {
        forwardMap.clear();
        reverseMap.clear();
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
        }
        return result;
    }

    public K keyForValue(V value) {
        return reverseMap.get(value);
    }
    
    public V remove(K key) {
        if (!forwardMap.containsKey(key)) {
            throw new IllegalArgumentException("Attempt to remove absent item?");
        }
        
        V value = forwardMap.remove(key);
        reverseMap.remove(value);
        
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
    
    public String toString() {
        return forwardMap.toString();
    }
    
}
