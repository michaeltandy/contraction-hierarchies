/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.me.mjt.ch;

import java.util.ArrayList;
import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mtandy
 */
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
