package uk.me.mjt.ch;

import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraphContractorTest {

    public GraphContractorTest() {
    }

    @Test
    public void testContractAll() {
        HashMap<Long,Node> graph = MakeTestData.makeLadder();
        
        System.out.println(GeoJson.allLinks(graph.values()));
        System.out.println("\n\n\n");
        
        GraphContractor instance = new GraphContractor(graph);
        instance.initialiseContractionOrder();
        instance.contractAll();
        
        Node startNode = graph.get(1L);
        Node endNode = graph.get(18L);
            
        DijkstraSolution contracted = Dijkstra.contractedGraphDijkstra(graph, startNode, endNode);
        System.out.println("Contraction: "+contracted);
    }

    

}