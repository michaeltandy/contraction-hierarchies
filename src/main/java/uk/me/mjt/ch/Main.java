package uk.me.mjt.ch;

import uk.me.mjt.ch.Dijkstra.Direction;
import java.io.*;
import java.util.HashMap;
import uk.me.mjt.ch.loader.NodeLoadCsv;

public class Main {
    
    public static void main(String[] args) {
        
        try {
            //BufferedReader br = (new BufferedReader( new FileReader("/home/mtandy/Documents/contraction hierarchies/nonprofile ch/resources/hatfield-b.csv")));
            BufferedReader br = (new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/hatfield-osm.csv"))));
            HashMap<Long,Node> allNodes=NodeLoadCsv.readData(br);
            
            Node startNode = allNodes.get(253199383L); // https://www.openstreetmap.org/node/253199383
            Node endNode = allNodes.get(651825497L); // https://www.openstreetmap.org/node/651825497

            DijkstraSolution forwards = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, endNode, Direction.FORWARDS);
            System.out.println("Forwards: " + forwards);
            
            DijkstraSolution backwards = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, endNode, Direction.BACKWARDS);
            System.out.println("Backwards: " + backwards);

            GraphContractor contractor = new GraphContractor(allNodes);

            long startTime = System.currentTimeMillis();
            contractor.initialiseContractionOrder();
            contractor.contractAll();
            long duration = System.currentTimeMillis()-startTime;
            System.out.println("Performed contraction in " + duration + "ms.");
            
            DijkstraSolution contracted = Dijkstra.contractedGraphDijkstra(allNodes, startNode, endNode);
            System.out.println("Contraction: "+contracted);

            if (contracted.toString().equals(forwards.toString())) {
                System.out.println("Contracted and forwards match!");
            } else {
                System.out.println("Contracted and forwards don't match?!?! <======");
            }
            
            System.out.println(contracted.toGeoJson());

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
}
