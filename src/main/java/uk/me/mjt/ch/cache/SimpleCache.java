
package uk.me.mjt.ch.cache;

import uk.me.mjt.ch.UpAndDownPair;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.me.mjt.ch.DijkstraSolution;
import uk.me.mjt.ch.Node;


public class SimpleCache implements PartialSolutionCache {
    private static final int MAXIMUM_CAPACITY = 8000;
    private static final float LOAD_FACTOR = 0.75f;
    private static final boolean EXPIRE_ACCESS_ORDER = true;
    
    private final Map<Node,UpAndDownPair> underlyingCache = Collections.synchronizedMap(new FixedCapacityMap());
    
    @Override
    public void put(Node key, UpAndDownPair partialSolution) {
        underlyingCache.put(key, partialSolution);
    }

    @Override
    public UpAndDownPair getIfPresent(Node key) {
        return underlyingCache.get(key);
    }
    
    private class FixedCapacityMap extends LinkedHashMap<Node,UpAndDownPair> {
        public FixedCapacityMap() {
            super(MAXIMUM_CAPACITY,LOAD_FACTOR,EXPIRE_ACCESS_ORDER);
        }
        
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAXIMUM_CAPACITY;
        }
    }

}
