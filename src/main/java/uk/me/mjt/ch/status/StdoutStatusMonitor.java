
package uk.me.mjt.ch.status;


public class StdoutStatusMonitor implements StatusMonitor {

    @Override
    public void updateStatus(MonitoredProcess process, long completed, long total) {
        System.out.println(toString(process, completed, total));
    }
    
    public static String toString(MonitoredProcess process, long completed, long total) {
        if (total > 0) {
            return String.format("%s - %d/%d = %.2f%%", process.toString(), completed, total, completed*100.0/total );
        } else {
            return String.format("%s - %d/?", process.toString(), completed);
        }
    }
    
}
