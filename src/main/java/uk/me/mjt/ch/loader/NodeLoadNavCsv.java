package uk.me.mjt.ch.loader;

import java.io.*;
import java.util.HashMap;
import java.util.regex.*;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.Node;

public class NodeLoadNavCsv {

    // Format is like "815636590,874581909,102962369,19.50,51.73166,-0.20265,51.73248,-0.20716,5"
    
    public static HashMap<Long,Node> readData(BufferedReader br) throws IOException{
        HashMap<Long,Node> allNodes = new HashMap<Long,Node>();

        Pattern p = Pattern.compile(",");

        while (true) {
            String thisLine = br.readLine();
            if (thisLine == null) {
                return allNodes;
            }
            if (thisLine.startsWith("#")) {
                // Comment
            } else {
                String[] parts = p.split(thisLine,-1);

                if (parts.length >=4) {
                    long fromName = Long.parseLong(parts[1]);
                    Node fromNode = getOrCreate(allNodes, fromName);

                    long toName = Long.parseLong(parts[2]);
                    Node toNode = getOrCreate(allNodes, toName);

                    float distance = Float.parseFloat(parts[3]);
                    DirectedEdge de = new DirectedEdge(fromNode, toNode, distance);
                    fromNode.edgesFrom.add(de);
                    toNode.edgesTo.add(de);
                } else { // If none of our parsers match it...
                    throw new IOException("I can't understand the line " + thisLine);
                }
            }
        } // end of while loop
        
    }

    public static Node getOrCreate(HashMap<Long,Node> allNodes, long nodeId) {
        if (allNodes.containsKey(nodeId)) {
            return allNodes.get(nodeId);
        } else {
            Node n = new Node(nodeId);
            allNodes.put(nodeId, n);
            return n;
        }
    }

}
