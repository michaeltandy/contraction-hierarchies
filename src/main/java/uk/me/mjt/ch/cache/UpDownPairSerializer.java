
package uk.me.mjt.ch.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import uk.me.mjt.ch.PartialSolution;
import uk.me.mjt.ch.Preconditions;


public class UpDownPairSerializer {
    private static final int BINARY_FORMAT_VERSION = 1;
    
    public byte[] serialize(UpAndDownPair upDownPair) {
        Preconditions.checkNoneNull(upDownPair);
        try 
            (ByteArrayOutputStream nodesOut = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(nodesOut)) {
            
            dos.writeInt(BINARY_FORMAT_VERSION);
            writePartialSolution(upDownPair.up,dos);
            writePartialSolution(upDownPair.down,dos);
            
            dos.flush();
            return nodesOut.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    private static void writePartialSolution(PartialSolution ps, DataOutputStream dos) throws IOException {
        //long[] nodeIds, long[] contractionOrders, int[] totalDriveTimes, long[] viaEdges
        int entries = ps.getNodeIds().length;
        dos.writeInt(entries);
        for (long l : ps.getNodeIds())
            dos.writeLong(l);
        for (long l : ps.getContractionOrders())
            dos.writeLong(l);
        for (int l : ps.getTotalDriveTimes())
            dos.writeInt(l);
        for (long l : ps.getViaEdges())
            dos.writeLong(l);
    }
    
    
    public UpAndDownPair deserialize(byte[] binary) {
        if (binary == null)
            return null;
        
        try 
            (ByteArrayInputStream bais = new ByteArrayInputStream(binary);
            DataInputStream dis = new DataInputStream(bais)) {
            
            int binaryFormatVersion = dis.readInt();
            Preconditions.require(binaryFormatVersion==BINARY_FORMAT_VERSION);
            
            int upEntryCount = dis.readInt();
            PartialSolution.UpwardSolution up = new PartialSolution.UpwardSolution(
                    readLongArray(dis,upEntryCount),
                    readLongArray(dis,upEntryCount),
                    readIntArray(dis,upEntryCount),
                    readLongArray(dis,upEntryCount));
            
            int downEntryCount = dis.readInt();
            PartialSolution.DownwardSolution down = new PartialSolution.DownwardSolution(
                    readLongArray(dis,downEntryCount),
                    readLongArray(dis,downEntryCount),
                    readIntArray(dis,downEntryCount),
                    readLongArray(dis,downEntryCount));
            
            return new UpAndDownPair(up, down);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static long[] readLongArray(DataInputStream dis, int length) throws IOException {
        long[] read = new long[length];
        for (int i=0 ; i<length ; i++) {
            read[i]=dis.readLong();
        }
        return read;
    }
    
    private static int[] readIntArray(DataInputStream dis, int length) throws IOException {
        int[] read = new int[length];
        for (int i=0 ; i<length ; i++) {
            read[i]=dis.readInt();
        }
        return read;
    }
}
