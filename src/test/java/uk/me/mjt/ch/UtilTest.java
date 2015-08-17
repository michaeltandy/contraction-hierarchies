package uk.me.mjt.ch;

import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class UtilTest {
    
    public UtilTest() {
    }
    
    @Test
    public void testDeepEquals_HashMap_HashMap() {
        System.out.println("deepEquals");
        MapData a = MakeTestData.makeSimpleThreeEntry();
        MapData b = MakeTestData.makeSimpleThreeEntry();
        assertTrue(Util.deepEquals(a, b, false));
        
        Node n = a.getNodeById(2L);
        n.edgesFrom.remove(0);
        
        assertFalse(Util.deepEquals(a, b, false));
    }
    
}
