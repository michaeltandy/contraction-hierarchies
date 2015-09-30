
package uk.me.mjt.ch;

import java.util.AbstractList;
import java.util.List;


public class NodeListFromEdgeList extends AbstractList<Node> {
    private final List<DirectedEdge> de;

    public NodeListFromEdgeList(List<DirectedEdge> de) {
        Preconditions.checkNoneNull(de);
        Preconditions.require(de.size()>0);
        
        if (de.size() > 1) {
            if (de.get(0).to != de.get(1).from) {
                throw new RuntimeException("This class only supports forward edge lists at the moment, sorry.");
            }
        }
        
        this.de = de;
    }
    
    @Override
    public Node get(int index) {
        if (index==0) {
            return de.get(0).from;
        } else {
            return de.get(index-1).to;
        }
    }

    @Override
    public int size() {
        return de.size()+1;
    }

}
