package uk.me.mjt.ch.loader;

import java.io.*;
import java.util.*;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.Node;

public class BinaryFormat {
    
    
    public void writeData(HashMap<Long,Node> toWrite, OutputStream destination) throws IOException {
        writeData(toWrite.values(),destination);
    }
    
    public void writeData(Collection<Node> toWrite, OutputStream destination) throws IOException {
        DataOutputStream dos = new DataOutputStream(destination);
        
        writeNodesWithoutEdges(toWrite, dos);
        writeEdges(toWrite, dos);
    }
    
    private void writeNodesWithoutEdges(Collection<Node> toWrite, DataOutputStream dos) throws IOException {
        dos.writeLong(toWrite.size());
        
        for (Node n : toWrite) {
            dos.writeLong(n.nodeId);
            dos.writeLong(n.contractionOrder);
        }
    }
    
    private void writeEdges(Collection<Node> toWrite, DataOutputStream dos) throws IOException {
        
        dos.writeLong(calculateTotalEdgeCount(toWrite));
        
        HashSet<Long> writtenEdges = new HashSet();
        for (Node n : toWrite) {
            for (DirectedEdge de : n.edgesFrom) {
                writeEdgeRecursively(de, writtenEdges, dos);
            }
        }
    }
    
    private long calculateTotalEdgeCount(Collection<Node> toWrite) {
        long totalEdgeCount = 0;
        for (Node n : toWrite) {
            totalEdgeCount += n.edgesFrom.size();
        }
        return totalEdgeCount;
    }
        
    private void writeEdgeRecursively(DirectedEdge de, HashSet<Long> alreadyWritten, DataOutputStream dos) throws IOException {
        if (de==null || alreadyWritten.contains(de.edgeId)) {
            return;
        }
        
        writeEdgeRecursively(de.first,alreadyWritten,dos);
        writeEdgeRecursively(de.second,alreadyWritten,dos);
        
        dos.writeLong(de.edgeId);
        dos.writeLong(de.from.nodeId);
        dos.writeLong(de.to.nodeId);
        dos.writeFloat(de.distance);
        dos.writeBoolean(de.isShortcut());
        if (de.isShortcut()) {
            dos.writeLong(0);
            dos.writeLong(0);
        } else {
            dos.writeLong(de.first.edgeId);
            dos.writeLong(de.second.edgeId);
        }
        
        alreadyWritten.add(de.edgeId);
    }

}
