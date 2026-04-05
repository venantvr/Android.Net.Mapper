package com.netmapper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Background service that monitors the network for new devices.
 * Sends notifications when new devices are detected and performs full analysis.
 */
public class NetworkMonitorService extends Service {

    private static final String CHANNEL_ID = "netmapper_monitor";
    private static final String CHANNEL_NAME = "Network Monitor";
    private static final int NOTIFICATION_ID = 1;
    private static final int SCAN_INTERVAL_MS = 60000; // 60 seconds

    private Handler handler;
    private Runnable scanRunnable;
    private Set<String> knownDevices = new HashSet<>();
    private String currentSubnet = "";
    private boolean isRunning = false;
    private PowerManager.WakeLock wakeLock;
    private ArpCache arpCache;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        arpCache = new ArpCache(this);
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopMonitoring();
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification("Monitoring réseau actif", "Surveillance des nouveaux appareils..."));

        if (!isRunning) {
            isRunning = true;
            loadCurrentSubnet();
            loadKnownDevices();
            startMonitoring();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopMonitoring();
        releaseWakeLock();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications du monitoring réseau");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent stopIntent = new Intent(this, NetworkMonitorService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Arrêter", stopPendingIntent)
                .build();
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "NetMapper::MonitorWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutes max
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void loadCurrentSubnet() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                DhcpInfo dhcp = wm.getDhcpInfo();
                if (dhcp != null) {
                    String ip = NetTools.intToIp(dhcp.ipAddress);
                    if (ip.contains(".")) {
                        String[] parts = ip.split("\\.");
                        if (parts.length == 4) {
                            currentSubnet = parts[0] + "." + parts[1] + "." + parts[2];
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void loadKnownDevices() {
        // Load from ARP cache
        List<ArpCache.Entry> entries = arpCache.getAllEntries();
        for (ArpCache.Entry entry : entries) {
            knownDevices.add(entry.ip);
        }
    }

    private void startMonitoring() {
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // Run scan in background thread to avoid UI blocking
                    new Thread(() -> {
                        performQuickScan();
                        if (isRunning) {
                            handler.postDelayed(scanRunnable, SCAN_INTERVAL_MS);
                        }
                    }).start();
                }
            }
        };
        // Initial delay before first scan
        handler.postDelayed(scanRunnable, 5000);
    }

    private void stopMonitoring() {
        isRunning = false;
        if (handler != null && scanRunnable != null) {
            handler.removeCallbacks(scanRunnable);
        }
    }

    private void performQuickScan() {
        if (currentSubnet.isEmpty()) {
            loadCurrentSubnet();
            if (currentSubnet.isEmpty()) return;
        }

        // Use fewer threads to reduce resource usage
        ExecutorService pool = Executors.newFixedThreadPool(16);

        for (int i = 1; i <= 254; i++) {
            final String ip = currentSubnet + "." + i;
            pool.submit(() -> {
                if (!isRunning) return;

                boolean alive = isHostAlive(ip);
                if (alive && !knownDevices.contains(ip)) {
                    // New device detected!
                    knownDevices.add(ip);
                    onNewDeviceDetected(ip);
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean isHostAlive(String ip) {
        // ICMP check first (faster)
        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isReachable(80)) return true;
        } catch (Exception ignored) {
        }

        // Quick TCP check on 2 common ports only
        int[] ports = {80, 443};
        for (int port : ports) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 50);
                return true;
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private void onNewDeviceDetected(String ip) {
        // Send notification
        sendNewDeviceNotification(ip);

        // Perform full analysis in background
        new Thread(() -> analyzeDevice(ip)).start();
    }

    private void sendNewDeviceNotification(String ip) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nouvel appareil détecté !")
                .setContentText(ip + " - Analyse en cours...")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .build();

        manager.notify(ip.hashCode(), notification);

        // Toast on UI thread
        handler.post(() -> {
            Toast.makeText(this, "Nouvel appareil: " + ip, Toast.LENGTH_SHORT).show();
        });
    }

    private void analyzeDevice(String ip) {
        Device device = new Device(ip);

        // Measure latency
        try {
            long start = System.currentTimeMillis();
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isReachable(1000)) {
                device.latencyMs = System.currentTimeMillis() - start;
            }
        } catch (Exception ignored) {
        }

        // Full port scan
        int[] ports = {21, 22, 23, 25, 53, 80, 110, 139, 143, 443, 445,
                993, 995, 1433, 3306, 3389, 5432, 5900, 6379, 8080, 8443, 8888, 27017};

        for (int port : ports) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 500);
                device.openPorts.add(port);
                device.services.put(port, OuiDatabase.getServiceName(port));
            } catch (Exception ignored) {
            }
        }

        // Reverse DNS
        try {
            InetAddress addr = InetAddress.getByName(ip);
            String hostname = addr.getCanonicalHostName();
            if (!hostname.equals(ip)) {
                device.hostname = hostname;
            }
        } catch (Exception ignored) {
        }

        // mDNS discovery
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

        // OS guess
        device.osGuess = guessOS(device);

        // Store in ARP cache (for tracking)
        arpCache.putMac(ip, "", ArpCache.SOURCE_MANUAL);

        // Send analysis complete notification
        sendAnalysisCompleteNotification(device);
    }

    private String guessOS(Device device) {
        boolean hasRdp = device.openPorts.contains(3389);
        boolean hasSmb = device.openPorts.contains(445);
        boolean hasNetbios = device.openPorts.contains(139);
        boolean hasSsh = device.openPorts.contains(22);

        if (hasRdp) return "Windows";
        if (hasSsh && !hasSmb && !hasRdp) return "Linux/Unix";
        if (hasSmb && hasNetbios && !hasSsh) return "Windows";

        return "";
    }

    private void sendAnalysisCompleteNotification(Device device) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        StringBuilder info = new StringBuilder();
        info.append(device.ip);
        if (!device.hostname.isEmpty()) {
            info.append(" (").append(device.hostname).append(")");
        }
        if (!device.osGuess.isEmpty()) {
            info.append("\nOS: ").append(device.osGuess);
        }
        if (!device.openPorts.isEmpty()) {
            info.append("\nPorts: ");
            for (int i = 0; i < Math.min(device.openPorts.size(), 5); i++) {
                if (i > 0) info.append(", ");
                info.append(device.openPorts.get(i));
            }
            if (device.openPorts.size() > 5) {
                info.append(" +").append(device.openPorts.size() - 5);
            }
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Analyse terminée")
                .setContentText(device.ip + " - " + device.openPorts.size() + " ports ouverts")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(info.toString()))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .build();

        manager.notify(device.ip.hashCode() + 1000, notification);

        // Toast on UI thread
        handler.post(() -> {
            Toast.makeText(this,
                    "Analyse OK: " + device.ip + " (" + device.openPorts.size() + " ports)",
                    Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Check if the monitor service is running.
     */
    public static boolean isServiceRunning(Context context) {
        android.app.ActivityManager manager =
                (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service :
                    manager.getRunningServices(Integer.MAX_VALUE)) {
                if (NetworkMonitorService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
