package uk.me.mjt.ch.loader;

import java.io.*;
import java.util.HashMap;
import java.util.regex.*;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.Node;

public class NodeLoadCsv {

    // Format is like:
    // #From node id,From node contractable?,From Lat,From lon,To node id,To node contractable?,To lat,To lon,Distance between from and to nodes
    // 674981,Y,51.778037,-0.221497,2851894541,Y,51.777995,-0.221667,12.577
    
    public static HashMap<Long,Node> readData(BufferedReader br) throws IOException{
        HashMap<Long,Node> allNodes = new HashMap<Long,Node>();

        Pattern p = Pattern.compile(",");
        long edgeId = 1;

        while (true) {
            String thisLine = br.readLine();
            if (thisLine == null) {
                return allNodes;
            }
            if (thisLine.startsWith("#")) {
                // Comment
            } else {
                String[] parts = p.split(thisLine,-1);

                if (parts.length ==9) {
                    long fromName = Long.parseLong(parts[0]);
                    boolean fromContractable = "Y".equalsIgnoreCase(parts[1]);
                    float fromLat = Float.parseFloat(parts[2]);
                    float fromLon = Float.parseFloat(parts[3]);
                    Node fromNode = getOrCreate(allNodes, fromName, fromLat, fromLon, fromContractable);

                    long toName = Long.parseLong(parts[4]);
                    boolean toContractable = "Y".equalsIgnoreCase(parts[5]);
                    float toLat = Float.parseFloat(parts[6]);
                    float toLon = Float.parseFloat(parts[7]);
                    Node toNode = getOrCreate(allNodes, toName, toLat, toLon, toContractable);

                    float distance = Float.parseFloat(parts[8]);
                    DirectedEdge de = new DirectedEdge(edgeId++, fromNode, toNode, distance);
                    fromNode.edgesFrom.add(de);
                    toNode.edgesTo.add(de);
                } else { // If none of our parsers match it...
                    throw new IOException("I can't understand the line " + thisLine);
                }
            }
        } // end of while loop
        
    }

    public static Node getOrCreate(HashMap<Long,Node> allNodes, long nodeId, float lat, float lon, boolean contractAllowed) {
        if (allNodes.containsKey(nodeId)) {
            Node n = allNodes.get(nodeId);
            n.contractionAllowed = n.contractionAllowed&&contractAllowed;
            return n;
        } else {
            Node n = new Node(nodeId,lat,lon);
            n.contractionAllowed = contractAllowed;
            allNodes.put(nodeId, n);
            return n;
        }
    }

}
