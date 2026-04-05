package com.netmapper;

import java.util.HashMap;
import java.util.Map;

/**
 * OUI (Organizationally Unique Identifier) database for MAC address vendor lookup.
 * Also contains service name mappings for common ports.
 */
public class OuiDatabase {

    // OUI Database (60+ entries)
    private static final Map<String, String> OUI = new HashMap<>();
    static {
        // Raspberry Pi Foundation
        OUI.put("B8:27:EB", "Raspberry Pi");
        OUI.put("DC:A6:32", "Raspberry Pi");
        OUI.put("E4:5F:01", "Raspberry Pi");
        OUI.put("28:CD:C1", "Raspberry Pi");

        // Apple
        OUI.put("00:1C:B3", "Apple");
        OUI.put("00:1E:C2", "Apple");
        OUI.put("00:25:BC", "Apple");
        OUI.put("3C:06:30", "Apple");
        OUI.put("AC:DE:48", "Apple");
        OUI.put("F0:18:98", "Apple");
        OUI.put("14:7D:DA", "Apple");
        OUI.put("A8:BE:27", "Apple");

        // Google
        OUI.put("00:1A:11", "Google");
        OUI.put("54:60:09", "Google");
        OUI.put("F4:F5:D8", "Google");
        OUI.put("94:EB:2C", "Google");

        // TP-Link
        OUI.put("50:C7:BF", "TP-Link");
        OUI.put("54:E6:FC", "TP-Link");
        OUI.put("EC:08:6B", "TP-Link");
        OUI.put("14:CC:20", "TP-Link");
        OUI.put("C0:06:C3", "TP-Link");

        // Cisco
        OUI.put("00:00:0C", "Cisco");
        OUI.put("00:1B:D7", "Cisco");
        OUI.put("00:25:83", "Cisco");
        OUI.put("58:8D:09", "Cisco");

        // Ubiquiti
        OUI.put("04:18:D6", "Ubiquiti");
        OUI.put("24:A4:3C", "Ubiquiti");
        OUI.put("78:8A:20", "Ubiquiti");
        OUI.put("FC:EC:DA", "Ubiquiti");

        // Samsung
        OUI.put("00:21:19", "Samsung");
        OUI.put("00:24:91", "Samsung");
        OUI.put("08:37:3D", "Samsung");
        OUI.put("54:40:AD", "Samsung");
        OUI.put("AC:5F:3E", "Samsung");

        // Xiaomi
        OUI.put("28:6C:07", "Xiaomi");
        OUI.put("64:CC:2E", "Xiaomi");
        OUI.put("78:02:F8", "Xiaomi");
        OUI.put("FC:64:BA", "Xiaomi");

        // Espressif (ESP8266/ESP32)
        OUI.put("18:FE:34", "Espressif");
        OUI.put("24:0A:C4", "Espressif");
        OUI.put("5C:CF:7F", "Espressif");
        OUI.put("84:F3:EB", "Espressif");
        OUI.put("A4:CF:12", "Espressif");
        OUI.put("30:AE:A4", "Espressif");

        // Amazon
        OUI.put("00:FC:8B", "Amazon");
        OUI.put("40:B4:CD", "Amazon");
        OUI.put("68:54:FD", "Amazon");
        OUI.put("A0:02:DC", "Amazon");
        OUI.put("74:C2:46", "Amazon");

        // Huawei
        OUI.put("00:25:9E", "Huawei");
        OUI.put("00:66:4B", "Huawei");
        OUI.put("20:F3:A3", "Huawei");
        OUI.put("48:46:FB", "Huawei");
        OUI.put("88:53:2E", "Huawei");

        // D-Link
        OUI.put("00:1C:F0", "D-Link");
        OUI.put("00:26:5A", "D-Link");
        OUI.put("1C:7E:E5", "D-Link");
        OUI.put("28:10:7B", "D-Link");

        // Asus
        OUI.put("00:1F:C6", "Asus");
        OUI.put("10:BF:48", "Asus");
        OUI.put("2C:56:DC", "Asus");
        OUI.put("AC:22:0B", "Asus");

        // Netgear
        OUI.put("00:1E:2A", "Netgear");
        OUI.put("00:24:B2", "Netgear");
        OUI.put("20:4E:7F", "Netgear");
        OUI.put("44:94:FC", "Netgear");

        // Linksys
        OUI.put("00:1A:70", "Linksys");
        OUI.put("00:21:29", "Linksys");
        OUI.put("C0:C1:C0", "Linksys");

        // VMware
        OUI.put("00:0C:29", "VMware");
        OUI.put("00:50:56", "VMware");
        OUI.put("00:05:69", "VMware");

        // Intel
        OUI.put("00:1B:21", "Intel");
        OUI.put("00:1E:67", "Intel");
        OUI.put("3C:97:0E", "Intel");
        OUI.put("80:86:F2", "Intel");

        // Dell
        OUI.put("00:14:22", "Dell");
        OUI.put("00:1E:4F", "Dell");
        OUI.put("18:A9:9B", "Dell");
        OUI.put("34:17:EB", "Dell");

        // HP
        OUI.put("00:1C:C4", "HP");
        OUI.put("00:21:5A", "HP");
        OUI.put("10:60:4B", "HP");
        OUI.put("3C:D9:2B", "HP");

        // Synology
        OUI.put("00:11:32", "Synology");
        OUI.put("00:14:78", "Synology");

        // QNAP
        OUI.put("00:08:9B", "QNAP");
        OUI.put("24:5E:BE", "QNAP");

        // Philips Hue
        OUI.put("00:17:88", "Philips Hue");
        OUI.put("EC:B5:FA", "Philips Hue");

        // Roku
        OUI.put("08:05:81", "Roku");
        OUI.put("B0:A7:37", "Roku");
        OUI.put("D8:31:34", "Roku");

        // Sonos
        OUI.put("00:0E:58", "Sonos");
        OUI.put("5C:AA:FD", "Sonos");
        OUI.put("94:9F:3E", "Sonos");

        // OnePlus
        OUI.put("64:A2:F9", "OnePlus");
        OUI.put("94:65:2D", "OnePlus");
        OUI.put("C0:EE:FB", "OnePlus");

        // Nest Labs
        OUI.put("18:B4:30", "Nest Labs");
        OUI.put("64:16:66", "Nest Labs");

        // Realtek
        OUI.put("00:E0:4C", "Realtek");
        OUI.put("52:54:00", "Realtek");
    }

