
package uk.me.mjt.ch;

import java.util.AbstractList;
import java.util.List;


public class UnionList<N> extends AbstractList<N> implements List<N> {
    private final List<N> first;
    private final List<N> second;
    private final int size;

    public UnionList(List<N> first, List<N> second) {
        Preconditions.checkNoneNull(first,second);
        this.first = first;
        this.second = second;
        this.size = first.size() + second.size();
    }
    
    @Override
    public N get(int index) {
        if (index < first.size()) {
            return first.get(index);
        } else {
            return second.get(index-first.size());
        }
    }

    @Override
    public int size() {
        return size;
    }

}
