
package uk.me.mjt.ch.profile;

import java.io.InputStream;

public class SvgPlotter {
    private static final int MILLIS_IN_A_SECOND = 1000;
    private static final int MILLIS_IN_A_MINUTE = 60*MILLIS_IN_A_SECOND;
    private static final int MILLIS_IN_AN_HOUR = 60*MILLIS_IN_A_MINUTE;
    private static final int MILLIS_IN_A_DAY = 24*MILLIS_IN_AN_HOUR;
    
    public static String plot(ArrayTimeProfile... toPlot) {
        String template = loadTemplate();
        
        StringBuilder paths = new StringBuilder();
        
        for (ArrayTimeProfile atp : toPlot) {
            String d = profileFor(atp);
            paths.append("<path fill-opacity=\"0\" stroke=\"#555\" stroke-width=\"20\" d=\"").append(d).append("\" />\n");
        }
        
        return template.replace("<!-- PATHS_INSERTED_HERE -->", paths.toString());
    }
    
    private static String profileFor(ArrayTimeProfile toPlot) {
        StringBuilder sb = new StringBuilder();
        
        int previousY = -100;
        String previousString = "";
        
        for (int i=0 ; i<=MILLIS_IN_A_DAY ; i+=1*MILLIS_IN_A_MINUTE) {
            
            int thisY = toPlot.transitTimeAt(i);
            String s = String.format("%s %.0f %.0f ", i==0?"M":"L", i/1000.0, thisY/1000.0);
            
            if (thisY == previousY && i!=0 && i!=MILLIS_IN_A_DAY) {
                previousString = s;
            } else {
                sb.append(previousString);
                sb.append(s);
                previousString="";
                previousY=thisY;
            }
            
        }
        
        return sb.toString();
    }
    
    private static String loadTemplate() {
        InputStream is = SvgPlotter.class.getResourceAsStream("/base-time-profile-graph.svg");
        // From https://stackoverflow.com/a/5445161/1367431
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
