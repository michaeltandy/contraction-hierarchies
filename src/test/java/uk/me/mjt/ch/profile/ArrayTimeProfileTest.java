package uk.me.mjt.ch.profile;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

public class ArrayTimeProfileTest {
    private final int MILLIS_IN_A_SECOND = 1000;
    private final int MILLIS_IN_A_MINUTE = 60*MILLIS_IN_A_SECOND;
    private final int MILLIS_IN_AN_HOUR = 60*MILLIS_IN_A_MINUTE;
    private final int MILLIS_IN_A_DAY = 24*MILLIS_IN_AN_HOUR;

    public ArrayTimeProfileTest() {
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidTransitTime() {
        repeat((15*MILLIS_IN_A_SECOND)+1,0);
    }
    
    public void testValidTransitTime() {
        repeat(15*MILLIS_IN_A_SECOND,0);
    }

    @Test
    public void testFlatTransitTimes() {
        ArrayTimeProfile edge12 = repeat(10000);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=1000) {
            assertEquals(10000,edge12.transitTimeAt(i));
        }
        
        ArrayTimeProfile edge23 = repeat(10000);
        
        ArrayTimeProfile edge13 = edge12.followedBy(edge23);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=1000) {
            assertEquals(20000,edge13.transitTimeAt(i));
        }
    }
    
    @Test
    public void testVaryingTransitTimes() {
        ArrayTimeProfile edge12 = repeat(2100000,2700000);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=60000) {
            assertTrue(edge12.transitTimeAt(i) >= 2100000);
            assertTrue(edge12.transitTimeAt(i) <= 2700000);
        }
        
        ArrayTimeProfile edge23 = repeat(600000,0);
        ArrayTimeProfile edge13 = edge12.followedBy(edge23);
        
        for (int i=-1000 ; i<MILLIS_IN_A_DAY ; i+=60000) {
            assertTrue(edge13.transitTimeAt(i) >= 2100000+0);
            assertTrue(edge13.transitTimeAt(i) <= 2700000+600000);
        }
    }
    
    @Test
    @Ignore
    public void testStepUpAtSixPm() {
        int[] data = new int[ArrayTimeProfile.ARRAY_LENGTH_SEGMENTS];
        Arrays.fill(data, 0, 18*4, 15*MILLIS_IN_A_MINUTE);
        Arrays.fill(data, 18*4, data.length, 30*MILLIS_IN_A_MINUTE);
        ArrayTimeProfile a = new ArrayTimeProfile(data);
        
        int fiveThirtyPm = 17*MILLIS_IN_AN_HOUR+30*MILLIS_IN_A_MINUTE;
        
        ArrayTimeProfile justBeforeStep = repeat(15*MILLIS_IN_A_MINUTE).followedBy(a);
        ArrayTimeProfile justAfterStep = repeat(16*MILLIS_IN_A_MINUTE).followedBy(a);
        
        assertEquals(30*MILLIS_IN_A_MINUTE, justBeforeStep.transitTimeAt(fiveThirtyPm));
        assertEquals(30*MILLIS_IN_A_MINUTE+90*MILLIS_IN_A_SECOND, justAfterStep.transitTimeAt(fiveThirtyPm));
        
        ArrayTimeProfile nearRiseEnd = repeat(45*MILLIS_IN_A_MINUTE).followedBy(a);
        ArrayTimeProfile afterRiseComplete = repeat(45*MILLIS_IN_A_MINUTE).followedBy(a);
        
    }
    
    static ArrayTimeProfile repeat(int... sequence) {
        int[] data = new int[ArrayTimeProfile.ARRAY_LENGTH_SEGMENTS];
        for (int i=0 ; i<data.length ; i++) {
            int ix = (i/4);
            data[i] = sequence[ix % sequence.length];
        }
        return new ArrayTimeProfile(data);
    }

}