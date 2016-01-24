package uk.me.mjt.ch;

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
        assertTrue(Util.deepEquals(a, b, true));
        
        Node n = a.getNodeById(2L);
        n.edgesFrom.remove(0);
        
        assertFalse(Util.deepEquals(a, b, true));
    }
    
    @Test
    public void testNodeEquality() {
        MapData a = MakeTestData.makeSimpleThreeEntry();
        MapData b = MakeTestData.makeSimpleThreeEntry();
        
        assertTrue(Util.deepEquals(a.getNodeById(2L), b.getNodeById(2L), true));
        assertFalse(Util.deepEquals(a.getNodeById(2L), b.getNodeById(3L), true));
    }
    
}
