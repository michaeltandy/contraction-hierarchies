
package uk.me.mjt.ch;


public enum AccessOnly {
    NO_EDGES_YET(""),
    TRUE("1"),
    FALSE("0"),
    START_ONLY("10"),
    END_ONLY("01"),
    WITH_GAP("101");
    
    // Same as https://docs.google.com/spreadsheets/d/1TVNjNsfISTqy3QJrHaWlkLO4h8vWlqX5nY0Gy0KOw8k/edit#gid=1010316628
    private static final AccessOnly[][] lookupTable = new AccessOnly[AccessOnly.values().length][AccessOnly.values().length];

    static {
        AccessOnly[] values = AccessOnly.values();
        
        for (int i=0 ; i<values.length ; i++) {
            for (int j=0 ; j<values.length ; j++) {
                String fromTruth=values[i].truth;
                String toTruth=values[j].truth;
                String combinedTruth = fromTruth+toTruth;
                combinedTruth = combinedTruth.replaceAll("11", "1").replaceAll("00", "0");
                
                lookupTable[i][j] = findEntryForTruth(combinedTruth);
            }
        }
    }
    
    private final String truth;
    AccessOnly(String truth) {
        this.truth=truth;
    }
    
    private static AccessOnly findEntryForTruth(String truth) {
        for (int i = 0; i < AccessOnly.values().length; i++) {
            if (AccessOnly.values()[i].truth.equals(truth)) {
                return AccessOnly.values()[i];
            }
        }
        return null;
    }
    
    public boolean mayBeFollowedBy(AccessOnly that) {
        return (lookupTable[this.ordinal()][that.ordinal()] != null);
    }
    
    public AccessOnly followedBy(AccessOnly that) {
        AccessOnly result = lookupTable[this.ordinal()][that.ordinal()];
        if (result==null) {
            throw new IllegalArgumentException("Didn't know what to do with AccessOnly append for " + this + " and " + that);
        }
        return result;
    }
    
    public AccessOnly reverse() {
        if (this==END_ONLY)
            return START_ONLY;
        else if (this==START_ONLY) 
            return END_ONLY;
        else
            return this;
    }
}
