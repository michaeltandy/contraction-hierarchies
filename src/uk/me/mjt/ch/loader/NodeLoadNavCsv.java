package uk.me.mjt.ch.loader;

import java.io.*;
import java.util.HashMap;
import java.util.regex.*;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.Node;

public class NodeLoadNavCsv {

    // Format is like "815636590,874581909,102962369,19.50,51.73166,-0.20265,51.73248,-0.20716,5"
    
    public static HashMap<String,Node> readData(BufferedReader br) throws IOException{
        HashMap<String,Node> allNodes = new HashMap<String,Node>();

        Pattern p = Pattern.compile(",");

        while (true) {
            String thisLine = br.readLine();
            if (thisLine == null) {
                return allNodes;
            }

            String[] parts = p.split(thisLine,-1);

            if (parts.length >=4) {

                String fromName = parts[1];
                Node fromNode = getOrCreate(allNodes, fromName);

                String toName = parts[2];
                Node toNode = getOrCreate(allNodes, toName);

                float distance = Float.parseFloat(parts[3]);
                DirectedEdge de = new DirectedEdge(fromNode, toNode, distance);
                fromNode.edgesFrom.add(de);
                toNode.edgesTo.add(de);
                
            } else { // If none of our parsers match it...
                throw new IOException("I can't understand the line " + thisLine);
            }
        } // end of while loop
        
    }

    public static Node getOrCreate(HashMap<String,Node> allNodes, String nodeName) {
        if (allNodes.containsKey(nodeName)) {
            return allNodes.get(nodeName);
        } else {
            Node n = new Node(nodeName);
            allNodes.put(nodeName, n);
            return n;
        }
    }

}
