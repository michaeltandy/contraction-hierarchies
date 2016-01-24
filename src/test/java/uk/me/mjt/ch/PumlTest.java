
package uk.me.mjt.ch;

import java.util.Collection;
import org.junit.Test;
import static org.junit.Assert.*;

public class PumlTest {

    public PumlTest() {
    }

    @Test
    public void testForNodes() {
        System.out.println("forNodes");
        MapData a = MakeTestData.makeSimpleThreeEntry();
        
        String result = Puml.forNodes(a.getAllNodes());
        System.out.println(result);
        
        for (int i=1 ; i<=3 ; i++) {
            assertContains("(Node "+i+")", result);
            assertContains("Edge 100"+(i-1), result);
        }
        
        assertTrue(result.startsWith("@startuml"));
        assertTrue(result.endsWith("@enduml"));
    }
    
    private void assertContains(String substring, String toCheck) {
        assertTrue(toCheck.contains(substring));
    }

}