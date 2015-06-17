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
        HashMap<Long, Node> a = MakeTestData.makeSimpleThreeEntry();
        HashMap<Long, Node> b = MakeTestData.makeSimpleThreeEntry();
        assertTrue(Util.deepEquals(a, b));
        
        Node n = a.get(2L);
        n.edgesFrom.remove(0);
        
        assertFalse(Util.deepEquals(a, b));
    }
    
}
