package uk.me.mjt.ch.loader;

import java.io.*;
import java.util.HashMap;
import java.util.regex.*;
import uk.me.mjt.ch.DirectedEdge;
import uk.me.mjt.ch.Node;

public class NodeLoadUWCS {

    private final static Pattern recordPattern = Pattern.compile("^(\\w+) ((\\w+:\\d+ ?)*)$");
    private final static Pattern endPattern = Pattern.compile("^END$");

    public static HashMap<String,Node> readData(BufferedReader br) throws IOException{
        HashMap<String,Node> allNodes = new HashMap<String,Node>();

        while (true) {
            String thisLine = br.readLine();
            if (thisLine == null) {
                return allNodes;
            }

            Matcher recordMatcher = recordPattern.matcher(thisLine);
            Matcher endMatcher = endPattern.matcher(thisLine);

            if (endMatcher.matches()) { // e.g. END
                return allNodes;

            } else if (recordMatcher.matches()) { // e.g. GT XH:5 TB:3 NA:7

                String fromName = recordMatcher.group(1);
                Node fromNode = getOrCreate(allNodes, fromName);

                String[] fromNames = recordMatcher.group(2).split("[ :]");
                if (fromNames.length >= 2) {
                    for (int i = 0; i < fromNames.length; i += 2) {
                        Node toNode = getOrCreate(allNodes, fromNames[i]);
                        float distance = Float.parseFloat(fromNames[i + 1]);
                        DirectedEdge de = new DirectedEdge(fromNode, toNode, distance);
                        fromNode.edgesFrom.add(de);
                        toNode.edgesTo.add(de);
                    }
                }

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
