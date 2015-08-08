package uk.me.mjt.ch;

import java.util.*;
import uk.me.mjt.ch.loader.BinaryFormat;

public class ContractAndSerialiseUk {
    
    
    public static void main(String[] args) {
        
        try {
            
            System.out.println("Loading data...");
            long startTime = System.currentTimeMillis();
            BinaryFormat bf = new BinaryFormat();
            HashMap<Long,Node> allNodes=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-ways.dat");
            
            Node hatfield = allNodes.get(253199386L); // https://www.openstreetmap.org/node/253199386 Hatfield
            Node herbalHill = allNodes.get(18670884L); // https://www.openstreetmap.org/node/18670884 Herbal Hill
            System.out.println(Dijkstra.dijkstrasAlgorithm(allNodes, hatfield, herbalHill, Dijkstra.Direction.FORWARDS).toString());
            
            InaccessibleNodes.removeNodesNotBidirectionallyAccessible(allNodes, hatfield);
            AccessOnly.stratifyMarkedAndImplicitAccessOnlyClusters(allNodes, hatfield);
            
            CheckOsmRouting.checkUncontracted(allNodes);
            
            GraphContractor contractor = new GraphContractor(allNodes);

            long startTime2 = System.currentTimeMillis();
            contractor.initialiseContractionOrder();
            contractor.contractAll();
            long duration = System.currentTimeMillis()-startTime2;
            System.out.println("Performed contraction in " + duration + "ms.");
            
            CheckOsmRouting.checkContracted(allNodes);
            
            bf.writeWays(allNodes.values(), 
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-ways.dat");
            
            HashMap<Long,Node> readback=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-ways.dat");
            
            boolean readbackMatch = Util.deepEquals(allNodes, readback, true);
            
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
