package uk.me.mjt.ch.loader;

import java.io.*;
import java.util.*;
import uk.me.mjt.ch.AccessOnly;
import uk.me.mjt.ch.Barrier;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.MapData;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.Preconditions;
import uk.me.mjt.ch.TurnRestriction;

public class BinaryFormat {
    private static final long MAX_FILE_VERSION_SUPPORTED = 5;
    private static final long MIN_FILE_VERSION_SUPPORTED = 5;
    private static final long FILE_VERSION_WRITTEN = MAX_FILE_VERSION_SUPPORTED;
    
    public MapData read(String nodeFile, String wayFile) throws IOException {
        try ( FileInputStream nodesIn = new FileInputStream(nodeFile);
              FileInputStream waysIn = new FileInputStream(wayFile)) {
            return read(nodesIn, waysIn, null);
        }
    }
    
    public MapData read(String nodeFile, String wayFile, String turnRestrictionFile) throws IOException {
        try ( FileInputStream nodesIn = new FileInputStream(nodeFile);
              FileInputStream waysIn = new FileInputStream(wayFile);
              FileInputStream restrictionsIn = new FileInputStream(turnRestrictionFile)) {
            return read(nodesIn, waysIn, restrictionsIn);
        }
    }
    
    public MapData read(InputStream nodesIn, InputStream waysIn, InputStream restrictionsIn) throws IOException {
        HashMap<Long,TurnRestriction> turnRestrictions = new HashMap<>();
        if (restrictionsIn != null)
            try (DataInputStream dis = inStream(restrictionsIn)) {
                turnRestrictions = readTurnRestrictions(dis);
            }
        
        HashMap<Long,Node> nodesById;
        try (DataInputStream dis = inStream(nodesIn)) {
            nodesById = readNodes(dis);
        }
        
        try (DataInputStream dis = inStream(waysIn)) {
            loadEdgesGivenNodes(nodesById,dis);
        }
        
        MapData md = new MapData(nodesById, turnRestrictions);
        md.validate();
        return md;
    }
    
    public void write(MapData toWrite, String nodeFile, String wayFile) throws IOException {
        write(toWrite, nodeFile, wayFile, null);
    }
    
    public void write(MapData toWrite, String nodeFile, String wayFile, String restrictionFile) throws IOException {
        try (DataOutputStream waysOut = outStream(wayFile)) {
            writeEdges(toWrite.getAllNodes(),waysOut);
        }
        
        try (DataOutputStream nodesOut = outStream(nodeFile)) {
            writeNodesWithoutEdges(toWrite.getAllNodes(),nodesOut);
        }
        
        if (restrictionFile != null) {
            try (DataOutputStream restrictionsOut = outStream(restrictionFile)) {
                writeTurnRestrictions(toWrite.allTurnRestrictions(), restrictionsOut);
            }
        }
    }
    
    private static DataInputStream inStream(InputStream inStream) throws FileNotFoundException {
        return new DataInputStream(new BufferedInputStream(inStream));
    }
    
