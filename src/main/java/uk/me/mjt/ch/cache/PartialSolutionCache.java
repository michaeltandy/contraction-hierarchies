package uk.me.mjt.ch.cache;

import uk.me.mjt.ch.Node;

public interface PartialSolutionCache {
    
    public void put(Node key, UpAndDownPair upDownPair);
    public UpAndDownPair getIfPresent(Node key);
    
}
