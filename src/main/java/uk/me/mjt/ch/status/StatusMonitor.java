package uk.me.mjt.ch.status;

public interface StatusMonitor {
    
    public void updateStatus(MonitoredProcess process, long completed, long total);
    
}
