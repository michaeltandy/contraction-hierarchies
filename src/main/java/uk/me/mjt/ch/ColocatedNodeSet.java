
package uk.me.mjt.ch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class ColocatedNodeSet implements Set<Node> {
    private Set<Node> underlying;
    
    public ColocatedNodeSet(Collection<Node> nodes) {
        underlying = Collections.unmodifiableSet(new HashSet<Node>(nodes));
        checkNodesAreColocated(underlying);
    }
    
    private void checkNodesAreColocated(Collection<Node> nodes) {
        Preconditions.require(!nodes.isEmpty());
        Node referenceLocation = null;
        for (Node n : nodes) {
            if (referenceLocation==null)
                referenceLocation=n;
            else
                Preconditions.require(
                        n.lat==referenceLocation.lat,
                        n.sourceDataNodeId==referenceLocation.sourceDataNodeId,
                        n.lon==referenceLocation.lon);
        }
    }
    
    public static ColocatedNodeSet singleton(Node node) {
        return new ColocatedNodeSet(Collections.singleton(node));
    }

    public int size() {
        return underlying.size();
    }

    public boolean isEmpty() {
        return underlying.isEmpty();
    }

    public boolean contains(Object o) {
        return underlying.contains(o);
    }

    public Iterator<Node> iterator() {
        return underlying.iterator();
    }

    public Object[] toArray() {
        return underlying.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return underlying.toArray(a);
    }

    public boolean add(Node e) {
        return underlying.add(e);
    }

    public boolean remove(Object o) {
        return underlying.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
        return underlying.containsAll(c);
    }

    public boolean addAll(Collection<? extends Node> c) {
        return underlying.addAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return underlying.retainAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return underlying.removeAll(c);
    }

    public void clear() {
        underlying.clear();
    }

    public boolean equals(Object o) {
        return underlying.equals(o);
    }

    public int hashCode() {
        return underlying.hashCode();
    }
    
    public String toString() {
        if (underlying.size()<20)
            return "ColocatedNodeSet:"+underlying.toString();
        else
            return "ColocatedNodeSet:"+underlying.size()+" elements";
    }

}
