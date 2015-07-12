package uk.me.mjt.ch;

import uk.me.mjt.ch.Dijkstra.Direction;
import java.io.*;
import java.util.HashMap;
import uk.me.mjt.ch.loader.BinaryFormat;
import uk.me.mjt.ch.loader.NodeLoadCsv;

public class Main {
    
    public static void main(String[] args) {
        
        try {
            
            /*BufferedReader br = (new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/hatfield-osm.csv"))));
            HashMap<Long,Node> allNodes=NodeLoadCsv.readData(br);
            Node startNode = allNodes.get(253199383L); // https://www.openstreetmap.org/node/253199383
            Node endNode = allNodes.get(651825497L); // https://www.openstreetmap.org/node/651825497*/
            /*BufferedReader br = (new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/semicircle.csv"))));
            HashMap<Long,Node> allNodes=NodeLoadCsv.readData(br);
            Node startNode = allNodes.get(1L);
            Node endNode = allNodes.get(9L);*/
            
            BinaryFormat bf = new BinaryFormat();
            //HashMap<Long,Node> allNodes=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/hertfordshire-nodes.dat",
            //        "/home/mtandy/Documents/contraction hierarchies/binary-test/hertfordshire-ways.dat");
            HashMap<Long,Node> allNodes=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-ways.dat");
            Node startNode = allNodes.get(253199386L); // https://www.openstreetmap.org/node/253199386 Hatfield
            //Node endNode = allNodes.get(26805194L); // https://www.openstreetmap.org/node/26805194 St Albans
            Node endNode = allNodes.get(18670884L); // https://www.openstreetmap.org/node/18670884 Herbal Hill
            
            
            //System.out.println(GeoJson.linksInBbox(allNodes.values(), 51.744675, -0.3088188, 51.7636966, -0.241098));
            
            long startTime1 = System.currentTimeMillis();
            DijkstraSolution forwards = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, endNode, Direction.FORWARDS);
            System.out.println("Forwards: " + forwards);
            System.out.println("Regular forwards search in " + (System.currentTimeMillis()-startTime1) + "ms.");
            
            DijkstraSolution backwards = Dijkstra.dijkstrasAlgorithm(allNodes, startNode, endNode, Direction.BACKWARDS);
            System.out.println("Backwards: " + backwards);

            GraphContractor contractor = new GraphContractor(allNodes);

            long startTime2 = System.currentTimeMillis();
            contractor.initialiseContractionOrder();
            contractor.contractAll();
            long duration = System.currentTimeMillis()-startTime2;
            System.out.println("Performed contraction in " + duration + "ms.");
            
            long startTime3 = System.currentTimeMillis();
            DijkstraSolution contracted = ContractedDjikstra.contractedGraphDijkstra(allNodes, startNode, endNode);
            System.out.println("Contraction: "+contracted);
            System.out.println("Contracted forwards search in " + (System.currentTimeMillis()-startTime3) + "ms.");

            if (contracted.toString().equals(forwards.toString())) {
                System.out.println("Contracted and forwards match!");
            } else {
                System.out.println("Contracted and forwards don't match?!?! <======");
            }
            
            System.out.println(GeoJson.solution(contracted));
            //System.out.println(GeoJson.allLinks(allNodes.values()));
            //System.out.println(GeoJson.allLinks(allNodes.values()));
            
            /*bf.writeWays(allNodes.values(), 
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-contracted-ways.dat");
            
            HashMap<Long,Node> readback=bf.read("/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-contracted-nodes.dat",
                    "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain-contracted-ways.dat");
            
            boolean readbackMatch = Util.deepEquals(allNodes, readback);
            System.out.println("Readback match: " + readback);
            if (!readbackMatch) {
                System.exit(1);
            }*/

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
}
