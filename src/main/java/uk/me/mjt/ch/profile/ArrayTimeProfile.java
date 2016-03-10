
package uk.me.mjt.ch.profile;

import java.util.Arrays;
import uk.me.mjt.ch.Preconditions;


public class ArrayTimeProfile implements TimeProfile {
    private static final int SEGMENT_WIDTH_MS = 15*60*1000;
    private static final int SEGMENT_MIDPOINT_MS = SEGMENT_WIDTH_MS/2;
    public static final int ARRAY_LENGTH_SEGMENTS = 24*4;
    
    private final int[] transitDurationMs;
    
    public ArrayTimeProfile(int[] transitDurationMs) {
        validateArray(transitDurationMs);
        this.transitDurationMs = Arrays.copyOf(transitDurationMs, transitDurationMs.length);
    }
    
    private static void validateArray(int[] toCheck) {
        if (toCheck == null || toCheck.length != ARRAY_LENGTH_SEGMENTS)
            throw new IllegalArgumentException("Invalid array - null or wrong length");
        
        int lastVal = toCheck[0];
        for (int i=0 ; i<toCheck.length ; i++) {
            if (toCheck[i] < 0)
                throw new IllegalArgumentException("Invalid array - negative");
            
            int delta = toCheck[i]-lastVal;
            
            if (delta < -SEGMENT_WIDTH_MS) {
                throw new IllegalArgumentException("Invalid array - profile to steep");
            }
        }
    }
    
    @Override
    public int transitTimeAt(int milliseconds) {
        if (milliseconds < SEGMENT_MIDPOINT_MS) {
            return transitDurationMs[0]; 
        } else if (milliseconds > SEGMENT_WIDTH_MS*(ARRAY_LENGTH_SEGMENTS-1)) {
            return transitDurationMs[ARRAY_LENGTH_SEGMENTS-1];
        }
        
        int firstBlock = (milliseconds-SEGMENT_MIDPOINT_MS)/SEGMENT_WIDTH_MS;
        int remainder = (milliseconds-SEGMENT_MIDPOINT_MS)%SEGMENT_WIDTH_MS;
        
        int firstTransitDuration = transitDurationMs[firstBlock];
        int secondTransitDuration = transitDurationMs[firstBlock+1];
        
        long numerator = secondTransitDuration*(long)remainder + firstTransitDuration*(long)(SEGMENT_WIDTH_MS-remainder);
        return (int)(numerator/SEGMENT_WIDTH_MS);
    }
    
    public ArrayTimeProfile followedBy(ArrayTimeProfile after) {
        Preconditions.checkNoneNull(after);
        
        int[] newDurationsMs = new int[ARRAY_LENGTH_SEGMENTS];
        
        for (int i=0 ; i<newDurationsMs.length ; i++) {
            int secondSegmentEntryTime = i*SEGMENT_WIDTH_MS+SEGMENT_MIDPOINT_MS+transitDurationMs[i];
            int secondSegmentTransitTime = after.transitTimeAt(secondSegmentEntryTime);
            newDurationsMs[i] = transitDurationMs[i] + secondSegmentTransitTime;
        }
        
        return new ArrayTimeProfile(newDurationsMs);
    }

    @Override
    public String toString() {
        return "Profile"+ Arrays.toString(transitDurationMs);
    }
    
    
    
    
}
