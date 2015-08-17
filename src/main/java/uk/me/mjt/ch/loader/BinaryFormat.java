package uk.me.mjt.ch.loader;

import java.io.*;
import java.util.*;
import uk.me.mjt.ch.AccessOnly;
import uk.me.mjt.ch.Barrier;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.MapData;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.Preconditions;

public class BinaryFormat {
    
    public MapData read(String nodeFile, String wayFile) throws IOException {
        FileInputStream nodesIn = new FileInputStream(nodeFile);
        FileInputStream waysIn = new FileInputStream(wayFile);
        
        MapData result = readNodes(new DataInputStream(new BufferedInputStream(nodesIn)));
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
    
    public MapData readNodes(DataInputStream source) throws IOException {
        HashMap<Long,Node> nodesById = new HashMap(1000);
        
        try {
            
            while(true) {
                long nodeId = source.readLong();
                long contrctionOrder = source.readLong();
                int properties = source.readByte();
                boolean isBorderNode = (properties&0x01)!=0;
                boolean isBarrier = (properties&0x02)!=0;
                double lat = source.readDouble();
                double lon = source.readDouble();
                
                Node n = new Node(nodeId,(float)lat,(float)lon,(isBarrier?Barrier.TRUE:Barrier.FALSE));
                n.contractionAllowed = !isBorderNode;
                n.contractionOrder=contrctionOrder;
                
                nodesById.put(nodeId, n);
            }
            
        } catch (EOFException e) { }
        
        return new MapData(nodesById);
    }
    
    public void loadEdgesGivenNodes(MapData nodesById, DataInputStream source) throws IOException {
        HashMap<Long,DirectedEdge> edgesById = new HashMap(1000);
        
        try {
            
            while(true) {
                long edgeId = source.readLong();
                long fromNodeId = source.readLong();
                long toNodeId = source.readLong();
                int driveTimeMs = source.readInt();
                byte properties = source.readByte();
                boolean isShortcut = (properties&0x01)==0x01;
                boolean isAccessOnly = (properties&0x02)==0x02;
                long firstEdgeId = source.readLong();
                long secondEdgeId = source.readLong();
                
                Node fromNode = nodesById.getNodeById(fromNodeId);
                Node toNode = nodesById.getNodeById(toNodeId);
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
                    de = new DirectedEdge(edgeId, fromNode, toNode, driveTimeMs, (isAccessOnly?AccessOnly.TRUE:AccessOnly.FALSE));
                }
                
                fromNode.edgesFrom.add(de);
                toNode.edgesTo.add(de);
                edgesById.put(edgeId, de);
            }
            
        } catch (EOFException e) { }
        
        for (Node n : nodesById.getAllNodes()) {
            n.sortNeighborLists();
        }
    }
    
    
    public void writeNodesWithoutEdges(Collection<Node> toWrite, DataOutputStream dest) throws IOException {
        
        for (Node n : toWrite) {
            dest.writeLong(n.nodeId);
            dest.writeLong(n.contractionOrder);
            int properties = (!n.contractionAllowed?0x01:0x00) | (n.barrier==Barrier.TRUE?0x02:0x00);
            dest.writeByte(properties);
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
        
        if (de.edgeId==DirectedEdge.PLACEHOLDER_ID) {
            throw new RuntimeException("Attempted to write an edge that hasn't been assigned a unique ID.");
        }
        
        writeEdgeRecursively(de.first,alreadyWritten,dos);
        writeEdgeRecursively(de.second,alreadyWritten,dos);
        
        dos.writeLong(de.edgeId);
        dos.writeLong(de.from.nodeId);
        dos.writeLong(de.to.nodeId);
        dos.writeInt(de.driveTimeMs);
        
        int properties = (de.isShortcut()?0x01:0x00) | (de.accessOnly==AccessOnly.TRUE?0x02:0x00);
        dos.writeByte(properties);
        
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
