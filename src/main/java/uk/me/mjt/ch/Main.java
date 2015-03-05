package uk.me.mjt.ch;

import uk.me.mjt.ch.Dijkstra.Direction;
import java.io.*;
import java.util.HashMap;
import uk.me.mjt.ch.loader.NodeLoadNavCsv;

public class Main {
    
    public static void main(String[] args) {
        
        try {
            //BufferedReader br = (new BufferedReader( new FileReader("/home/mtandy/Documents/contraction hierarchies/nonprofile ch/resources/hatfield-b.csv")));
            BufferedReader br = (new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/hatfield-b.csv"))));
            HashMap<Long,Node> allNodes=NodeLoadNavCsv.readData(br);

            DijkstraSolution forwards = Dijkstra.dijkstrasAlgorithm(allNodes, allNodes.get(102945686L), allNodes.get(849770864L), Direction.FORWARDS);
            System.out.println("Forwards: " + forwards);
            
            DijkstraSolution backwards = Dijkstra.dijkstrasAlgorithm(allNodes, allNodes.get(102945686L), allNodes.get(849770864L), Direction.BACKWARDS);
            System.out.println("Backwards: " + backwards);

            GraphContractor contractor = new GraphContractor(allNodes);

            long startTime = System.currentTimeMillis();
            contractor.initialiseContractionOrder();
            contractor.contractAll();
            long duration = System.currentTimeMillis()-startTime;
            System.out.println("Performed contraction in " + duration + "ms.");
            //routed = Dijkstra.dijkstrasAlgorithm(allNodes, allNodes.get("A"), allNodes.get("F"), Direction.FORWARDS);
            //System.out.println("Upwards: "+routed + ", dist " + routed.totalDistance);
            //routed = Dijkstra.dijkstrasAlgorithm(allNodes, allNodes.get("PEAK"), allNodes.get("F"), Direction.BACKWARDS);
            //System.out.println("Downwards: "+routed + ", dist " + routed.totalDistance);

            DijkstraSolution contracted = Dijkstra.contractedGraphDijkstra(allNodes, allNodes.get(102945686L), allNodes.get(849770864L));
            System.out.println("Contraction: "+contracted);

            if (contracted.toString().equals(forwards.toString())) {
                System.out.println("Contracted and forwards match!");
            } else {
                System.out.println("Contracted and forwards don't match?!?! <======");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
}
