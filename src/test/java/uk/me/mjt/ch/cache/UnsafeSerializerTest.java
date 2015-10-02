
package uk.me.mjt.ch.cache;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import uk.me.mjt.ch.PartialSolution;

public class UnsafeSerializerTest {

    public UnsafeSerializerTest() {
    }

    @Test
    public void testSerialize() {
        UpAndDownPair upDownPair = makeTestUpDownPair();
        
        UnsafeSerializer instance = new UnsafeSerializer();
        byte[] binary = instance.serialize(upDownPair);
        
        UpAndDownPair loopback = instance.deserialize(binary);
        
        assertEquals(upDownPair.up,loopback.up);
        assertEquals(upDownPair.down,loopback.down);
    }
    
    private UpAndDownPair makeTestUpDownPair() {
        Random r = new Random();
        
        long[] upNodeIds = new long[] {r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong()};
        long[] upContractionOrders = new long[] {r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong()};
        int[] upDriveTimes = new int[] {r.nextInt(),r.nextInt(),r.nextInt(),r.nextInt()};
        long[] upViaEdges = new long[] {r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong()};
        PartialSolution.UpwardSolution up = new PartialSolution.UpwardSolution(upNodeIds, upContractionOrders, upDriveTimes, upViaEdges);
        
        long[] downNodeIds = new long[] {r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong()};
        long[] downContractionOrders = new long[] {r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong()};
        int[] downDriveTimes = new int[] {r.nextInt(),r.nextInt(),r.nextInt(),r.nextInt(),r.nextInt()};
        long[] downViaEdges = new long[] {r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong(),r.nextLong()};
        PartialSolution.DownwardSolution down = new PartialSolution.DownwardSolution(downNodeIds, downContractionOrders, downDriveTimes, downViaEdges);
        
        return new UpAndDownPair(up, down);
    }
    
    
    
}