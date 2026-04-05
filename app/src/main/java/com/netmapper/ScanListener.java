package com.netmapper;

import java.util.List;

/**
 * Callback interface for network scan events.
 */
public interface ScanListener {
    /**
     * Called when a host is discovered.
     * @param device The discovered device
     */
    void onHost(Device device);

    /**
     * Called to report scan progress.
     * @param done Number of hosts scanned
     * @param total Total number of hosts to scan
     */
    void onProgress(int done, int total);

    /**
     * Called when the scan is complete.
     * @param devices List of all discovered devices
     */
    void onDone(List<Device> devices);

    /**
     * Called for log messages.
     * @param message The log message
     */
    void onLog(String message);
}
