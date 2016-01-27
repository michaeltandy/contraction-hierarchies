package uk.me.mjt.ch;

import java.util.*;
import uk.me.mjt.ch.loader.BinaryFormat;
import uk.me.mjt.ch.status.StdoutStatusMonitor;

public class ContractAndSerialiseUk {
    
    
    public static void main(String[] args) {
        
        try {
            
            System.out.println("Loading data...");
            long startTime = System.currentTimeMillis();
            BinaryFormat bf = new BinaryFormat();
            MapData allNodes=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-ways.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-turnrestrictions.dat", new StdoutStatusMonitor());
            
            Node hatfield = allNodes.getNodeById(253199386L);
            Node asdf = allNodes.getNodeById(672630347L);
            System.out.println(Dijkstra.dijkstrasAlgorithm(hatfield, asdf, Dijkstra.Direction.FORWARDS).toString());
            
            // TODO restriction support goes here!
            
            CheckOsmRouting.checkUncontracted(allNodes);
            
            Node from = allNodes.getNodeById(672630347L);
            Node to = allNodes.getNodeById(927070648L);
            DijkstraSolution ds = Dijkstra.dijkstrasAlgorithm(from, to, Dijkstra.Direction.FORWARDS);
            System.out.println(ds.toString());
            System.out.println(GeoJson.solution(ds));
            
            GraphContractor contractor = new GraphContractor(allNodes);

            long startTime2 = System.currentTimeMillis();
            contractor.initialiseContractionOrder();
            contractor.contractAll();
            long duration = System.currentTimeMillis()-startTime2;
            System.out.println("Performed contraction in " + duration + "ms.");
            
            CheckOsmRouting.checkContracted(allNodes);
            
            bf.write(allNodes,
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-ways.dat");
            
            MapData readback=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-new-contracted-ways.dat", new StdoutStatusMonitor());
            
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
