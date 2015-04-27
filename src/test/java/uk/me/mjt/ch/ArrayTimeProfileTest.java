package uk.me.mjt.ch;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArrayTimeProfileTest {
    private final int MILLIS_IN_A_DAY = 24*60*60*1000;
    
    private final Node testNode1 = new Node(1,0,0);
    private final Node testNode2 = new Node(2,0,0);
    
    public ArrayTimeProfileTest() {
    }

    @Test
    public void testBasicTransitTimes() {
        ArrayTimeProfile instance = flatProfile(10000);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=1000) {
            assertEquals(10000,instance.transitTimeAt(i));
        }
        
        instance = instance.plusVia(instance,testNode1);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=1000) {
            assertEquals(20000,instance.transitTimeAt(i));
            assertEquals(testNode1, instance.viaPointAt(i));
        }
        
        instance = repeatedProfile(2100000,2700000);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=60000) {
            assertTrue(instance.transitTimeAt(i) >= 2100000);
            assertTrue(instance.transitTimeAt(i) <= 2700000);
        }
        
        instance = instance.plusVia(repeatedProfile(600000,0),testNode1);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=60000) {
            assertTrue(instance.transitTimeAt(i) >= 2100000+0);
            assertTrue(instance.transitTimeAt(i) <= 2700000+600000);
            assertEquals(testNode1, instance.viaPointAt(i));
        }
    }
    
    
    @Test
    public void testTransitTimes() {
        ArrayTimeProfile steppedProfile = repeatedProfile(1000,5000);
        ArrayTimeProfile steppedVia = steppedProfile.plusVia(steppedProfile, testNode1);
        
        ArrayTimeProfile flatProfile = flatProfile(2500);
        ArrayTimeProfile flatVia = flatProfile.plusVia(flatProfile, testNode2);
        
        ArrayTimeProfile min = steppedVia.minWith(flatVia);
        
        int node1Count = 0;
        int node2Count = 0;
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=1000) {
            assertTrue(min.transitTimeAt(i) >= 2000);
            assertTrue(min.transitTimeAt(i) <= 5000);
            if (min.viaPointAt(i)==testNode1) {
                node1Count++;
            } else if (min.viaPointAt(i)==testNode2) {
                node2Count++;
            } else {
                fail("This should never happen");
            }
        }
        
        System.out.println("Counts: " + node1Count + " & " + node2Count);
        assertEquals(node1Count, node2Count, 2);
    }
    
    
    
    private ArrayTimeProfile flatProfile(int duration) {
        int[] data = new int[ArrayTimeProfile.ARRAY_LENGTH_SEGMENTS];
        Arrays.fill(data, duration);
        return new ArrayTimeProfile(data);
    }
    
    private ArrayTimeProfile repeatedProfile(int... sequence) {
        int[] data = new int[ArrayTimeProfile.ARRAY_LENGTH_SEGMENTS];
        for (int i=0 ; i<data.length ; i++) {
            int ix = (i/4);
            data[i] = sequence[ix % sequence.length];
        }
        return new ArrayTimeProfile(data);
    }
    
    private ArrayTimeProfile slopedProfile(int slope) {
        int[] data = new int[ArrayTimeProfile.ARRAY_LENGTH_SEGMENTS];
        for (int i=0 ; i<data.length ; i++) {
            data[i] = slope*i;
        }
        return new ArrayTimeProfile(data);
    }
    
}
