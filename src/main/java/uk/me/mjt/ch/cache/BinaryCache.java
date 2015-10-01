
package uk.me.mjt.ch.cache;

import java.util.*;
import uk.me.mjt.ch.Node;


public class BinaryCache implements PartialSolutionCache {
    private static final int MAXIMUM_CAPACITY = 8000;
    private static final float LOAD_FACTOR = 0.75f;
    private static final boolean EXPIRE_ACCESS_ORDER = true;
    private static final UpDownPairSerializer serializer = new UpDownPairSerializer();
    
    private final Map<Node,byte[]> underlyingCache = Collections.synchronizedMap(new FixedCapacityMap());
    
    @Override
    public void put(Node key, UpAndDownPair upDownPair) {
        underlyingCache.put(key, serializer.serialize(upDownPair));
    }

    @Override
    public UpAndDownPair getIfPresent(Node key) {
        return serializer.deserialize(underlyingCache.get(key));
    }
    
    private class FixedCapacityMap extends LinkedHashMap<Node,byte[]> {
        public FixedCapacityMap() {
            super(MAXIMUM_CAPACITY,LOAD_FACTOR,EXPIRE_ACCESS_ORDER);
        }
        
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAXIMUM_CAPACITY;
        }
    }
    
}
