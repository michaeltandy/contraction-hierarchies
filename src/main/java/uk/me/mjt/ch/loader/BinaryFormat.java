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
import uk.me.mjt.ch.status.MonitoredProcess;
import uk.me.mjt.ch.status.StatusMonitor;

public class BinaryFormat {
    private static final long MAX_FILE_VERSION_SUPPORTED = 6;
    private static final long MIN_FILE_VERSION_SUPPORTED = 5;
    private static final long FILE_VERSION_WRITTEN = MAX_FILE_VERSION_SUPPORTED;
    
    public MapData read(String nodeFile, String wayFile, StatusMonitor monitor) throws IOException {
        try ( FileInputStream nodesIn = new FileInputStream(nodeFile);
              FileInputStream waysIn = new FileInputStream(wayFile)) {
            return read(nodesIn, waysIn, null, monitor);
        }
    }
    
    public MapData read(String nodeFile, String wayFile, String turnRestrictionFile, StatusMonitor monitor) throws IOException {
        try ( FileInputStream nodesIn = new FileInputStream(nodeFile);
              FileInputStream waysIn = new FileInputStream(wayFile);
              FileInputStream restrictionsIn = (turnRestrictionFile==null?null:new FileInputStream(turnRestrictionFile))) {
            return read(nodesIn, waysIn, restrictionsIn, monitor);
        }
    }
    
    public MapData read(InputStream nodesIn, InputStream waysIn, InputStream restrictionsIn, StatusMonitor monitor) throws IOException {
        HashSet<TurnRestriction> turnRestrictions = new HashSet<>();
        if (restrictionsIn != null)
            try (DataInputStream dis = inStream(restrictionsIn)) {
                turnRestrictions = readTurnRestrictions(dis);
            }
        
        HashMap<Long,Node> nodesById;
        try (DataInputStream dis = inStream(nodesIn)) {
            nodesById = readNodes(dis, monitor);
        }
        
        try (DataInputStream dis = inStream(waysIn)) {
            loadEdgesGivenNodes(nodesById, dis, monitor);
        }
        
        MapData md = new MapData(nodesById, turnRestrictions, monitor);
        md.validate(monitor);
        return md;
    }
    
    public void write(MapData toWrite, String nodeFile, String wayFile) throws IOException {
        try (DataOutputStream waysOut = outStream(wayFile);
                DataOutputStream nodesOut = outStream(nodeFile);) {
            write(toWrite, nodesOut, waysOut, null);
        }
    }
    
    /*Not broken or anything - just currently unused.
    public void write(MapData toWrite, String nodeFile, String wayFile, String restrictionFile) throws IOException {
        try (DataOutputStream waysOut = outStream(wayFile);
                DataOutputStream nodesOut = outStream(nodeFile);
                DataOutputStream restrictionsOut = (restrictionFile==null?null:outStream(restrictionFile))) {
            write(toWrite, nodesOut, waysOut, restrictionsOut);
        }
    }*/
    
    public void write(MapData toWrite, DataOutputStream nodesOut, DataOutputStream waysOut, DataOutputStream restrictionsOut) throws IOException {
        writeEdges(toWrite.getAllNodes(),waysOut);
        writeNodesWithoutEdges(toWrite.getAllNodes(),nodesOut);
        if (restrictionsOut != null) {
            writeTurnRestrictions(toWrite.allTurnRestrictions(), restrictionsOut);
        }
    }
    
    private static DataInputStream inStream(InputStream inStream) throws FileNotFoundException {
        return new DataInputStream(new BufferedInputStream(inStream));
    }
    
