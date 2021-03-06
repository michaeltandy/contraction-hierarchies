package uk.me.mjt.ch.cache;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.PartialSolution.DownwardSolution;
import uk.me.mjt.ch.PartialSolution.UpwardSolution;


public class BinaryCache implements PartialSolutionCache {
    private static final int MAXIMUM_CAPACITY = 8000;
    private static final float LOAD_FACTOR = 0.75f;
    private static final boolean EXPIRE_ACCESS_ORDER = true;
    
    private final Map<Node,ByteBuffer> underlyingCache = Collections.synchronizedMap(new FixedCapacityMap());
    
    @Override
    public void put(Node key, UpAndDownPair upDownPair) {
        underlyingCache.put(key, serialize(upDownPair));
    }

    @Override
    public UpAndDownPair getIfPresent(Node key) {
        return deserialize(underlyingCache.get(key));
    }
    
    private class FixedCapacityMap extends LinkedHashMap<Node,ByteBuffer> {
        public FixedCapacityMap() {
            super(MAXIMUM_CAPACITY,LOAD_FACTOR,EXPIRE_ACCESS_ORDER);
        }
        
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAXIMUM_CAPACITY;
        }
    }
    
    ByteBuffer serialize(UpAndDownPair upDown) {
        ByteBuffer a = upDown.up.getUnderlyingBuffer();
        ByteBuffer b = upDown.down.getUnderlyingBuffer();
        ByteBuffer joined = ByteBuffer.allocateDirect(a.capacity()+b.capacity());
        joined.order(ByteOrder.LITTLE_ENDIAN);
        joined.put(a).put(b);
        return joined;
    }
    
    UpAndDownPair deserialize(ByteBuffer bb) {
        if (bb==null)
            return null;
        
        int firstRecordCount = bb.getInt(0);
        int firstLength = 24*firstRecordCount + 4;
        
        bb.position(0); // REVISIT not thread safe?
        bb.limit(firstLength);
        ByteBuffer a = bb.slice();
        a.order(ByteOrder.LITTLE_ENDIAN);
        
        bb.limit(bb.capacity());
        bb.position(firstLength);
        ByteBuffer b = bb.slice();
        b.order(ByteOrder.LITTLE_ENDIAN);
        
        return new UpAndDownPair(new UpwardSolution(a), new DownwardSolution(b));
    }
    
}