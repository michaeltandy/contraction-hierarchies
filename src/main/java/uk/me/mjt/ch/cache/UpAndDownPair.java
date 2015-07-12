
package uk.me.mjt.ch.cache;

import uk.me.mjt.ch.PartialSolution.DownwardSolution;
import uk.me.mjt.ch.PartialSolution.UpwardSolution;

public class UpAndDownPair {
    public final UpwardSolution up;
    public final DownwardSolution down;

    public UpAndDownPair(UpwardSolution up, DownwardSolution down) {
        this.up = up;
        this.down = down;
    }
}
