package uk.me.mjt.ch.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static org.junit.Assert.*;
import uk.me.mjt.ch.Barrier;
import uk.me.mjt.ch.MakeTestData;
import uk.me.mjt.ch.MapData;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.Util;

public class BinaryFormatTest {
    
    public BinaryFormatTest() {
    }
    

    @org.junit.Test
    public void testLoopback() throws Exception {
        MapData testData = MakeTestData.makeSimpleThreeEntry();
        writeAndReadBack(testData);
    }
    
    @org.junit.Test
    public void testLoopbackAccessOnly() throws Exception {
        MapData testData = MakeTestData.makePartlyAccessOnlyRing();
        writeAndReadBack(testData);
    }
    
    @org.junit.Test
    public void testLoopbackGate() throws Exception {
        MapData testData = MakeTestData.makeGatedRow();
        writeAndReadBack(testData);
    }
    
    @org.junit.Test
    public void testTurnRestricted() throws Exception {
        MapData testData = MakeTestData.makeTurnRestrictedH();
        writeAndReadBack(testData);
    }
    
    @org.junit.Test
    public void testSynthetic() throws Exception {
        MapData testData = MakeTestData.makeSimpleThreeEntry();
        testData.add(new Node(5, testData.getNodeById(1)));
        writeAndReadBack(testData);
    }
    
    private void writeAndReadBack(MapData testData) throws IOException {
        ByteArrayOutputStream nodesOut = new ByteArrayOutputStream();
        ByteArrayOutputStream waysOut = new ByteArrayOutputStream();
        ByteArrayOutputStream turnRestrictionsOut = new ByteArrayOutputStream();
        
        BinaryFormat instance = new BinaryFormat();
        
        instance.writeNodesWithoutEdges(testData.getAllNodes(), new DataOutputStream(nodesOut));
        instance.writeEdges(testData.getAllNodes(), new DataOutputStream(waysOut));
        instance.writeTurnRestrictions(testData.allTurnRestrictions(), new DataOutputStream(turnRestrictionsOut));
        
        ByteArrayInputStream nodesIn = new ByteArrayInputStream(nodesOut.toByteArray());
        ByteArrayInputStream waysIn = new ByteArrayInputStream(waysOut.toByteArray());
        ByteArrayInputStream turnRestrictionsIn = new ByteArrayInputStream(turnRestrictionsOut.toByteArray());
        
        MapData loopback = instance.read(nodesIn, waysIn, turnRestrictionsIn);
        
        assertTrue(Util.deepEquals(testData, loopback, true));
    }

    
    
}
