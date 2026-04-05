package com.netmapper;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

/**
 * Network scanner for discovering hosts and services.
 */
public class Scanner {
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

    private ArpCache arpCache;
    private Context context;

    /**
     * Set the context for ARP cache operations.
     * @param context Application context
     */
    public void setContext(Context context) {
        this.context = context;
        if (context != null) {
            this.arpCache = new ArpCache(context);
        }
    }

    /**
     * Start a network scan.
     * @param subnet The subnet base (e.g., "192.168.1")
     * @param profile Scan profile (PROFILE_PING, PROFILE_QUICK, or PROFILE_FULL)
     * @param grabBanners Whether to grab service banners
     * @param doDiscovery Whether to perform mDNS/UPnP/SNMP discovery
     * @param listener Callback for scan events
     */
    public void scan(String subnet, int profile, boolean grabBanners, boolean doDiscovery, ScanListener listener) {
        if (running) return;
        running = true;
        devices.clear();
        completed.set(0);
        startTime = System.currentTimeMillis();

        int[] ports;
        switch (profile) {
            case PROFILE_PING:
                ports = PORTS_PING;
                break;
            case PROFILE_FULL:
                ports = PORTS_FULL;
                break;
            default:
                ports = PORTS_QUICK;
                break;
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
                            socket.connect(new InetSocketAddress(ip, port), 500);
                            device.openPorts.add(port);
                            device.services.put(port, OuiDatabase.getServiceName(port));
                            alive = true;

                            // Banner grabbing
                            if (doBanners) {
                                String banner = grabBanner(socket, ip, port);
                                if (!banner.isEmpty()) {
                                    device.banners.put(port, banner);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    // ICMP check if no ports found
                    if (!alive) {
                        try {
                            InetAddress addr = InetAddress.getByName(ip);
                            alive = addr.isReachable(500);
                        } catch (Exception ignored) {
                        }
                    }

                    if (alive) {
                        // Reverse DNS
                        try {
                            InetAddress addr = InetAddress.getByName(ip);
                            String hostname = addr.getCanonicalHostName();
                            if (!hostname.equals(ip)) {
                                device.hostname = hostname;
                            }
                        } catch (Exception ignored) {
                        }

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
                            } catch (Exception ignored) {
                            }

                            // UPnP discovery
                            try {
                                device.upnpInfo = UpnpDiscovery.discoverDevice(ip, 500);
                            } catch (Exception ignored) {
                            }

                            // SNMP discovery
                            try {
                                device.snmpInfo = SnmpDiscovery.queryDevice(ip, 300);
                            } catch (Exception ignored) {
                            }
                        }

                        devices.add(device);
                        handler.post(() -> {
                            if (listener != null && running) {
                                listener.onHost(device);
                            }
                        });
                    }
                } catch (Exception ignored) {
                }

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
            } catch (InterruptedException ignored) {
            }

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
        } catch (Exception ignored) {
        }
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

        } catch (Exception ignored) {
        }
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
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String t) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String t) {
                        }
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
        } catch (Exception ignored) {
        }
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
        // On Android 11+, use ArpCache instead of /proc/net/arp
        boolean useArpCache = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;

        if (useArpCache && arpCache != null) {
            // Enrich from local ARP cache database
            for (Device d : devices) {
                if (d.mac == null || d.mac.isEmpty()) {
                    arpCache.enrichDevice(d);
                }
            }
        }

        // Multiple passes to maximize MAC retrieval
        for (int pass = 0; pass < 3; pass++) {
            // Check how many devices don't have MAC
            int missingMac = 0;
            for (Device d : devices) {
                if (d.mac == null || d.mac.isEmpty()) missingMac++;
            }
            if (missingMac == 0) break; // All MACs found

            // Warm ARP cache
            warmArpCache();

            // Delay to let cache populate
            try {
                Thread.sleep(300 + pass * 200);
            } catch (Exception ignored) {
            }

            // Try all read methods
            tryReadProcArp();
            tryIpNeigh();
            tryShellArp();
        }

        // Store discovered MACs in ArpCache for future use
        if (arpCache != null) {
            for (Device d : devices) {
                if (d.mac != null && !d.mac.isEmpty()) {
                    arpCache.putMac(d.ip, d.mac, ArpCache.SOURCE_SYSTEM_ARP);
                }
            }
        }
    }

    private void warmArpCache() {
        // Force ARP resolution with multiple techniques
        ExecutorService pingPool = Executors.newFixedThreadPool(32);
        int[] arpPorts = {80, 443, 22, 445, 139}; // Common ports to force ARP

        for (Device d : devices) {
            final String ip = d.ip;
            pingPool.submit(() -> {
                // Technique 1: Short TCP connections on common ports (forces ARP)
                for (int port : arpPorts) {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(ip, port), 50);
                    } catch (Exception ignored) {
                    }
                }

                // Technique 2: UDP datagram (forces ARP without connection)
                try (java.net.DatagramSocket udp = new java.net.DatagramSocket()) {
                    byte[] buf = new byte[1];
                    java.net.DatagramPacket packet = new java.net.DatagramPacket(
                            buf, buf.length, InetAddress.getByName(ip), 7); // Echo port
                    udp.send(packet);
                } catch (Exception ignored) {
                }

                // Technique 3: isReachable (ICMP if possible)
                try {
                    InetAddress addr = InetAddress.getByName(ip);
                    addr.isReachable(100);
                } catch (Exception ignored) {
                }

                // Technique 4: Shell ping (more reliable on Android)
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"ping", "-c", "1", "-W", "1", ip});
                    p.waitFor();
                } catch (Exception ignored) {
                }
            });
        }
        pingPool.shutdown();
        try {
            pingPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private boolean tryReadProcArp() {
        int found = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    String ip = parts[0];
                    String mac = parts[3].toUpperCase();
                    if (mac.startsWith("00:00:00") || mac.equals("00:00:00:00:00:00")) continue;
                    if (mac.length() < 17) continue;

                    for (Device d : devices) {
                        if (d.ip.equals(ip) && (d.mac == null || d.mac.isEmpty())) {
                            d.mac = mac;
                            d.vendor = OuiDatabase.getVendor(mac);
                            found++;
                            break;
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception ignored) {
        }
        return found > 0;
    }

    private boolean tryIpNeigh() {
        int found = 0;
        try {
            Process process = Runtime.getRuntime().exec("ip neigh show");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Format: IP dev INTERFACE lladdr MAC STATE
                String[] parts = line.split("\\s+");
                String ip = null;
                String mac = null;
                for (int i = 0; i < parts.length; i++) {
                    if (i == 0) ip = parts[i];
                    if ("lladdr".equals(parts[i]) && i + 1 < parts.length) {
                        mac = parts[i + 1].toUpperCase();
                    }
                }
                if (ip != null && mac != null && mac.length() >= 17) {
                    if (mac.startsWith("00:00:00") || mac.equals("00:00:00:00:00:00")) continue;
                    for (Device d : devices) {
                        if (d.ip.equals(ip) && (d.mac == null || d.mac.isEmpty())) {
                            d.mac = mac;
                            d.vendor = OuiDatabase.getVendor(mac);
                            found++;
                            break;
                        }
                    }
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception ignored) {
        }
        return found > 0;
    }

    private boolean tryShellArp() {
        int found = 0;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "cat /proc/net/arp"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    String ip = parts[0];
                    String mac = parts[3].toUpperCase();
                    if (mac.startsWith("00:00:00") || mac.equals("00:00:00:00:00:00")) continue;
                    if (mac.length() < 17) continue;

                    for (Device d : devices) {
                        if (d.ip.equals(ip) && (d.mac == null || d.mac.isEmpty())) {
                            d.mac = mac;
                            d.vendor = OuiDatabase.getVendor(mac);
                            found++;
                            break;
                        }
                    }
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception ignored) {
        }

        // Method 4: arp -a command (if available)
        if (found == 0) {
            found = tryArpCommand();
        }
        return found > 0;
    }

    private int tryArpCommand() {
        int found = 0;
        try {
            Process process = Runtime.getRuntime().exec("arp -a");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Format: hostname (IP) at MAC [ether] on interface
                // or: ? (192.168.1.1) at aa:bb:cc:dd:ee:ff [ether] on wlan0
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "\\(([0-9.]+)\\)\\s+at\\s+([0-9a-fA-F:]+)"
                );
                java.util.regex.Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String ip = matcher.group(1);
                    String mac = matcher.group(2).toUpperCase();
                    if (mac.startsWith("00:00:00") || mac.equals("00:00:00:00:00:00")) continue;
                    if (mac.length() < 17) continue;

                    for (Device d : devices) {
                        if (d.ip.equals(ip) && (d.mac == null || d.mac.isEmpty())) {
                            d.mac = mac;
                            d.vendor = OuiDatabase.getVendor(mac);
                            found++;
                            break;
                        }
                    }
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception ignored) {
        }
        return found;
    }

    /**
     * Get the duration of the current or last scan.
     * @return Duration in milliseconds
     */
    public long getScanDuration() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Cancel the current scan.
     */
    public void cancel() {
        running = false;
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    /**
     * Check if a scan is currently running.
     * @return true if scan is running
     */
    public boolean isRunning() {
        return running;
    }
}
