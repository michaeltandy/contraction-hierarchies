
package uk.me.mjt.ch;

import java.util.List;
import uk.me.mjt.ch.DijkstraSolution;


public class UpAndDownPair {
    public final List<DijkstraSolution> up;
    public final List<DijkstraSolution> down;

    public UpAndDownPair(List<DijkstraSolution> up, List<DijkstraSolution> down) {
        this.up = up;
        this.down = down;
    }
}
