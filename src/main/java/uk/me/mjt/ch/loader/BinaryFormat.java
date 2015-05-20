package uk.me.mjt.ch.loader;

import java.io.*;
import java.util.*;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.Preconditions;

public class BinaryFormat {
    
    public HashMap<Long,Node> read(String nodeFile, String wayFile) throws IOException {
        FileInputStream nodesIn = new FileInputStream(nodeFile);
        FileInputStream waysIn = new FileInputStream(wayFile);
        
        HashMap<Long, Node> result = readNodes(new DataInputStream(new BufferedInputStream(nodesIn)));
        loadEdgesGivenNodes(result,new DataInputStream(new BufferedInputStream(waysIn)));
        
        return result;
    }
    
    public void writeWays(Collection<Node> toWrite, String nodeFile, String wayFile) throws IOException {
        DataOutputStream waysOut = outStream(wayFile);
        writeEdges(toWrite,waysOut);
        waysOut.close();
        
        DataOutputStream nodesOut = outStream(nodeFile);
        writeNodesWithoutEdges(toWrite,nodesOut);
        nodesOut.close();
    }
    
    private static DataOutputStream outStream(String filename) throws FileNotFoundException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    }
    
    /*public void writeData(HashMap<Long,Node> toWrite, OutputStream destination) throws IOException {
        writeData(toWrite.values(),destination);
    }
    
    public void writeData(Collection<Node> toWrite, OutputStream destination) throws IOException {
        DataOutputStream dos = new DataOutputStream(destination);
        
        writeNodesWithoutEdges(toWrite, dos);
        writeEdges(toWrite, dos);
    }*/
    
    public HashMap<Long,Node> readNodes(DataInputStream source) throws IOException {
        HashMap<Long,Node> nodesById = new HashMap(1000);
        
        try {
            
            while(true) {
                long nodeId = source.readLong();
                long contrctionOrder = source.readLong();
                boolean isBorderNode = source.readBoolean();
                double lat = source.readDouble();
                double lon = source.readDouble();
                
                Node n = new Node(nodeId,(float)lat,(float)lon);
                n.contractionAllowed = !isBorderNode;
                n.contractionOrder=contrctionOrder;
                
                nodesById.put(nodeId, n);
            }
            
        } catch (EOFException e) { }
        
        return nodesById;
    }
    
    public void loadEdgesGivenNodes(HashMap<Long,Node> nodesById, DataInputStream source) throws IOException {
        HashMap<Long,DirectedEdge> edgesById = new HashMap(1000);
        
        try {
            
            while(true) {
                long edgeId = source.readLong();
                long fromNodeId = source.readLong();
                long toNodeId = source.readLong();
                int driveTimeMs = source.readInt();
                boolean isShortcut = source.readBoolean();
                long firstEdgeId = source.readLong();
                long secondEdgeId = source.readLong();
                
                Node fromNode = nodesById.get(fromNodeId);
                Node toNode = nodesById.get(toNodeId);
                if (fromNode==null || toNode==null) {
                    String problem = "Tried to load nodes " + fromNodeId + 
                            " and " + toNodeId + " for edge " + edgeId + 
                            " but got " + fromNode + " and " + toNode;
                    throw new RuntimeException(problem);
                }
                Preconditions.checkNoneNull(fromNode,toNode);
                
                DirectedEdge de;
                if (isShortcut) {
                    DirectedEdge firstEdge = edgesById.get(firstEdgeId);
                    DirectedEdge secondEdge = edgesById.get(secondEdgeId);
                    Preconditions.checkNoneNull(firstEdge,secondEdge);
                    de = new DirectedEdge(edgeId, fromNode, toNode, driveTimeMs, firstEdge, secondEdge);
                } else {
                    de = new DirectedEdge(edgeId, fromNode, toNode, driveTimeMs);
                }
                
                fromNode.edgesFrom.add(de);
                toNode.edgesTo.add(de);
                edgesById.put(edgeId, de);
            }
            
        } catch (EOFException e) { }
        
    }
    
    
    public void writeNodesWithoutEdges(Collection<Node> toWrite, DataOutputStream dest) throws IOException {
        
        for (Node n : toWrite) {
            dest.writeLong(n.nodeId);
            dest.writeLong(n.contractionOrder);
            dest.writeBoolean(!n.contractionAllowed);
            dest.writeDouble(n.lat);
            dest.writeDouble(n.lon);
        }
    }
    
    public void writeEdges(Collection<Node> toWrite, DataOutputStream dos) throws IOException {
        
        //dos.writeLong(calculateTotalEdgeCount(toWrite));
        
        HashSet<Long> writtenEdges = new HashSet();
        for (Node n : toWrite) {
            for (DirectedEdge de : n.edgesFrom) {
                writeEdgeRecursively(de, writtenEdges, dos);
            }
        }
    }
    
    /*private long calculateTotalEdgeCount(Collection<Node> toWrite) {
        long totalEdgeCount = 0;
        for (Node n : toWrite) {
            totalEdgeCount += n.edgesFrom.size();
        }
        return totalEdgeCount;
    }*/
        
    private void writeEdgeRecursively(DirectedEdge de, HashSet<Long> alreadyWritten, DataOutputStream dos) throws IOException {
        if (de==null || alreadyWritten.contains(de.edgeId)) {
            return;
        }
        
        writeEdgeRecursively(de.first,alreadyWritten,dos);
        writeEdgeRecursively(de.second,alreadyWritten,dos);
        
        dos.writeLong(de.edgeId);
        dos.writeLong(de.from.nodeId);
        dos.writeLong(de.to.nodeId);
        dos.writeInt(de.driveTimeMs);
        dos.writeBoolean(de.isShortcut());
        if (de.isShortcut()) {
            dos.writeLong(de.first.edgeId);
            dos.writeLong(de.second.edgeId);
        } else {
            dos.writeLong(0);
            dos.writeLong(0);
        }
        
        alreadyWritten.add(de.edgeId);
    }

}
