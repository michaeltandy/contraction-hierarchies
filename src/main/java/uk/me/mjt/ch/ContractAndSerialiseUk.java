package uk.me.mjt.ch;

import java.util.*;
import uk.me.mjt.ch.loader.BinaryFormat;
import uk.me.mjt.ch.status.StdoutStatusMonitor;

public class ContractAndSerialiseUk {
    
    
    public static void main(String[] args) {
        String filenamePrefix;
        long startNodeId;
        if (args.length > 3) {
            filenamePrefix = args[1];
            startNodeId = Long.parseLong(args[2]);
        } else {
            filenamePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain";
            startNodeId = 253199386L; // Hatfield
        }
        
        try {
            
            System.out.println("Loading data...");
            BinaryFormat bf = new BinaryFormat();
            MapData allNodes=bf.read(filenamePrefix+"-nodes.dat", filenamePrefix+"-ways.dat",
                    filenamePrefix+"-turnrestrictions.dat", new StdoutStatusMonitor());
            
            Node startNode = allNodes.getNodeById(startNodeId);
            
            System.out.println("Adjusting for restrictions...");
            allNodes = AdjustGraphForRestrictions.makeNewGraph(allNodes, startNode);
            
            CheckOsmRouting.checkUncontracted(allNodes);
            
            System.out.println("Contracting...");
            GraphContractor contractor = new GraphContractor(allNodes);

            long startTime2 = System.currentTimeMillis();
            contractor.initialiseContractionOrder();
            contractor.contractAll();
            long duration = System.currentTimeMillis()-startTime2;
            System.out.println("Performed contraction in " + duration + "ms.");
            
            bf.write(allNodes,filenamePrefix+"-contracted-nodes.dat",filenamePrefix+"-contracted-ways.dat");
            MapData readback=bf.read(filenamePrefix+"-contracted-nodes.dat",filenamePrefix+"-contracted-ways.dat", new StdoutStatusMonitor());
            boolean readbackMatch = Util.deepEquals(allNodes, readback, true);
            
            CheckOsmRouting.checkContracted(allNodes);
            
            if (!readbackMatch) {
                System.out.println("Readback mismatch?!");
                System.exit(2);
            } else {
                System.out.println("Readback success!");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
}
