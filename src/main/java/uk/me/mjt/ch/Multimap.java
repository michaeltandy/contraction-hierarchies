
package uk.me.mjt.ch;

import java.util.*;

public class Multimap<K,V> {
    
    private final HashMap<K,List<V>> underlyingMap = new HashMap<>();
    
    public void add(K key, V value) {
        getOrAddList(key, underlyingMap).add(value);
    }
    
    public List<V> get(K key) {
        List<V> result = underlyingMap.get(key);
        if (result == null) 
            result = Collections.emptyList();
        return Collections.unmodifiableList(result);
    }
    
    public boolean containsKey(K key) {
        return !get(key).isEmpty();
    }
    
    public Set<K> keySet() {
        return Collections.unmodifiableSet(underlyingMap.keySet());
    }
    
    private static <A,B> List<B> getOrAddList(A key, Map<A,List<B>> map) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<B>());
        }
        return map.get(key);
    }

}
