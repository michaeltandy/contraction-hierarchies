package uk.me.mjt.ch;

import uk.me.mjt.ch.Dijkstra.Direction;
import java.io.*;
import java.util.HashMap;
import uk.me.mjt.ch.loader.NodeLoadNavCsv;

public class Main {
    
    public static void main(String[] args) {
        
        try {
            BufferedReader br = (new BufferedReader( new FileReader("/home/mtandy/Documents/contraction hierarchies/nonprofile ch/resources/hatfield-b.csv")));
            HashMap<String,Node> allNodes=NodeLoadNavCsv.readData(br);

            DijkstraSolution forwards = Dijkstra.dijkstrasAlgorithm(allNodes, allNodes.get("102945686"), allNodes.get("849770864"), Direction.FORWARDS);
            System.out.println("Forwards: " + forwards);
            
            DijkstraSolution backwards = Dijkstra.dijkstrasAlgorithm(allNodes, allNodes.get("102945686"), allNodes.get("849770864"), Direction.BACKWARDS);
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

            DijkstraSolution contracted = Dijkstra.contractedGraphDijkstra(allNodes, allNodes.get("102945686"), allNodes.get("849770864"));
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
    /*
    public static void testWithLongGraph() {
        
        BufferedReader br = new BufferedReader( new InputStreamReader ( new ByteArrayInputStream(bigLongGraph().getBytes()) ) );
        Main instance = new Main();
        
        try {
            instance.readData(br);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("Testing with " + instance.allNodes.size() + " nodes...");
        
        String path = instance.dijkstrasAlgorithm();
        //System.out.println("Selected path: " + path);
        
    }
    
    public static String bigLongGraph() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Start ASDF\n");
        String lastCamp = "ASDF";
        for (int i=100 ; i<500100 ; i++) {
            String thisCamp = numToString(i);
            sb.append(lastCamp + " " + thisCamp + ":1\n");
            lastCamp = thisCamp;
        }
        sb.append(lastCamp + " PEAK:1\n");
        sb.append("END\n");
        
        return sb.toString();
    }
    
    public static String numToString(int number) {
        if (number==0) return "Q";
        if (number<0) return "<0?";
        
        char A = "F".charAt(0);
        String s = "";
        int numLetters = (int)Math.floor(Math.log(number)/Math.log(10)+1);
        //System.out.println("numLetters: " + numLetters);
        char[] chars  = new char[numLetters];
        for (int i=0 ; i<numLetters ; i++) {
            //System.out.println("number: " + number + " / " + (number%27));
            chars[numLetters-i-1] = (char) (A + (number%10));
            number = number/10;
        }
        return new String(chars);
    }
    */
}
