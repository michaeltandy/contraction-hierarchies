
package uk.me.mjt.ch;

import java.util.*;


public class TurnRestriction {
    public enum TurnRestrictionType { NOT_ALLOWED, ONLY_ALLOWED }
    
    private final long turnRestrictionId;
    private final TurnRestrictionType type;
    private final List<Long> directedEdgeIds;
    
    public TurnRestriction(long turnRestrictionId, TurnRestrictionType type, List<Long> directedEdgeIds) {
        Preconditions.checkNoneNull(type, directedEdgeIds);
        this.turnRestrictionId = turnRestrictionId;
        this.type = type;
        this.directedEdgeIds = Collections.unmodifiableList(directedEdgeIds);
    }

    public long getTurnRestrictionId() {
        return turnRestrictionId;
    }

    public TurnRestrictionType getType() {
        return type;
    }

    public List<Long> getDirectedEdgeIds() {
        return directedEdgeIds;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (int) (this.turnRestrictionId ^ (this.turnRestrictionId >>> 32));
        hash = 41 * hash + Objects.hashCode(this.type);
        hash = 41 * hash + Objects.hashCode(this.directedEdgeIds);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TurnRestriction other = (TurnRestriction) obj;
        return this.turnRestrictionId == other.turnRestrictionId
                && this.type == other.type
                && Objects.equals(this.directedEdgeIds, other.directedEdgeIds);
    }
    
}
