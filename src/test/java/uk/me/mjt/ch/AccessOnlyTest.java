
package uk.me.mjt.ch;

import org.junit.Test;
import static org.junit.Assert.*;

public class AccessOnlyTest {

    public AccessOnlyTest() {
    }

    @Test
    public void testFollowedBy() {
        assertEquals(AccessOnly.TRUE,AccessOnly.NO_EDGES_YET.followedBy(AccessOnly.TRUE));
        assertEquals(AccessOnly.TRUE,AccessOnly.TRUE.followedBy(AccessOnly.TRUE));
        assertEquals(AccessOnly.WITH_GAP,AccessOnly.TRUE.followedBy(AccessOnly.END_ONLY));
        assertEquals(AccessOnly.WITH_GAP,AccessOnly.WITH_GAP.followedBy(AccessOnly.NO_EDGES_YET));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalA() {
        assertFalse(AccessOnly.WITH_GAP.mayBeFollowedBy(AccessOnly.FALSE));
        AccessOnly.WITH_GAP.followedBy(AccessOnly.FALSE);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalB() {
        assertFalse(AccessOnly.START_ONLY.mayBeFollowedBy(AccessOnly.START_ONLY));
        AccessOnly.START_ONLY.followedBy(AccessOnly.START_ONLY);
    }

}