    // Service Names Database
    private static final Map<Integer, String> SERVICES = new HashMap<>();
    static {
        SERVICES.put(21, "FTP");
        SERVICES.put(22, "SSH");
        SERVICES.put(23, "Telnet");
        SERVICES.put(25, "SMTP");
        SERVICES.put(53, "DNS");
        SERVICES.put(80, "HTTP");
        SERVICES.put(110, "POP3");
        SERVICES.put(139, "NetBIOS");
        SERVICES.put(143, "IMAP");
        SERVICES.put(443, "HTTPS");
        SERVICES.put(445, "SMB");
        SERVICES.put(993, "IMAPS");
        SERVICES.put(995, "POP3S");
        SERVICES.put(1433, "MSSQL");
        SERVICES.put(1521, "Oracle");
        SERVICES.put(3306, "MySQL");
        SERVICES.put(3389, "RDP");
        SERVICES.put(5432, "PostgreSQL");
        SERVICES.put(5900, "VNC");
        SERVICES.put(6379, "Redis");
        SERVICES.put(8080, "HTTP-Alt");
        SERVICES.put(8443, "HTTPS-Alt");
        SERVICES.put(8888, "HTTP-Proxy");
        SERVICES.put(27017, "MongoDB");
    }

    /**
     * Look up vendor name by MAC address.
     * @param mac MAC address (at least 8 characters for OUI prefix)
     * @return Vendor name or "Unknown Vendor"
     */
    public static String getVendor(String mac) {
        if (mac == null || mac.length() < 8) return "Unknown";
        return OUI.getOrDefault(mac.toUpperCase().substring(0, 8), "Unknown Vendor");
    }

    /**
     * Get the number of OUI entries in the database.
     * @return Number of OUI entries
     */
    public static int getOuiCount() {
        return OUI.size();
    }

    /**
     * Look up service name by port number.
     * @param port Port number
     * @return Service name or "Unknown"
     */
    public static String getServiceName(int port) {
        return SERVICES.getOrDefault(port, "Unknown");
    }

    /**
     * Get the number of known services.
     * @return Number of service entries
     */
    public static int getServicesCount() {
        return SERVICES.size();
    }
}
