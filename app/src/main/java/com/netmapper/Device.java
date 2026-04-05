package com.netmapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class representing a network device discovered during scan.
 */
public class Device {
    public String ip;
    public String mac;
    public String vendor;
    public String hostname;
    public String notes;
    public List<Integer> openPorts;
    public Map<Integer, String> banners;  // Port -> Banner
    public Map<Integer, String> services; // Port -> Service name
    public String sslCertInfo;            // SSL certificate info
    public long latencyMs;                // RTT in milliseconds
    public boolean saved;
    public long firstSeen;
    public String osGuess;                // OS fingerprint guess
    public List<String> mdnsServices;     // mDNS/Bonjour discovered services
    public String upnpInfo;               // UPnP device info
    public String snmpInfo;               // SNMP system info

    public Device(String ip) {
        this.ip = ip;
        this.mac = "";
        this.vendor = "Unknown";
        this.hostname = "";
        this.notes = "";
        this.openPorts = new ArrayList<>();
        this.banners = new HashMap<>();
        this.services = new HashMap<>();
        this.sslCertInfo = "";
        this.latencyMs = -1;
        this.saved = false;
        this.firstSeen = System.currentTimeMillis();
        this.osGuess = "";
        this.mdnsServices = new ArrayList<>();
        this.upnpInfo = "";
        this.snmpInfo = "";
    }
}