    private static DataOutputStream outStream(String filename) throws FileNotFoundException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    }
    
    private HashMap<Long,Node> readNodes(DataInputStream source, StatusMonitor monitor) throws IOException {
        Preconditions.checkNoneNull(monitor);
        long fileFormatVersion = source.readLong();
        checkFileFormatVersion(fileFormatVersion);
        
        long totalNodeCount = (fileFormatVersion >= 6 ? source.readLong() : -1);
        long nodesLoadedSoFar = 0;
        HashMap<Long,Node> nodesById = new HashMap(Math.max(1000, (int)totalNodeCount));
        
        try {
            monitor.updateStatus(MonitoredProcess.LOAD_NODES, nodesLoadedSoFar, totalNodeCount);
            
            while(true) {
                long nodeId = source.readLong();
                long sourceDataNodeId = source.readLong();
                long contractionOrder = source.readLong();
                int properties = source.readByte();
                boolean isBorderNode = (properties&0x01)!=0;
                boolean isBarrier = (properties&0x02)!=0;
                double lat = source.readDouble();
                double lon = source.readDouble();
                
                Node n = new Node(nodeId,sourceDataNodeId,(float)lat,(float)lon,(isBarrier?Barrier.TRUE:Barrier.FALSE));
                n.contractionAllowed = !isBorderNode;
                if (contractionOrder==Long.MAX_VALUE)
                    contractionOrder=Node.UNCONTRACTED;
                n.contractionOrder=(int)contractionOrder;
                
                nodesById.put(nodeId, n);
                nodesLoadedSoFar++;
                if (nodesLoadedSoFar % 10000 == 0)
                    monitor.updateStatus(MonitoredProcess.LOAD_NODES, nodesLoadedSoFar, totalNodeCount);
            }
            
        } catch (EOFException e) { }
        
        monitor.updateStatus(MonitoredProcess.LOAD_NODES, nodesLoadedSoFar, totalNodeCount);
        return nodesById;
    }
    
    private void loadEdgesGivenNodes(HashMap<Long,Node> nodesById, DataInputStream source, StatusMonitor monitor) throws IOException {
        Preconditions.checkNoneNull(monitor);
        long fileFormatVersion = source.readLong();
        checkFileFormatVersion(fileFormatVersion);
        
        long totalEdgeCount = (fileFormatVersion >= 6 ? source.readLong() : -1);
        long edgesLoadedSoFar = 0;
        HashMap<Long,DirectedEdge> edgesById = new HashMap(Math.max(1000, (int)totalEdgeCount));
        
        try {
            monitor.updateStatus(MonitoredProcess.LOAD_WAYS, edgesLoadedSoFar, totalEdgeCount);
            
            while(true) {
                long edgeId = source.readLong();
                long sourceDataEdgeId = source.readLong();
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
                    de = new DirectedEdge(edgeId, sourceDataEdgeId, firstEdge, secondEdge);
                } else {
                    de = new DirectedEdge(edgeId, sourceDataEdgeId, fromNode, toNode, driveTimeMs, (isAccessOnly?AccessOnly.TRUE:AccessOnly.FALSE));
                }
                
                fromNode.edgesFrom.add(de);
                toNode.edgesTo.add(de);
                edgesById.put(edgeId, de);
                
                edgesLoadedSoFar++;
                if (edgesLoadedSoFar % 10000 == 0)
                    monitor.updateStatus(MonitoredProcess.LOAD_WAYS, edgesLoadedSoFar, totalEdgeCount);
            }
            
        } catch (EOFException e) { }
        
        monitor.updateStatus(MonitoredProcess.LOAD_WAYS, edgesLoadedSoFar, totalEdgeCount);
        Node.sortNeighborListsAll(nodesById.values());
    }
    
    
    private void writeNodesWithoutEdges(Collection<Node> toWrite, DataOutputStream dest) throws IOException {
        dest.writeLong(FILE_VERSION_WRITTEN);
        dest.writeLong(toWrite.size());
        
        for (Node n : toWrite) {
            dest.writeLong(n.nodeId);
            dest.writeLong(n.sourceDataNodeId);
            if (n.isContracted()) { // REVISIT next time we bump the file version, maybe write it as an int?
                dest.writeLong(n.contractionOrder);
            } else {
                dest.writeLong(Long.MAX_VALUE);
            }
            int properties = (!n.contractionAllowed?0x01:0x00) | (n.barrier==Barrier.TRUE?0x02:0x00);
            dest.writeByte(properties);
            dest.writeDouble(n.lat);
            dest.writeDouble(n.lon);
        }
    }
    
    private void writeEdges(Collection<Node> toWrite, DataOutputStream dos) throws IOException {
        dos.writeLong(FILE_VERSION_WRITTEN);
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
        dos.writeLong(de.sourceDataEdgeId);
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
    
    private HashSet<TurnRestriction> readTurnRestrictions(DataInputStream source) throws IOException {
        long fileFormatVersion = source.readLong();
        checkFileFormatVersion(fileFormatVersion);
        
        long totalRestrictionCount = (fileFormatVersion >= 6 ? source.readLong() : -1);
        HashSet<TurnRestriction> result = new HashSet(Math.max(1000, (int)totalRestrictionCount));
        
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
                result.add(new TurnRestriction(turnRestrictionId, trt, edgeIds));
            }
            
        } catch (EOFException e) { }
        
        return result;
    }
    
    private void writeTurnRestrictions(Collection<TurnRestriction> toWrite, DataOutputStream dos) throws IOException {
        dos.writeLong(FILE_VERSION_WRITTEN);
        dos.writeLong(toWrite.size());
        
        for (TurnRestriction tr : toWrite) {
            
            dos.writeLong(tr.getTurnRestrictionId());
            dos.writeBoolean(tr.getType()==TurnRestriction.TurnRestrictionType.NOT_ALLOWED);
            dos.writeInt(tr.getDirectedEdgeIds().size());
            for (Long edgeId : tr.getDirectedEdgeIds()) {
                dos.writeLong(edgeId);
            }
            
        }
    }
    
    private void checkFileFormatVersion(long fileFormatVersion) throws IOException{
        if (fileFormatVersion < MIN_FILE_VERSION_SUPPORTED) {
            throw new IOException("File format version, " + fileFormatVersion + ", is below lowest version supported, " + MIN_FILE_VERSION_SUPPORTED);
        } else if (fileFormatVersion > MAX_FILE_VERSION_SUPPORTED) {
            throw new IOException("File format version, " + fileFormatVersion + ", is above greatest version supported, " + MAX_FILE_VERSION_SUPPORTED);
        }
    }

}
