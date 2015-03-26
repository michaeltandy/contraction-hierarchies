/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.me.mjt.ch.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import static org.junit.Assert.*;
import uk.me.mjt.ch.MakeTestData;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.Util;

/**
 *
 * @author mtandy
 */
public class BinaryFormatTest {
    
    public BinaryFormatTest() {
    }
    

    @org.junit.Test
    public void testLoopback() throws Exception {
        ByteArrayOutputStream nodesOut = new ByteArrayOutputStream();
        ByteArrayOutputStream waysOut = new ByteArrayOutputStream();
        
        HashMap<Long, Node> testData = MakeTestData.makeSimpleThreeEntry();
        BinaryFormat instance = new BinaryFormat();
        
        instance.writeNodesWithoutEdges(testData.values(), new DataOutputStream(nodesOut));
        instance.writeEdges(testData.values(), new DataOutputStream(waysOut));
        
        ByteArrayInputStream nodesIn = new ByteArrayInputStream(nodesOut.toByteArray());
        ByteArrayInputStream waysIn = new ByteArrayInputStream(waysOut.toByteArray());
        
        HashMap<Long, Node> loopback = instance.readNodes(new DataInputStream(nodesIn));
        instance.loadEdgesGivenNodes(loopback,new DataInputStream(waysIn));
        
        assertTrue(Util.deepEquals(testData, loopback));
    }

    
    
}