    private static DataOutputStream outStream(String filename) throws FileNotFoundException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    }
    
    private HashMap<Long,Node> readNodes(DataInputStream source) throws IOException {
        HashMap<Long,Node> nodesById = new HashMap(1000);
        long fileFormatVersion = source.readLong();
        Preconditions.require(fileFormatVersion>=MIN_FILE_VERSION_SUPPORTED, fileFormatVersion<=MAX_FILE_VERSION_SUPPORTED);
        
        try {
            
            while(true) {
                long nodeId = source.readLong();
                long sourceDataNodeId = source.readLong();
                long contrctionOrder = source.readLong();
                int properties = source.readByte();
                boolean isBorderNode = (properties&0x01)!=0;
                boolean isBarrier = (properties&0x02)!=0;
                double lat = source.readDouble();
                double lon = source.readDouble();
                
                Node n = new Node(nodeId,sourceDataNodeId,(float)lat,(float)lon,(isBarrier?Barrier.TRUE:Barrier.FALSE));
                n.contractionAllowed = !isBorderNode;
                n.contractionOrder=contrctionOrder;
                
                nodesById.put(nodeId, n);
            }
            
        } catch (EOFException e) { }
        
        return nodesById;
    }
    
    private void loadEdgesGivenNodes(HashMap<Long,Node> nodesById, DataInputStream source) throws IOException {
        HashMap<Long,DirectedEdge> edgesById = new HashMap(1000);
        long fileFormatVersion = source.readLong();
        Preconditions.require(fileFormatVersion>=MIN_FILE_VERSION_SUPPORTED, fileFormatVersion<=MAX_FILE_VERSION_SUPPORTED);
        
        try {
            
            while(true) {
                long edgeId = source.readLong();
                long temp = source.readLong(); // REVISIT start using this
                long fromNodeId = source.readLong();
                long toNodeId = source.readLong();
                int driveTimeMs = source.readInt();
                byte properties = source.readByte();
                boolean isShortcut = (properties&0x01)==0x01;
                boolean isAccessOnly = (properties&0x02)==0x02;
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
                    de = new DirectedEdge(edgeId, fromNode, toNode, driveTimeMs, (isAccessOnly?AccessOnly.TRUE:AccessOnly.FALSE));
                }
                
                fromNode.edgesFrom.add(de);
                toNode.edgesTo.add(de);
                edgesById.put(edgeId, de);
            }
            
        } catch (EOFException e) { }
        
        Node.sortNeighborListsAll(nodesById.values());
    }
    
    
    public void writeNodesWithoutEdges(Collection<Node> toWrite, DataOutputStream dest) throws IOException {
        dest.writeLong(FILE_VERSION_WRITTEN);
        
        for (Node n : toWrite) {
            dest.writeLong(n.nodeId);
            dest.writeLong(n.sourceDataNodeId);
            dest.writeLong(n.contractionOrder);
            int properties = (!n.contractionAllowed?0x01:0x00) | (n.barrier==Barrier.TRUE?0x02:0x00);
            dest.writeByte(properties);
            dest.writeDouble(n.lat);
            dest.writeDouble(n.lon);
        }
    }
    
    public void writeEdges(Collection<Node> toWrite, DataOutputStream dos) throws IOException {
        dos.writeLong(FILE_VERSION_WRITTEN);
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
        dos.writeLong(de.edgeId); // REVISIT start using this
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
    
    /*private void writeTurnRestriction(long turnRestrictionId, String restrictionType, List<WrittenRoadSegment> turnRestriction) {
        try {
            turnRestrictionOutput.writeLong(turnRestrictionId);
            turnRestrictionOutput.writeInt(turnRestriction.size());
            turnRestrictionOutput.writeBoolean(restrictionType.startsWith("no_"));
            for (WrittenRoadSegment segment : turnRestriction) {
                turnRestrictionOutput.writeLong(segment.graphEdgeId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/
    
    private HashMap<Long,TurnRestriction> readTurnRestrictions(DataInputStream source) throws IOException {
        HashMap<Long,TurnRestriction> result = new HashMap();
        
        long fileFormatVersion = source.readLong();
        Preconditions.require(fileFormatVersion>=MIN_FILE_VERSION_SUPPORTED, fileFormatVersion<=MAX_FILE_VERSION_SUPPORTED);
        
        try {
            
            while (true) {
                long turnRestrictionId = source.readLong();
                boolean typeStartsWithNo = source.readBoolean();
                int entryCount = source.readInt();
                List<Long> edgeIds = new ArrayList(entryCount);
                
                for (int i=0 ; i<entryCount ; i++) {
                    edgeIds.add(source.readLong());
                }
                
                TurnRestriction.TurnRestrictionType trt = (typeStartsWithNo?TurnRestriction.TurnRestrictionType.NOT_ALLOWED:TurnRestriction.TurnRestrictionType.ONLY_ALLOWED);
                result.put(turnRestrictionId, new TurnRestriction(turnRestrictionId, trt, edgeIds));
            }
            
        } catch (EOFException e) { }
        
        return result;
    }
    
    public void writeTurnRestrictions(Collection<TurnRestriction> toWrite, DataOutputStream dos) throws IOException {
        dos.writeLong(FILE_VERSION_WRITTEN);
        
        for (TurnRestriction tr : toWrite) {
            
            dos.writeLong(tr.getTurnRestrictionId());
            dos.writeBoolean(tr.getType()==TurnRestriction.TurnRestrictionType.NOT_ALLOWED);
            dos.writeInt(tr.getDirectedEdgeIds().size());
            for (Long edgeId : tr.getDirectedEdgeIds()) {
                dos.writeLong(edgeId);
            }
            
        }
    }

}
