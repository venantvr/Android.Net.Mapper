package com.netmapper;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    // ============================================================
    // OUI Database (60+ entries)
    // ============================================================
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

    // ============================================================
    // Service Names Database
    // ============================================================
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

    public static String oui(String mac) {
        if (mac == null || mac.length() < 8) return "Unknown";
        return OUI.getOrDefault(mac.toUpperCase().substring(0, 8), "Unknown Vendor");
    }

    public static int ouiCount() {
        return OUI.size();
    }

    public static String serviceName(int port) {
        return SERVICES.getOrDefault(port, "Unknown");
    }

    // ============================================================
    // Device Model (Enhanced)
    // ============================================================
    public static class Device {
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

    // ============================================================
    // Scan History Database
    // ============================================================
    public static class ScanDbHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "netmapper.db";
        private static final int DB_VERSION = 1;

        public ScanDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE scans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp INTEGER," +
                    "subnet TEXT," +
                    "profile TEXT," +
                    "hosts_count INTEGER," +
                    "ports_count INTEGER," +
                    "duration_ms INTEGER)");

            db.execSQL("CREATE TABLE devices (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "scan_id INTEGER," +
                    "ip TEXT," +
                    "mac TEXT," +
                    "vendor TEXT," +
                    "hostname TEXT," +
                    "ports TEXT," +
                    "latency_ms INTEGER," +
                    "banners TEXT," +
                    "FOREIGN KEY(scan_id) REFERENCES scans(id))");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS devices");
            db.execSQL("DROP TABLE IF EXISTS scans");
            onCreate(db);
        }
    }

    // ============================================================
    // Scanner (Enhanced with diagnostics)
    // ============================================================
    public interface ScanListener {
        void onHost(Device device);
        void onProgress(int done, int total);
        void onDone(List<Device> devices);
        void onLog(String message);
    }

    public static class Scanner {
        private static final int[] PORTS_PING = {};
        private static final int[] PORTS_QUICK = {22, 80, 443, 8080, 3389, 445};
        private static final int[] PORTS_FULL = {21, 22, 23, 25, 53, 80, 110, 139, 143, 443, 445,
            993, 995, 1433, 3306, 3389, 5432, 5900, 6379, 8080, 8443, 8888, 27017};

        public static final int PROFILE_PING = 0;
        public static final int PROFILE_QUICK = 1;
        public static final int PROFILE_FULL = 2;

        private volatile boolean running = false;
        private ExecutorService pool;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final List<Device> devices = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger completed = new AtomicInteger(0);
        private long startTime;

        public void scan(String subnet, int profile, boolean grabBanners, boolean doDiscovery, ScanListener listener) {
            if (running) return;
            running = true;
            devices.clear();
            completed.set(0);
            startTime = System.currentTimeMillis();

            int[] ports;
            switch (profile) {
                case PROFILE_PING: ports = PORTS_PING; break;
                case PROFILE_FULL: ports = PORTS_FULL; break;
                default: ports = PORTS_QUICK; break;
            }

            pool = Executors.newFixedThreadPool(64);

            for (int i = 1; i <= 254; i++) {
                final String ip = subnet + "." + i;
                final int[] scanPorts = ports;
                final boolean doBanners = grabBanners;
                final boolean runDiscovery = doDiscovery;

                pool.submit(() -> {
                    if (!running) return;

                    try {
                        Device device = new Device(ip);
                        boolean alive = false;

                        // Measure latency first
                        device.latencyMs = measureLatency(ip);
                        if (device.latencyMs > 0) {
                            alive = true;
                        }

                        // Port scanning with banner grabbing
                        for (int port : scanPorts) {
                            if (!running) return;
                            try (Socket socket = new Socket()) {
                                long portStart = System.currentTimeMillis();
                                socket.connect(new InetSocketAddress(ip, port), 500);
                                device.openPorts.add(port);
                                device.services.put(port, serviceName(port));
                                alive = true;

                                // Banner grabbing
                                if (doBanners) {
                                    String banner = grabBanner(socket, ip, port);
                                    if (!banner.isEmpty()) {
                                        device.banners.put(port, banner);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        // ICMP check if no ports found
                        if (!alive) {
                            try {
                                InetAddress addr = InetAddress.getByName(ip);
                                alive = addr.isReachable(500);
                            } catch (Exception ignored) {}
                        }

                        if (alive) {
                            // Reverse DNS
                            try {
                                InetAddress addr = InetAddress.getByName(ip);
                                String hostname = addr.getCanonicalHostName();
                                if (!hostname.equals(ip)) {
                                    device.hostname = hostname;
                                }
                            } catch (Exception ignored) {}

                            // SSL Certificate info
                            if (device.openPorts.contains(443)) {
                                device.sslCertInfo = getSSLCertInfo(ip, 443);
                            } else if (device.openPorts.contains(8443)) {
                                device.sslCertInfo = getSSLCertInfo(ip, 8443);
                            }

                            // OS Fingerprinting guess
                            device.osGuess = guessOS(device);

                            // Discovery protocols (if enabled)
                            if (runDiscovery) {
                                // mDNS/Bonjour discovery
                                try {
                                    device.mdnsServices = MdnsDiscovery.discoverServices(ip, 500);
                                } catch (Exception ignored) {}

                                // UPnP discovery
                                try {
                                    device.upnpInfo = UpnpDiscovery.discoverDevice(ip, 500);
                                } catch (Exception ignored) {}

                                // SNMP discovery
                                try {
                                    device.snmpInfo = SnmpDiscovery.queryDevice(ip, 300);
                                } catch (Exception ignored) {}
                            }

                            devices.add(device);
                            handler.post(() -> {
                                if (listener != null && running) {
                                    listener.onHost(device);
                                }
                            });
                        }
                    } catch (Exception ignored) {}

                    int done = completed.incrementAndGet();
                    handler.post(() -> {
                        if (listener != null && running) {
                            listener.onProgress(done, 254);
                        }
                    });
                });
            }

            // Completion handler
            new Thread(() -> {
                pool.shutdown();
                try {
                    pool.awaitTermination(5, TimeUnit.MINUTES);
                } catch (InterruptedException ignored) {}

                if (running) {
                    // ARP enrichment
                    enrichWithArp();

                    handler.post(() -> {
                        if (listener != null) {
                            listener.onDone(new ArrayList<>(devices));
                        }
                    });
                }
                running = false;
            }).start();
        }

        private long measureLatency(String ip) {
            try {
                InetAddress addr = InetAddress.getByName(ip);
                long start = System.currentTimeMillis();
                if (addr.isReachable(1000)) {
                    return System.currentTimeMillis() - start;
                }
            } catch (Exception ignored) {}
            return -1;
        }

        private String grabBanner(Socket socket, String ip, int port) {
            try {
                socket.setSoTimeout(1000);

                // HTTP banner
                if (port == 80 || port == 8080 || port == 8888) {
                    OutputStream os = socket.getOutputStream();
                    os.write(("GET / HTTP/1.0\r\nHost: " + ip + "\r\n\r\n").getBytes());
                    os.flush();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    StringBuilder banner = new StringBuilder();
                    String line;
                    int lines = 0;
                    while ((line = reader.readLine()) != null && lines < 5) {
                        banner.append(line).append("\n");
                        lines++;
                        if (line.isEmpty()) break;
                    }
                    return extractServerHeader(banner.toString());
                }

                // SSH banner
                if (port == 22) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    String banner = reader.readLine();
                    return banner != null ? banner.trim() : "";
                }

                // FTP banner
                if (port == 21) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    String banner = reader.readLine();
                    return banner != null ? banner.trim() : "";
                }

                // SMTP banner
                if (port == 25) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    String banner = reader.readLine();
                    return banner != null ? banner.trim() : "";
                }

            } catch (Exception ignored) {}
            return "";
        }

        private String extractServerHeader(String response) {
            for (String line : response.split("\n")) {
                if (line.toLowerCase().startsWith("server:")) {
                    return line.substring(7).trim();
                }
            }
            // Return first line (HTTP status) if no server header
            String[] lines = response.split("\n");
            return lines.length > 0 ? lines[0].trim() : "";
        }

        private String getSSLCertInfo(String ip, int port) {
            try {
                TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String t) {}
                        public void checkServerTrusted(X509Certificate[] certs, String t) {}
                    }
                };

                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAll, new java.security.SecureRandom());
                SSLSocketFactory factory = sc.getSocketFactory();

                SSLSocket socket = (SSLSocket) factory.createSocket();
                socket.connect(new InetSocketAddress(ip, port), 2000);
                socket.setSoTimeout(2000);
                socket.startHandshake();

                SSLSession session = socket.getSession();
                java.security.cert.Certificate[] certs = session.getPeerCertificates();

                if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) certs[0];
                    String cn = x509.getSubjectX500Principal().getName();
                    // Extract CN
                    for (String part : cn.split(",")) {
                        if (part.trim().startsWith("CN=")) {
                            return part.trim().substring(3);
                        }
                    }
                    return cn;
                }
                socket.close();
            } catch (Exception ignored) {}
            return "";
        }

        private String guessOS(Device device) {
            // Simple OS fingerprinting based on open ports and banners
            boolean hasRdp = device.openPorts.contains(3389);
            boolean hasSmb = device.openPorts.contains(445);
            boolean hasNetbios = device.openPorts.contains(139);
            boolean hasSsh = device.openPorts.contains(22);

            String sshBanner = device.banners.getOrDefault(22, "").toLowerCase();
            String httpBanner = device.banners.getOrDefault(80, "").toLowerCase();

            if (hasRdp) return "Windows";
            if (sshBanner.contains("ubuntu")) return "Ubuntu Linux";
            if (sshBanner.contains("debian")) return "Debian Linux";
            if (sshBanner.contains("raspbian") || device.vendor.contains("Raspberry")) return "Raspberry Pi OS";
            if (httpBanner.contains("iis")) return "Windows Server";
            if (httpBanner.contains("apache")) return "Linux (Apache)";
            if (httpBanner.contains("nginx")) return "Linux (nginx)";
            if (hasSsh && !hasSmb && !hasRdp) return "Linux/Unix";
            if (hasSmb && hasNetbios && !hasSsh) return "Windows";

            return "";
        }

        private void enrichWithArp() {
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
                String line;
                reader.readLine(); // Skip header
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        String ip = parts[0];
                        String mac = parts[3].toUpperCase();
                        if (mac.startsWith("00:00:00")) continue;

                        for (Device d : devices) {
                            if (d.ip.equals(ip)) {
                                d.mac = mac;
                                d.vendor = oui(mac);
                                break;
                            }
                        }
                    }
                }
                reader.close();
            } catch (Exception ignored) {}
        }

        public long getScanDuration() {
            return System.currentTimeMillis() - startTime;
        }

        public void cancel() {
            running = false;
            if (pool != null) {
                pool.shutdownNow();
            }
        }

        public boolean isRunning() {
            return running;
        }
    }

    // ============================================================
    // Network Tools
    // ============================================================
    public static class NetTools {

        // Wake-on-LAN
        public static boolean sendWoL(String macAddress, String broadcastIp) {
            try {
                byte[] macBytes = parseMac(macAddress);
                byte[] packet = new byte[6 + 16 * 6];

                // 6 bytes of 0xFF
                for (int i = 0; i < 6; i++) {
                    packet[i] = (byte) 0xFF;
                }

                // 16 repetitions of MAC address
                for (int i = 0; i < 16; i++) {
                    System.arraycopy(macBytes, 0, packet, 6 + i * 6, 6);
                }

                InetAddress address = InetAddress.getByName(broadcastIp);
                DatagramPacket dgram = new DatagramPacket(packet, packet.length, address, 9);
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.send(dgram);
                socket.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private static byte[] parseMac(String mac) {
            String[] parts = mac.split(":");
            byte[] bytes = new byte[6];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(parts[i], 16);
            }
            return bytes;
        }

        // Traceroute (simplified - uses ping TTL)
        public static List<String> traceroute(String host, int maxHops) {
            List<String> hops = new ArrayList<>();
            try {
                for (int ttl = 1; ttl <= maxHops; ttl++) {
                    String[] cmd = {"ping", "-c", "1", "-t", String.valueOf(ttl), "-W", "1", host};
                    Process proc = Runtime.getRuntime().exec(cmd);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(proc.getInputStream()));
                    BufferedReader errReader = new BufferedReader(
                            new InputStreamReader(proc.getErrorStream()));

                    String line;
                    String hop = ttl + ": *";
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("From") || line.contains("from")) {
                            // Extract IP from response
                            int start = line.indexOf("(");
                            int end = line.indexOf(")");
                            if (start > 0 && end > start) {
                                hop = ttl + ": " + line.substring(start + 1, end);
                            } else {
                                // Try to extract IP differently
                                String[] parts = line.split("\\s+");
                                for (String part : parts) {
                                    if (part.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                        hop = ttl + ": " + part;
                                        break;
                                    }
                                }
                            }
                        } else if (line.contains("bytes from")) {
                            // Reached destination
                            int start = line.indexOf("from") + 5;
                            int end = line.indexOf(":", start);
                            if (end > start) {
                                hop = ttl + ": " + line.substring(start, end).trim() + " (destination)";
                            }
                            hops.add(hop);
                            return hops;
                        }
                    }
                    reader.close();
                    errReader.close();
                    proc.waitFor();

                    hops.add(hop);
                }
            } catch (Exception e) {
                hops.add("Error: " + e.getMessage());
            }
            return hops;
        }

        // Ping with statistics
        public static String ping(String host, int count) {
            try {
                String[] cmd = {"ping", "-c", String.valueOf(count), "-W", "1", host};
                Process proc = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                reader.close();
                proc.waitFor();

                return result.toString();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
    }

    // ============================================================
    // mDNS/Bonjour Discovery
    // ============================================================
    public static class MdnsDiscovery {
        private static final String MDNS_ADDR = "224.0.0.251";
        private static final int MDNS_PORT = 5353;

        // Common mDNS service types
        private static final String[] SERVICE_TYPES = {
            "_http._tcp.local",
            "_https._tcp.local",
            "_ssh._tcp.local",
            "_sftp-ssh._tcp.local",
            "_smb._tcp.local",
            "_afpovertcp._tcp.local",
            "_nfs._tcp.local",
            "_ftp._tcp.local",
            "_ipp._tcp.local",
            "_printer._tcp.local",
            "_airplay._tcp.local",
            "_raop._tcp.local",
            "_googlecast._tcp.local",
            "_spotify-connect._tcp.local",
            "_homekit._tcp.local",
            "_hap._tcp.local",
            "_workstation._tcp.local",
            "_device-info._tcp.local"
        };

        public static List<String> discoverServices(String targetIp, int timeoutMs) {
            List<String> services = new ArrayList<>();

            for (String serviceType : SERVICE_TYPES) {
                try {
                    byte[] query = buildMdnsQuery(serviceType);
                    DatagramSocket socket = new DatagramSocket();
                    socket.setSoTimeout(timeoutMs / SERVICE_TYPES.length);
                    socket.setBroadcast(true);

                    InetAddress mdnsAddr = InetAddress.getByName(MDNS_ADDR);
                    DatagramPacket sendPacket = new DatagramPacket(query, query.length, mdnsAddr, MDNS_PORT);
                    socket.send(sendPacket);

                    byte[] buffer = new byte[1500];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                    try {
                        socket.receive(receivePacket);
                        String responderIp = receivePacket.getAddress().getHostAddress();

                        // Check if response is from the target IP
                        if (responderIp.equals(targetIp)) {
                            String serviceName = extractServiceName(serviceType);
                            if (!services.contains(serviceName)) {
                                services.add(serviceName);
                            }
                        }
                    } catch (Exception ignored) {
                        // Timeout or no response
                    }

                    socket.close();
                } catch (Exception ignored) {}
            }

            return services;
        }

        public static Map<String, List<String>> discoverAllServices(int timeoutMs) {
            Map<String, List<String>> results = new HashMap<>();

            for (String serviceType : SERVICE_TYPES) {
                try {
                    byte[] query = buildMdnsQuery(serviceType);
                    DatagramSocket socket = new DatagramSocket();
                    socket.setSoTimeout(200);
                    socket.setBroadcast(true);

                    InetAddress mdnsAddr = InetAddress.getByName(MDNS_ADDR);
                    DatagramPacket sendPacket = new DatagramPacket(query, query.length, mdnsAddr, MDNS_PORT);
                    socket.send(sendPacket);

                    byte[] buffer = new byte[1500];
                    long endTime = System.currentTimeMillis() + 500;

                    while (System.currentTimeMillis() < endTime) {
                        try {
                            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                            socket.receive(receivePacket);
                            String responderIp = receivePacket.getAddress().getHostAddress();
                            String serviceName = extractServiceName(serviceType);

                            if (!results.containsKey(responderIp)) {
                                results.put(responderIp, new ArrayList<>());
                            }
                            if (!results.get(responderIp).contains(serviceName)) {
                                results.get(responderIp).add(serviceName);
                            }
                        } catch (Exception e) {
                            break;
                        }
                    }

                    socket.close();
                } catch (Exception ignored) {}
            }

            return results;
        }

        private static byte[] buildMdnsQuery(String name) {
            // Simple DNS query packet
            byte[] query = new byte[512];
            int pos = 0;

            // Transaction ID (random)
            query[pos++] = 0x00;
            query[pos++] = 0x00;

            // Flags: standard query
            query[pos++] = 0x00;
            query[pos++] = 0x00;

            // Questions: 1
            query[pos++] = 0x00;
            query[pos++] = 0x01;

            // Answer RRs: 0
            query[pos++] = 0x00;
            query[pos++] = 0x00;

            // Authority RRs: 0
            query[pos++] = 0x00;
            query[pos++] = 0x00;

            // Additional RRs: 0
            query[pos++] = 0x00;
            query[pos++] = 0x00;

            // QNAME (domain name)
            String[] labels = name.split("\\.");
            for (String label : labels) {
                query[pos++] = (byte) label.length();
                for (char c : label.toCharArray()) {
                    query[pos++] = (byte) c;
                }
            }
            query[pos++] = 0x00; // End of name

            // QTYPE: PTR (12)
            query[pos++] = 0x00;
            query[pos++] = 0x0C;

            // QCLASS: IN (1) with cache flush bit
            query[pos++] = 0x00;
            query[pos++] = 0x01;

            byte[] result = new byte[pos];
            System.arraycopy(query, 0, result, 0, pos);
            return result;
        }

        private static String extractServiceName(String serviceType) {
            // Convert _http._tcp.local to HTTP
            if (serviceType.startsWith("_") && serviceType.contains("._")) {
                String name = serviceType.substring(1, serviceType.indexOf("._"));
                return name.toUpperCase().replace("-", " ");
            }
            return serviceType;
        }
    }

    // ============================================================
    // UPnP Discovery (SSDP)
    // ============================================================
    public static class UpnpDiscovery {
        private static final String SSDP_ADDR = "239.255.255.250";
        private static final int SSDP_PORT = 1900;

        private static final String SSDP_MSEARCH =
            "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 2\r\n" +
            "ST: ssdp:all\r\n" +
            "\r\n";

        public static String discoverDevice(String targetIp, int timeoutMs) {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(timeoutMs);
                socket.setBroadcast(true);

                InetAddress ssdpAddr = InetAddress.getByName(SSDP_ADDR);
                byte[] queryBytes = SSDP_MSEARCH.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(queryBytes, queryBytes.length, ssdpAddr, SSDP_PORT);
                socket.send(sendPacket);

                byte[] buffer = new byte[2048];
                StringBuilder deviceInfo = new StringBuilder();
                long endTime = System.currentTimeMillis() + timeoutMs;

                while (System.currentTimeMillis() < endTime) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(receivePacket);
                        String responderIp = receivePacket.getAddress().getHostAddress();

                        if (responderIp.equals(targetIp)) {
                            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                            String info = parseUpnpResponse(response);
                            if (!info.isEmpty() && !deviceInfo.toString().contains(info)) {
                                if (deviceInfo.length() > 0) deviceInfo.append("\n");
                                deviceInfo.append(info);
                            }
                        }
                    } catch (Exception e) {
                        break;
                    }
                }

                socket.close();
                return deviceInfo.toString();
            } catch (Exception e) {
                return "";
            }
        }

        public static Map<String, String> discoverAllDevices(int timeoutMs) {
            Map<String, String> results = new HashMap<>();

            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(500);
                socket.setBroadcast(true);

                InetAddress ssdpAddr = InetAddress.getByName(SSDP_ADDR);
                byte[] queryBytes = SSDP_MSEARCH.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(queryBytes, queryBytes.length, ssdpAddr, SSDP_PORT);
                socket.send(sendPacket);

                byte[] buffer = new byte[2048];
                long endTime = System.currentTimeMillis() + timeoutMs;

                while (System.currentTimeMillis() < endTime) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(receivePacket);
                        String responderIp = receivePacket.getAddress().getHostAddress();
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        String info = parseUpnpResponse(response);

                        if (!info.isEmpty()) {
                            if (results.containsKey(responderIp)) {
                                String existing = results.get(responderIp);
                                if (!existing.contains(info)) {
                                    results.put(responderIp, existing + "\n" + info);
                                }
                            } else {
                                results.put(responderIp, info);
                            }
                        }
                    } catch (Exception e) {
                        break;
                    }
                }

                socket.close();
            } catch (Exception ignored) {}

            return results;
        }

        private static String parseUpnpResponse(String response) {
            StringBuilder info = new StringBuilder();

            // Extract SERVER header
            String server = extractHeader(response, "SERVER");
            if (server != null && !server.isEmpty()) {
                info.append(server);
            }

            // Extract ST (service type)
            String st = extractHeader(response, "ST");
            if (st != null && !st.isEmpty()) {
                String deviceType = parseDeviceType(st);
                if (!deviceType.isEmpty()) {
                    if (info.length() > 0) info.append(" | ");
                    info.append(deviceType);
                }
            }

            // Extract USN
            String usn = extractHeader(response, "USN");
            if (usn != null && usn.contains("uuid:")) {
                int start = usn.indexOf("uuid:") + 5;
                int end = usn.indexOf("::", start);
                if (end == -1) end = usn.length();
                String uuid = usn.substring(start, Math.min(end, start + 8));
                if (info.length() > 0) info.append(" ");
                info.append("[").append(uuid).append("]");
            }

            return info.toString();
        }

        private static String extractHeader(String response, String header) {
            String[] lines = response.split("\r\n");
            for (String line : lines) {
                if (line.toUpperCase().startsWith(header.toUpperCase() + ":")) {
                    return line.substring(header.length() + 1).trim();
                }
            }
            return null;
        }

        private static String parseDeviceType(String st) {
            if (st.contains("MediaRenderer")) return "Media Renderer";
            if (st.contains("MediaServer")) return "Media Server";
            if (st.contains("InternetGatewayDevice")) return "Gateway";
            if (st.contains("WANDevice")) return "WAN Device";
            if (st.contains("WANConnection")) return "WAN Connection";
            if (st.contains("Basic")) return "Basic Device";
            if (st.contains("rootdevice")) return "Root Device";
            return "";
        }
    }

    // ============================================================
    // SNMP Discovery
    // ============================================================
    public static class SnmpDiscovery {
        private static final int SNMP_PORT = 161;
        private static final String[] COMMUNITIES = {"public", "private"};

        // Common SNMP OIDs
        private static final byte[] OID_SYS_DESCR = {0x2B, 0x06, 0x01, 0x02, 0x01, 0x01, 0x01, 0x00}; // 1.3.6.1.2.1.1.1.0
        private static final byte[] OID_SYS_NAME = {0x2B, 0x06, 0x01, 0x02, 0x01, 0x01, 0x05, 0x00};  // 1.3.6.1.2.1.1.5.0

        public static String queryDevice(String ip, int timeoutMs) {
            StringBuilder result = new StringBuilder();

            for (String community : COMMUNITIES) {
                try {
                    // Query sysName
                    String sysName = snmpGet(ip, community, OID_SYS_NAME, timeoutMs);
                    if (sysName != null && !sysName.isEmpty()) {
                        result.append("Name: ").append(sysName);

                        // Query sysDescr
                        String sysDescr = snmpGet(ip, community, OID_SYS_DESCR, timeoutMs);
                        if (sysDescr != null && !sysDescr.isEmpty()) {
                            result.append("\nDescr: ").append(sysDescr);
                        }

                        result.append("\nCommunity: ").append(community);
                        return result.toString();
                    }
                } catch (Exception ignored) {}
            }

            return "";
        }

        public static Map<String, String> scanNetwork(String subnet, int timeoutMs) {
            Map<String, String> results = new HashMap<>();
            ExecutorService pool = Executors.newFixedThreadPool(32);

            for (int i = 1; i <= 254; i++) {
                final String ip = subnet + "." + i;
                pool.submit(() -> {
                    String info = queryDevice(ip, timeoutMs);
                    if (!info.isEmpty()) {
                        synchronized (results) {
                            results.put(ip, info);
                        }
                    }
                });
            }

            pool.shutdown();
            try {
                pool.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            return results;
        }

        private static String snmpGet(String ip, String community, byte[] oid, int timeoutMs) {
            try {
                byte[] packet = buildSnmpGetRequest(community, oid);

                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(timeoutMs);

                InetAddress address = InetAddress.getByName(ip);
                DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, address, SNMP_PORT);
                socket.send(sendPacket);

                byte[] buffer = new byte[1500];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);

                socket.close();

                return parseSnmpResponse(receivePacket.getData(), receivePacket.getLength());
            } catch (Exception e) {
                return null;
            }
        }

        private static byte[] buildSnmpGetRequest(String community, byte[] oid) {
            // Build SNMP v1 GET request
            byte[] communityBytes = community.getBytes();

            // Variable bindings (just the OID with null value)
            int varbindLen = oid.length + 4; // OID + null type + null length
            int varbindListLen = varbindLen + 2;

            // PDU
            int pduLen = varbindListLen + 2 + 12; // varbind list + sequence header + request-id + error stuff

            // Message
            int messageLen = pduLen + 2 + communityBytes.length + 2 + 3; // pdu + header + community + header + version

            byte[] packet = new byte[messageLen + 2];
            int pos = 0;

            // SEQUENCE (message)
            packet[pos++] = 0x30;
            packet[pos++] = (byte) messageLen;

            // Version (SNMPv1 = 0)
            packet[pos++] = 0x02; // INTEGER
            packet[pos++] = 0x01; // length
            packet[pos++] = 0x00; // value

            // Community string
            packet[pos++] = 0x04; // OCTET STRING
            packet[pos++] = (byte) communityBytes.length;
            System.arraycopy(communityBytes, 0, packet, pos, communityBytes.length);
            pos += communityBytes.length;

            // GetRequest PDU
            packet[pos++] = (byte) 0xA0; // GET-REQUEST
            packet[pos++] = (byte) pduLen;

            // Request ID
            packet[pos++] = 0x02; // INTEGER
            packet[pos++] = 0x04; // length
            packet[pos++] = 0x00;
            packet[pos++] = 0x00;
            packet[pos++] = 0x00;
            packet[pos++] = 0x01;

            // Error status
            packet[pos++] = 0x02;
            packet[pos++] = 0x01;
            packet[pos++] = 0x00;

            // Error index
            packet[pos++] = 0x02;
            packet[pos++] = 0x01;
            packet[pos++] = 0x00;

            // Variable bindings SEQUENCE
            packet[pos++] = 0x30;
            packet[pos++] = (byte) varbindListLen;

            // VarBind SEQUENCE
            packet[pos++] = 0x30;
            packet[pos++] = (byte) varbindLen;

            // OID
            packet[pos++] = 0x06; // OBJECT IDENTIFIER
            packet[pos++] = (byte) oid.length;
            System.arraycopy(oid, 0, packet, pos, oid.length);
            pos += oid.length;

            // NULL value
            packet[pos++] = 0x05;
            packet[pos++] = 0x00;

            byte[] result = new byte[pos];
            System.arraycopy(packet, 0, result, 0, pos);
            return result;
        }

        private static String parseSnmpResponse(byte[] data, int length) {
            try {
                // Simple parser - look for OCTET STRING values
                for (int i = 0; i < length - 2; i++) {
                    if (data[i] == 0x04) { // OCTET STRING
                        int strLen = data[i + 1] & 0xFF;
                        if (strLen > 0 && strLen < 200 && i + 2 + strLen <= length) {
                            String value = new String(data, i + 2, strLen);
                            // Filter out non-printable strings
                            if (isPrintable(value) && value.length() > 1) {
                                return value.trim();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            return null;
        }

        private static boolean isPrintable(String s) {
            for (char c : s.toCharArray()) {
                if (c < 32 || c > 126) {
                    if (c != '\n' && c != '\r' && c != '\t') {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    // ============================================================
    // Device Adapter (Enhanced)
    // ============================================================
    public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
        private final List<Device> all = new ArrayList<>();
        private final List<Device> shown = new ArrayList<>();
        private String filter = "";
        private OnDeviceClickListener clickListener;

        public interface OnDeviceClickListener {
            void onDeviceClick(Device device);
        }

        public void setOnDeviceClickListener(OnDeviceClickListener listener) {
            this.clickListener = listener;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView tvIp, tvMac, tvVendor, tvHost, tvPorts, tvLatency, tvOs;
            ImageButton ibStar;

            ViewHolder(View v) {
                super(v);
                card = v.findViewById(R.id.cardDevice);
                tvIp = v.findViewById(R.id.tvIp);
                tvMac = v.findViewById(R.id.tvMac);
                tvVendor = v.findViewById(R.id.tvVendor);
                tvHost = v.findViewById(R.id.tvHost);
                tvPorts = v.findViewById(R.id.tvPorts);
                tvLatency = v.findViewById(R.id.tvLatency);
                tvOs = v.findViewById(R.id.tvOs);
                ibStar = v.findViewById(R.id.ibStar);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_device, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            Device d = shown.get(position);
            h.tvIp.setText(d.ip);
            h.tvMac.setText(d.mac.isEmpty() ? "--:--:--:--:--:--" : d.mac);
            h.tvVendor.setText(d.vendor);

            if (d.hostname != null && !d.hostname.isEmpty()) {
                h.tvHost.setText(d.hostname);
                h.tvHost.setVisibility(View.VISIBLE);
            } else {
                h.tvHost.setVisibility(View.GONE);
            }

            // Ports with service names
            if (d.openPorts.isEmpty()) {
                h.tvPorts.setText("Aucun port ouvert");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < d.openPorts.size(); i++) {
                    if (i > 0) sb.append(", ");
                    int port = d.openPorts.get(i);
                    String svc = d.services.get(port);
                    sb.append(port);
                    if (svc != null && !svc.equals("Unknown")) {
                        sb.append("/").append(svc);
                    }
                }
                h.tvPorts.setText(sb.toString());
            }

            // Latency
            if (h.tvLatency != null) {
                if (d.latencyMs > 0) {
                    h.tvLatency.setText("⏱ " + d.latencyMs + "ms");
                    h.tvLatency.setVisibility(View.VISIBLE);
                } else {
                    h.tvLatency.setVisibility(View.GONE);
                }
            }

            // OS guess
            if (h.tvOs != null) {
                if (!d.osGuess.isEmpty()) {
                    h.tvOs.setText("🖥 " + d.osGuess);
                    h.tvOs.setVisibility(View.VISIBLE);
                } else {
                    h.tvOs.setVisibility(View.GONE);
                }
            }

            h.card.setStrokeColor(d.saved ? 0xFF4CAF50 : 0xFF1E2E3E);
            h.ibStar.setImageResource(d.saved ?
                    android.R.drawable.btn_star_big_on :
                    android.R.drawable.btn_star_big_off);

            h.ibStar.setOnClickListener(v -> {
                d.saved = !d.saved;
                notifyItemChanged(position);
                updateStats();
            });

            h.card.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onDeviceClick(d);
                }
            });
        }

        @Override
        public int getItemCount() {
            return shown.size();
        }

        public void add(Device d) {
            all.add(d);
            applyFilter();
        }

        public void setAll(List<Device> devices) {
            all.clear();
            all.addAll(devices);
            applyFilter();
        }

        public void clear() {
            all.clear();
            shown.clear();
            notifyDataSetChanged();
        }

        public void setFilter(String f) {
            filter = f.toLowerCase();
            applyFilter();
        }

        private void applyFilter() {
            shown.clear();
            for (Device d : all) {
                if (filter.isEmpty() ||
                        d.ip.toLowerCase().contains(filter) ||
                        d.mac.toLowerCase().contains(filter) ||
                        d.hostname.toLowerCase().contains(filter) ||
                        d.vendor.toLowerCase().contains(filter) ||
                        d.osGuess.toLowerCase().contains(filter)) {
                    shown.add(d);
                }
            }
            notifyDataSetChanged();
        }

        public List<Device> getAll() {
            return all;
        }

        public int getSavedCount() {
            int count = 0;
            for (Device d : all) {
                if (d.saved) count++;
            }
            return count;
        }

        public int getTotalPorts() {
            int count = 0;
            for (Device d : all) {
                count += d.openPorts.size();
            }
            return count;
        }
    }

    // ============================================================
    // Activity
    // ============================================================
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // Views - Tabs
    private View viewScan, viewDevices, viewMap, viewSettings;

    // Views - Scan
    private TextView tvSsid, tvBssid, tvSubnetInfo, tvGateway, tvStatus, tvLog;
    private EditText etSubnet, etMask;
    private RadioGroup rgProfile;
    private Button btnScan;
    private LinearProgressIndicator progressBar;
    private android.widget.CheckBox cbBanners;

    // Views - Devices
    private EditText etFilter;
    private RecyclerView rvDevices;
    private DeviceAdapter adapter;

    // Views - Map
    private LinearLayout mapContent;

    // Views - Settings
    private EditText etMacLookup, etWolMac, etPingHost, etTracerouteHost;
    private Button btnMacLookup, btnWol, btnPing, btnTraceroute, btnExportJson, btnExportCsv;
    private Button btnMdnsScan, btnUpnpScan, btnSnmpScan;
    private TextView tvMacResult, tvStats, tvToolsResult, tvDiscoveryResult;
    private android.widget.CheckBox cbDiscovery;

    // State
    private Scanner scanner;
    private ScanDbHelper dbHelper;
    private String currentSsid = "";
    private String currentBssid = "";
    private String myIp = "";
    private String gateway = "";
    private StringBuilder logBuffer = new StringBuilder();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private List<Device> lastScanDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanner = new Scanner();
        dbHelper = new ScanDbHelper(this);
        initViews();
        setupNavigation();
        requestPermissions();
    }

    private void initViews() {
        // Tabs
        viewScan = findViewById(R.id.view_scan);
        viewDevices = findViewById(R.id.view_devices);
        viewMap = findViewById(R.id.view_map);
        viewSettings = findViewById(R.id.view_settings);

        // Scan
        tvSsid = findViewById(R.id.tvSsid);
        tvBssid = findViewById(R.id.tvBssid);
        tvSubnetInfo = findViewById(R.id.tvSubnetInfo);
        tvGateway = findViewById(R.id.tvGateway);
        tvStatus = findViewById(R.id.tvStatus);
        tvLog = findViewById(R.id.tvLog);
        etSubnet = findViewById(R.id.etSubnet);
        etMask = findViewById(R.id.etMask);
        rgProfile = findViewById(R.id.rgProfile);
        btnScan = findViewById(R.id.btnScan);
        progressBar = findViewById(R.id.progressBar);
        cbBanners = findViewById(R.id.cbBanners);

        if (btnScan != null) {
            btnScan.setOnClickListener(v -> toggleScan());
        }

        // Devices
        etFilter = findViewById(R.id.etFilter);
        rvDevices = findViewById(R.id.rvDevices);
        adapter = new DeviceAdapter();
        if (rvDevices != null) {
            rvDevices.setLayoutManager(new LinearLayoutManager(this));
            rvDevices.setAdapter(adapter);
        }

        adapter.setOnDeviceClickListener(this::showDeviceDetails);

        if (etFilter != null) {
            etFilter.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.setFilter(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Map
        mapContent = findViewById(R.id.mapContent);

        // Settings
        etMacLookup = findViewById(R.id.etMacLookup);
        btnMacLookup = findViewById(R.id.btnMacLookup);
        tvMacResult = findViewById(R.id.tvMacResult);
        tvStats = findViewById(R.id.tvStats);

        // Tools
        etWolMac = findViewById(R.id.etWolMac);
        btnWol = findViewById(R.id.btnWol);
        etPingHost = findViewById(R.id.etPingHost);
        btnPing = findViewById(R.id.btnPing);
        etTracerouteHost = findViewById(R.id.etTracerouteHost);
        btnTraceroute = findViewById(R.id.btnTraceroute);
        tvToolsResult = findViewById(R.id.tvToolsResult);
        btnExportJson = findViewById(R.id.btnExportJson);
        btnExportCsv = findViewById(R.id.btnExportCsv);

        if (btnMacLookup != null) {
            btnMacLookup.setOnClickListener(v -> {
                String mac = etMacLookup.getText().toString().trim();
                if (mac.length() >= 8) {
                    String prefix = mac.substring(0, 8).toUpperCase();
                    String vendor = oui(mac);
                    tvMacResult.setText("OUI : " + prefix + "\nVendor : " + vendor);
                } else {
                    tvMacResult.setText("Entrez une adresse MAC valide");
                }
            });
        }

        if (btnWol != null) {
            btnWol.setOnClickListener(v -> {
                String mac = etWolMac.getText().toString().trim();
                if (mac.matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")) {
                    String broadcast = gateway.substring(0, gateway.lastIndexOf('.')) + ".255";
                    boolean success = NetTools.sendWoL(mac, broadcast);
                    tvToolsResult.setText(success ?
                            "✓ Magic packet envoyé à " + mac :
                            "✗ Erreur d'envoi WoL");
                } else {
                    tvToolsResult.setText("Format MAC invalide (AA:BB:CC:DD:EE:FF)");
                }
            });
        }

        if (btnPing != null) {
            btnPing.setOnClickListener(v -> {
                String host = etPingHost.getText().toString().trim();
                if (!host.isEmpty()) {
                    tvToolsResult.setText("Ping en cours...");
                    new Thread(() -> {
                        String result = NetTools.ping(host, 4);
                        runOnUiThread(() -> tvToolsResult.setText(result));
                    }).start();
                }
            });
        }

        if (btnTraceroute != null) {
            btnTraceroute.setOnClickListener(v -> {
                String host = etTracerouteHost.getText().toString().trim();
                if (!host.isEmpty()) {
                    tvToolsResult.setText("Traceroute en cours...");
                    new Thread(() -> {
                        List<String> hops = NetTools.traceroute(host, 15);
                        StringBuilder sb = new StringBuilder("Traceroute vers " + host + ":\n");
                        for (String hop : hops) {
                            sb.append(hop).append("\n");
                        }
                        runOnUiThread(() -> tvToolsResult.setText(sb.toString()));
                    }).start();
                }
            });
        }

        if (btnExportJson != null) {
            btnExportJson.setOnClickListener(v -> exportToJson());
        }

        if (btnExportCsv != null) {
            btnExportCsv.setOnClickListener(v -> exportToCsv());
        }

        // Discovery checkbox
        cbDiscovery = findViewById(R.id.cbDiscovery);

        // Discovery tools
        btnMdnsScan = findViewById(R.id.btnMdnsScan);
        btnUpnpScan = findViewById(R.id.btnUpnpScan);
        btnSnmpScan = findViewById(R.id.btnSnmpScan);
        tvDiscoveryResult = findViewById(R.id.tvDiscoveryResult);

        if (btnMdnsScan != null) {
            btnMdnsScan.setOnClickListener(v -> runMdnsScan());
        }

        if (btnUpnpScan != null) {
            btnUpnpScan.setOnClickListener(v -> runUpnpScan());
        }

        if (btnSnmpScan != null) {
            btnSnmpScan.setOnClickListener(v -> runSnmpScan());
        }
    }

    private void runMdnsScan() {
        if (tvDiscoveryResult != null) {
            tvDiscoveryResult.setText("Scan mDNS/Bonjour en cours...");
        }
        new Thread(() -> {
            Map<String, List<String>> results = MdnsDiscovery.discoverAllServices(3000);
            StringBuilder sb = new StringBuilder();
            sb.append("=== mDNS/Bonjour Discovery ===\n\n");
            if (results.isEmpty()) {
                sb.append("Aucun service mDNS trouvé");
            } else {
                for (Map.Entry<String, List<String>> entry : results.entrySet()) {
                    sb.append(entry.getKey()).append(":\n");
                    for (String svc : entry.getValue()) {
                        sb.append("  • ").append(svc).append("\n");
                    }
                    sb.append("\n");
                }
                sb.append("Total: ").append(results.size()).append(" hôtes");
            }
            runOnUiThread(() -> {
                if (tvDiscoveryResult != null) {
                    tvDiscoveryResult.setText(sb.toString());
                }
            });
        }).start();
    }

    private void runUpnpScan() {
        if (tvDiscoveryResult != null) {
            tvDiscoveryResult.setText("Scan UPnP/SSDP en cours...");
        }
        new Thread(() -> {
            Map<String, String> results = UpnpDiscovery.discoverAllDevices(3000);
            StringBuilder sb = new StringBuilder();
            sb.append("=== UPnP/SSDP Discovery ===\n\n");
            if (results.isEmpty()) {
                sb.append("Aucun appareil UPnP trouvé");
            } else {
                for (Map.Entry<String, String> entry : results.entrySet()) {
                    sb.append(entry.getKey()).append(":\n");
                    sb.append("  ").append(entry.getValue().replace("\n", "\n  ")).append("\n\n");
                }
                sb.append("Total: ").append(results.size()).append(" appareils");
            }
            runOnUiThread(() -> {
                if (tvDiscoveryResult != null) {
                    tvDiscoveryResult.setText(sb.toString());
                }
            });
        }).start();
    }

    private void runSnmpScan() {
        String subnet = etSubnet.getText().toString().trim();
        if (subnet.isEmpty()) {
            if (tvDiscoveryResult != null) {
                tvDiscoveryResult.setText("Erreur: entrez d'abord la plage réseau");
            }
            return;
        }
        if (tvDiscoveryResult != null) {
            tvDiscoveryResult.setText("Scan SNMP en cours sur " + subnet + ".1-254...\n(peut prendre 30s)");
        }
        new Thread(() -> {
            Map<String, String> results = SnmpDiscovery.scanNetwork(subnet, 500);
            StringBuilder sb = new StringBuilder();
            sb.append("=== SNMP Discovery ===\n\n");
            if (results.isEmpty()) {
                sb.append("Aucun appareil SNMP trouvé\n");
                sb.append("(assurez-vous que SNMP est activé avec community 'public')");
            } else {
                for (Map.Entry<String, String> entry : results.entrySet()) {
                    sb.append(entry.getKey()).append(":\n");
                    sb.append("  ").append(entry.getValue().replace("\n", "\n  ")).append("\n\n");
                }
                sb.append("Total: ").append(results.size()).append(" appareils SNMP");
            }
            runOnUiThread(() -> {
                if (tvDiscoveryResult != null) {
                    tvDiscoveryResult.setText(sb.toString());
                }
            });
        }).start();
    }

    private void showDeviceDetails(Device d) {
        StringBuilder details = new StringBuilder();
        details.append("IP: ").append(d.ip).append("\n");
        details.append("MAC: ").append(d.mac.isEmpty() ? "N/A" : d.mac).append("\n");
        details.append("Vendor: ").append(d.vendor).append("\n");
        if (!d.hostname.isEmpty()) {
            details.append("Hostname: ").append(d.hostname).append("\n");
        }
        if (!d.osGuess.isEmpty()) {
            details.append("OS: ").append(d.osGuess).append("\n");
        }
        if (d.latencyMs > 0) {
            details.append("Latency: ").append(d.latencyMs).append("ms\n");
        }

        // mDNS services
        if (!d.mdnsServices.isEmpty()) {
            details.append("\n--- Services mDNS ---\n");
            for (String svc : d.mdnsServices) {
                details.append("• ").append(svc).append("\n");
            }
        }

        // UPnP info
        if (!d.upnpInfo.isEmpty()) {
            details.append("\n--- UPnP ---\n");
            details.append(d.upnpInfo).append("\n");
        }

        // SNMP info
        if (!d.snmpInfo.isEmpty()) {
            details.append("\n--- SNMP ---\n");
            details.append(d.snmpInfo).append("\n");
        }

        // Notes
        if (!d.notes.isEmpty()) {
            details.append("\n--- Notes ---\n");
            details.append(d.notes).append("\n");
        }

        details.append("\n--- Ports ouverts ---\n");
        for (int port : d.openPorts) {
            String svc = d.services.get(port);
            details.append(port);
            if (svc != null) details.append(" (").append(svc).append(")");
            String banner = d.banners.get(port);
            if (banner != null && !banner.isEmpty()) {
                details.append("\n  → ").append(banner);
            }
            details.append("\n");
        }
        if (!d.sslCertInfo.isEmpty()) {
            details.append("\n--- Certificat SSL ---\n");
            details.append(d.sslCertInfo).append("\n");
        }

        new AlertDialog.Builder(this, R.style.Theme_NetMapper_Dialog)
                .setTitle("📱 " + d.ip)
                .setMessage(details.toString())
                .setPositiveButton("Fermer", null)
                .setNegativeButton("📝 Notes", (dialog, which) -> showNotesEditor(d))
                .setNeutralButton("Wake-on-LAN", (dialog, which) -> {
                    if (!d.mac.isEmpty()) {
                        String broadcast = gateway.substring(0, gateway.lastIndexOf('.')) + ".255";
                        boolean success = NetTools.sendWoL(d.mac, broadcast);
                        Toast.makeText(this, success ? "WoL envoyé" : "Erreur WoL", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "MAC inconnue", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showNotesEditor(Device d) {
        EditText editNotes = new EditText(this);
        editNotes.setText(d.notes);
        editNotes.setHint("Entrez vos notes...");
        editNotes.setMinLines(3);
        editNotes.setBackgroundColor(0xFF1A2A3A);
        editNotes.setTextColor(0xFFFFFFFF);
        editNotes.setHintTextColor(0xFF666666);
        editNotes.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout container = new LinearLayout(this);
        container.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(8));
        container.addView(editNotes, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this, R.style.Theme_NetMapper_Dialog)
                .setTitle("📝 Notes pour " + d.ip)
                .setView(container)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    d.notes = editNotes.getText().toString().trim();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Notes enregistrées", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void exportToJson() {
        if (lastScanDevices.isEmpty()) {
            Toast.makeText(this, "Aucune donnée à exporter", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n  \"scan_date\": \"").append(sdf.format(new Date())).append("\",\n");
            json.append("  \"subnet\": \"").append(etSubnet.getText()).append("\",\n");
            json.append("  \"devices\": [\n");

            for (int i = 0; i < lastScanDevices.size(); i++) {
                Device d = lastScanDevices.get(i);
                json.append("    {\n");
                json.append("      \"ip\": \"").append(d.ip).append("\",\n");
                json.append("      \"mac\": \"").append(d.mac).append("\",\n");
                json.append("      \"vendor\": \"").append(d.vendor).append("\",\n");
                json.append("      \"hostname\": \"").append(d.hostname).append("\",\n");
                json.append("      \"os\": \"").append(d.osGuess).append("\",\n");
                json.append("      \"latency_ms\": ").append(d.latencyMs).append(",\n");
                json.append("      \"ports\": [");
                for (int j = 0; j < d.openPorts.size(); j++) {
                    if (j > 0) json.append(", ");
                    json.append(d.openPorts.get(j));
                }
                json.append("]\n");
                json.append("    }");
                if (i < lastScanDevices.size() - 1) json.append(",");
                json.append("\n");
            }

            json.append("  ]\n}");

            File file = new File(getExternalFilesDir(null), "netmapper_export.json");
            FileWriter writer = new FileWriter(file);
            writer.write(json.toString());
            writer.close();

            Toast.makeText(this, "Exporté: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToCsv() {
        if (lastScanDevices.isEmpty()) {
            Toast.makeText(this, "Aucune donnée à exporter", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("IP,MAC,Vendor,Hostname,OS,Latency(ms),Ports\n");

            for (Device d : lastScanDevices) {
                csv.append("\"").append(d.ip).append("\",");
                csv.append("\"").append(d.mac).append("\",");
                csv.append("\"").append(d.vendor).append("\",");
                csv.append("\"").append(d.hostname).append("\",");
                csv.append("\"").append(d.osGuess).append("\",");
                csv.append(d.latencyMs).append(",");
                csv.append("\"");
                for (int j = 0; j < d.openPorts.size(); j++) {
                    if (j > 0) csv.append(";");
                    csv.append(d.openPorts.get(j));
                }
                csv.append("\"\n");
            }

            File file = new File(getExternalFilesDir(null), "netmapper_export.csv");
            FileWriter writer = new FileWriter(file);
            writer.write(csv.toString());
            writer.close();

            Toast.makeText(this, "Exporté: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveScanToDb(List<Device> devices, String subnet, String profile, long durationMs) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues scanValues = new ContentValues();
            scanValues.put("timestamp", System.currentTimeMillis());
            scanValues.put("subnet", subnet);
            scanValues.put("profile", profile);
            scanValues.put("hosts_count", devices.size());
            int totalPorts = 0;
            for (Device d : devices) totalPorts += d.openPorts.size();
            scanValues.put("ports_count", totalPorts);
            scanValues.put("duration_ms", durationMs);

            long scanId = db.insert("scans", null, scanValues);

            for (Device d : devices) {
                ContentValues deviceValues = new ContentValues();
                deviceValues.put("scan_id", scanId);
                deviceValues.put("ip", d.ip);
                deviceValues.put("mac", d.mac);
                deviceValues.put("vendor", d.vendor);
                deviceValues.put("hostname", d.hostname);
                StringBuilder ports = new StringBuilder();
                for (int i = 0; i < d.openPorts.size(); i++) {
                    if (i > 0) ports.append(",");
                    ports.append(d.openPorts.get(i));
                }
                deviceValues.put("ports", ports.toString());
                deviceValues.put("latency_ms", d.latencyMs);
                StringBuilder banners = new StringBuilder();
                for (Map.Entry<Integer, String> entry : d.banners.entrySet()) {
                    if (banners.length() > 0) banners.append("|");
                    banners.append(entry.getKey()).append(":").append(entry.getValue());
                }
                deviceValues.put("banners", banners.toString());

                db.insert("devices", null, deviceValues);
            }
        } catch (Exception e) {
            log("DB Error: " + e.getMessage());
        }
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            viewScan.setVisibility(id == R.id.nav_scan ? View.VISIBLE : View.GONE);
            viewDevices.setVisibility(id == R.id.nav_devices ? View.VISIBLE : View.GONE);
            viewMap.setVisibility(id == R.id.nav_map ? View.VISIBLE : View.GONE);
            viewSettings.setVisibility(id == R.id.nav_settings ? View.VISIBLE : View.GONE);

            if (id == R.id.nav_settings) {
                updateStats();
            }
            return true;
        });
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.INTERNET);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        List<String> toRequest = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(p);
            }
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    toRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            loadWifiInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            loadWifiInfo();
        }
    }

    private void loadWifiInfo() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null) {
                    currentSsid = wi.getSSID();
                    if (currentSsid != null) {
                        currentSsid = currentSsid.replace("\"", "");
                    }
                    currentBssid = wi.getBSSID();
                    int ip = wi.getIpAddress();
                    myIp = ip4(ip);

                    DhcpInfo dhcp = wm.getDhcpInfo();
                    if (dhcp != null) {
                        gateway = ip4(dhcp.gateway);
                    }

                    // Populate UI
                    tvSsid.setText("📶 " + (currentSsid != null ? currentSsid : "--"));
                    String vendor = oui(currentBssid);
                    tvBssid.setText("AP : " + (currentBssid != null ? currentBssid : "--") + "  (" + vendor + ")");
                    tvSubnetInfo.setText("IP : " + myIp);
                    tvGateway.setText("GW : " + gateway);

                    // Pre-fill subnet
                    if (!myIp.isEmpty() && myIp.contains(".")) {
                        String[] parts = myIp.split("\\.");
                        if (parts.length == 4) {
                            etSubnet.setText(parts[0] + "." + parts[1] + "." + parts[2]);
                        }
                    }

                    // Pre-fill tools
                    if (etPingHost != null) etPingHost.setText(gateway);
                    if (etTracerouteHost != null) etTracerouteHost.setText("8.8.8.8");
                }
            }
        } catch (Exception e) {
            log("Erreur WiFi: " + e.getMessage());
        }
        updateStats();
    }

    public static String ip4(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    private void toggleScan() {
        if (scanner.isRunning()) {
            scanner.cancel();
            btnScan.setText("🔍 Lancer le scan");
            progressBar.setVisibility(View.GONE);
            log("Scan annulé");
        } else {
            startScan();
        }
    }

    private void startScan() {
        String subnet = etSubnet.getText().toString().trim();
        if (subnet.isEmpty()) {
            log("Erreur: plage réseau vide");
            return;
        }

        adapter.clear();
        logBuffer.setLength(0);
        tvLog.setText("");
        tvStatus.setText("");

        int profile;
        String profileName;
        int checkedId = rgProfile.getCheckedRadioButtonId();
        if (checkedId == R.id.rbPing) {
            profile = Scanner.PROFILE_PING;
            profileName = "Ping";
            log("Profil: Ping (ICMP/ARP uniquement)");
        } else if (checkedId == R.id.rbFull) {
            profile = Scanner.PROFILE_FULL;
            profileName = "Full";
            log("Profil: Full (23 ports + services)");
        } else {
            profile = Scanner.PROFILE_QUICK;
            profileName = "Quick";
            log("Profil: Quick (6 ports)");
        }

        boolean grabBanners = cbBanners != null && cbBanners.isChecked();
        if (grabBanners) {
            log("Banner grabbing activé");
        }

        boolean doDiscovery = cbDiscovery != null && cbDiscovery.isChecked();
        if (doDiscovery) {
            log("Discovery activé (mDNS, UPnP, SNMP)");
        }

        log("Scan de " + subnet + ".1-254...");
        btnScan.setText("⏹ Arrêter");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        final String finalProfileName = profileName;
        final boolean finalDoDiscovery = doDiscovery;
        scanner.scan(subnet, profile, grabBanners, finalDoDiscovery, new ScanListener() {
            @Override
            public void onHost(Device device) {
                adapter.add(device);
                String msg = "✓ " + device.ip;
                if (device.latencyMs > 0) {
                    msg += " (" + device.latencyMs + "ms)";
                }
                if (!device.openPorts.isEmpty()) {
                    msg += " [" + device.openPorts.size() + " ports]";
                }
                if (!device.osGuess.isEmpty()) {
                    msg += " " + device.osGuess;
                }
                log(msg);
            }

            @Override
            public void onProgress(int done, int total) {
                int progress = (done * 100) / total;
                progressBar.setProgress(progress);
            }

            @Override
            public void onDone(List<Device> devices) {
                btnScan.setText("🔍 Lancer le scan");
                progressBar.setVisibility(View.GONE);

                lastScanDevices = devices;

                int hosts = devices.size();
                int ports = 0;
                for (Device d : devices) {
                    ports += d.openPorts.size();
                }

                long duration = scanner.getScanDuration();
                tvStatus.setText(hosts + " hôtes · " + ports + " ports · " + (duration / 1000) + "s");
                log("Scan terminé: " + hosts + " hôtes en " + (duration / 1000) + "s");

                // Save to database
                saveScanToDb(devices, subnet, finalProfileName, duration);

                buildMap(devices);
                updateStats();
            }

            @Override
            public void onLog(String message) {
                log(message);
            }
        });
    }

    private void log(String msg) {
        String line = sdf.format(new Date()) + " " + msg + "\n";
        logBuffer.insert(0, line);
        tvLog.setText(logBuffer.toString());
    }

    private void buildMap(List<Device> devices) {
        mapContent.removeAllViews();

        // Title
        TextView title = new TextView(this);
        title.setText("Topologie réseau");
        title.setTextColor(0xFF7CB8FF);
        title.setTextSize(16);
        title.setPadding(0, 0, 0, 16);
        mapContent.addView(title);

        if (devices.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucun hôte trouvé");
            empty.setTextColor(0xFF666666);
            empty.setPadding(0, 32, 0, 0);
            mapContent.addView(empty);
            return;
        }

        // Internet node
        addMapNode("☁️ INTERNET", "", 0xFFF5A623);
        addMapLine();

        // Gateway
        String gwVendor = oui(currentBssid);
        addMapNode("🌐 " + gateway, currentSsid + " (" + gwVendor + ")", 0xFF0A84FF);

        // Hosts
        for (Device d : devices) {
            addMapLine();
            StringBuilder info = new StringBuilder();
            if (!d.mac.isEmpty()) {
                info.append(d.mac).append(" · ").append(d.vendor);
            }
            if (!d.hostname.isEmpty()) {
                info.append("\n").append(d.hostname);
            }
            if (!d.osGuess.isEmpty()) {
                info.append("\n🖥 ").append(d.osGuess);
            }
            if (d.latencyMs > 0) {
                info.append("\n⏱ ").append(d.latencyMs).append("ms");
            }
            if (!d.openPorts.isEmpty()) {
                info.append("\n");
                for (int i = 0; i < Math.min(d.openPorts.size(), 6); i++) {
                    int port = d.openPorts.get(i);
                    String svc = d.services.get(port);
                    if (i > 0) info.append(" ");
                    info.append(port);
                    if (svc != null && !svc.equals("Unknown")) {
                        info.append("/").append(svc);
                    }
                }
                if (d.openPorts.size() > 6) {
                    info.append(" +").append(d.openPorts.size() - 6);
                }
            }
            int color = d.saved ? 0xFF4CAF50 : 0xFF0A84FF;
            addMapNode("💻 " + d.ip, info.toString(), color);
        }
    }

    private void addMapNode(String title, String info, int color) {
        TextView tv = new TextView(this);
        String text = title;
        if (!info.isEmpty()) {
            text += "\n" + info;
        }
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(12);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setBackgroundColor(0xFF0D1B2A);
        tv.setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(4), 0, dpToPx(4));
        tv.setLayoutParams(lp);

        mapContent.addView(tv);
    }

    private void addMapLine() {
        TextView tv = new TextView(this);
        tv.setText("│");
        tv.setTextColor(0xFF4CAF50);
        tv.setTextSize(14);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setPadding(dpToPx(30), 0, 0, 0);
        mapContent.addView(tv);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateStats() {
        int hosts = adapter.getAll().size();
        int ports = adapter.getTotalPorts();
        int saved = adapter.getSavedCount();
        int ouiEntries = ouiCount();
        int servicesCount = SERVICES.size();

        // Count scans in DB
        int totalScans = 0;
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM scans", null);
            if (cursor.moveToFirst()) {
                totalScans = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception ignored) {}

        tvStats.setText("Hôtes scannés : " + hosts +
                "\nPorts ouverts : " + ports +
                "\nMémorisés : " + saved +
                "\nEntrées OUI : " + ouiEntries +
                "\nServices connus : " + servicesCount +
                "\nScans historiques : " + totalScans);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) {
            scanner.cancel();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
