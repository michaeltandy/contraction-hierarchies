package uk.me.mjt.ch;

import java.util.*;
import uk.me.mjt.ch.loader.BinaryFormat;

public class ContractAndSerialiseLondon {
    
    
    public static void main(String[] args) {
        
        try {
            
            System.out.println("Loading data...");
            long startTime = System.currentTimeMillis();
            BinaryFormat bf = new BinaryFormat();
            MapData allNodes=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/greater-london-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/greater-london-ways.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/greater-london-turnrestrictions.dat");
            allNodes.validate();
            
            Node herbal = allNodes.getNodeById(18670884L);
            Node angel = allNodes.getNodeById(1670708085L);
            System.out.println("Before graph adjustment: " + allNodes.getNodeCount());
            System.out.println(Dijkstra.dijkstrasAlgorithm(herbal, angel, Dijkstra.Direction.FORWARDS).toString());
            
            allNodes = AdjustGraphForRestrictions.makeNewGraph(allNodes, herbal);
            
            ColocatedNodeSet herbal2 = allNodes.getNodeBySourceDataId(18670884L);
            ColocatedNodeSet angel2 = allNodes.getNodeBySourceDataId(1670708085L);
            DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(angel2, herbal2, Dijkstra.Direction.FORWARDS);
            System.out.println("After graph adjusted for restrictions:" + allNodes.getNodeCount());
            System.out.println(ds.toString());
            System.out.println(GeoJson.solution(ds));
            
            GraphContractor contractor = new GraphContractor(allNodes);

            long startTime2 = System.currentTimeMillis();
            contractor.initialiseContractionOrder();
            contractor.contractAll();
            long duration = System.currentTimeMillis()-startTime2;
            System.out.println("Performed contraction in " + duration + "ms.");
            
            ds = ContractedDijkstra.contractedGraphDijkstra(allNodes, angel2, herbal2);
            System.out.println("After adjusted graph contracted:" + allNodes.getNodeCount());
            System.out.println(ds.toString());
            System.out.println(GeoJson.solution(ds));
            
            bf.write(allNodes,
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/greater-london-new-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/greater-london-new-contracted-ways.dat");
            
            MapData readback=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/greater-london-new-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/greater-london-new-contracted-ways.dat");
            
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
