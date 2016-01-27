package uk.me.mjt.ch.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import static org.junit.Assert.*;
import uk.me.mjt.ch.AccessOnly;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.MakeTestData;
import uk.me.mjt.ch.MapData;
import uk.me.mjt.ch.Node;
import uk.me.mjt.ch.Util;
import uk.me.mjt.ch.status.MonitoredProcess;
import uk.me.mjt.ch.status.StdoutStatusMonitor;
import static uk.me.mjt.ch.status.StdoutStatusMonitor.toString;

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
        writeAndReadBack(MakeTestData.makeSimpleThreeEntry());
    }
    
    @org.junit.Test
    public void testSourceDataEdgeId() throws Exception {
        MapData testData = MakeTestData.makeSimpleThreeEntry();
        DirectedEdge de = new DirectedEdge(100L, 200L, testData.getNodeById(1L), testData.getNodeById(2L), 123, AccessOnly.FALSE);
        de.addToToAndFromNodes();
        writeAndReadBack(testData);
    }
    
    private void writeAndReadBack(MapData testData) throws IOException {
        ByteArrayOutputStream nodesOut = new ByteArrayOutputStream();
        ByteArrayOutputStream waysOut = new ByteArrayOutputStream();
        ByteArrayOutputStream turnRestrictionsOut = new ByteArrayOutputStream();
        
        BinaryFormat instance = new BinaryFormat();
        
        instance.write(testData, new DataOutputStream(nodesOut), new DataOutputStream(waysOut), new DataOutputStream(turnRestrictionsOut));
        
        ByteArrayInputStream nodesIn = new ByteArrayInputStream(nodesOut.toByteArray());
        ByteArrayInputStream waysIn = new ByteArrayInputStream(waysOut.toByteArray());
        ByteArrayInputStream turnRestrictionsIn = new ByteArrayInputStream(turnRestrictionsOut.toByteArray());
        LoggingStatusMonitor monitor = new LoggingStatusMonitor();
        
        MapData loopback = instance.read(nodesIn, waysIn, turnRestrictionsIn, monitor);
        
        assertTrue(Util.deepEquals(testData, loopback, true));
        
        String monitorStatuses = monitor.statuses.toString();
        assertEquals(6, monitor.statuses.size());
        assertTrue(monitorStatuses.contains(" 0.00%"));
        assertTrue(monitorStatuses.contains(" 100.00%"));
    }
    
    @org.junit.Test(expected=IOException.class)
    public void testExceptionForOldVersion() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(-1);
        dos.writeChars("some other data");
        dos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        BinaryFormat instance = new BinaryFormat();
        instance.read(bais, bais, bais, new StdoutStatusMonitor());
    }
    
    private class LoggingStatusMonitor extends StdoutStatusMonitor {
        public final ArrayList<String> statuses = new ArrayList();
        
        @Override
        public void updateStatus(MonitoredProcess process, long completed, long total) {
            statuses.add(toString(process, completed, total));
        }
    }
    
}
