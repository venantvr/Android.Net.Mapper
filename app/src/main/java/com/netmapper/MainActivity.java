package com.netmapper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    public static String oui(String mac) {
        if (mac == null || mac.length() < 8) return "Unknown";
        return OUI.getOrDefault(mac.toUpperCase().substring(0, 8), "Unknown Vendor");
    }

    public static int ouiCount() {
        return OUI.size();
    }

    // ============================================================
    // Device Model
    // ============================================================
    public static class Device {
        public String ip;
        public String mac;
        public String vendor;
        public String hostname;
        public String notes;
        public List<Integer> openPorts;
        public boolean saved;
        public long firstSeen;

        public Device(String ip) {
            this.ip = ip;
            this.mac = "";
            this.vendor = "Unknown";
            this.hostname = "";
            this.notes = "";
            this.openPorts = new ArrayList<>();
            this.saved = false;
            this.firstSeen = System.currentTimeMillis();
        }
    }

    // ============================================================
    // Scanner
    // ============================================================
    public interface ScanListener {
        void onHost(Device device);
        void onProgress(int done, int total);
        void onDone(List<Device> devices);
    }

    public static class Scanner {
        private static final int[] PORTS_PING = {};
        private static final int[] PORTS_QUICK = {22, 80, 443, 8080, 3389, 445};
        private static final int[] PORTS_FULL = {21, 22, 23, 25, 53, 80, 110, 139, 143, 443, 445,
            3306, 3389, 5000, 5900, 8080, 8443, 8888};

        public static final int PROFILE_PING = 0;
        public static final int PROFILE_QUICK = 1;
        public static final int PROFILE_FULL = 2;

        private volatile boolean running = false;
        private ExecutorService pool;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final List<Device> devices = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger completed = new AtomicInteger(0);

        public void scan(String subnet, int profile, ScanListener listener) {
            if (running) return;
            running = true;
            devices.clear();
            completed.set(0);

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

                pool.submit(() -> {
                    if (!running) return;

                    try {
                        Device device = new Device(ip);
                        boolean alive = false;

                        // Port scanning
                        for (int port : scanPorts) {
                            if (!running) return;
                            try (Socket socket = new Socket()) {
                                socket.connect(new InetSocketAddress(ip, port), 500);
                                device.openPorts.add(port);
                                alive = true;
                            } catch (Exception ignored) {}
                        }

                        // ICMP/ARP check if no ports open
                        if (!alive && scanPorts.length == 0) {
                            try {
                                InetAddress addr = InetAddress.getByName(ip);
                                alive = addr.isReachable(500);
                            } catch (Exception ignored) {}
                        }

                        // Additional reachability check
                        if (!alive && !device.openPorts.isEmpty()) {
                            alive = true;
                        }

                        if (!alive && scanPorts.length > 0) {
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

        public boolean tryNmap(File filesDir, String subnet, String args, StringBuilder out) {
            File nmap = new File(filesDir, "nmap");
            if (!nmap.exists()) return false;

            try {
                nmap.setExecutable(true);
                String cmd = nmap.getAbsolutePath() + " " + args + " " + subnet + ".0/24 -oX -";
                Process process = Runtime.getRuntime().exec(cmd);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append("\n");
                }
                reader.close();

                int exitCode = process.waitFor();
                return exitCode == 0;
            } catch (Exception e) {
                return false;
            }
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
    // Device Adapter
    // ============================================================
    public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
        private final List<Device> all = new ArrayList<>();
        private final List<Device> shown = new ArrayList<>();
        private String filter = "";

        public class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView tvIp, tvMac, tvVendor, tvHost, tvPorts;
            ImageButton ibStar;

            ViewHolder(View v) {
                super(v);
                card = v.findViewById(R.id.cardDevice);
                tvIp = v.findViewById(R.id.tvIp);
                tvMac = v.findViewById(R.id.tvMac);
                tvVendor = v.findViewById(R.id.tvVendor);
                tvHost = v.findViewById(R.id.tvHost);
                tvPorts = v.findViewById(R.id.tvPorts);
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

            if (d.openPorts.isEmpty()) {
                h.tvPorts.setText("Aucun port ouvert");
            } else {
                StringBuilder sb = new StringBuilder("Ports: ");
                for (int i = 0; i < d.openPorts.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(d.openPorts.get(i));
                }
                h.tvPorts.setText(sb.toString());
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
                        d.vendor.toLowerCase().contains(filter)) {
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

    // Views - Devices
    private EditText etFilter;
    private RecyclerView rvDevices;
    private DeviceAdapter adapter;

    // Views - Map
    private LinearLayout mapContent;
    private TextView tvMapEmpty;

    // Views - Settings
    private EditText etMacLookup;
    private Button btnMacLookup;
    private TextView tvMacResult, tvStats;

    // State
    private Scanner scanner;
    private String currentSsid = "";
    private String currentBssid = "";
    private String myIp = "";
    private String gateway = "";
    private StringBuilder logBuffer = new StringBuilder();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanner = new Scanner();
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

        btnScan.setOnClickListener(v -> toggleScan());

        // Devices
        etFilter = findViewById(R.id.etFilter);
        rvDevices = findViewById(R.id.rvDevices);
        adapter = new DeviceAdapter();
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(adapter);

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

        // Map
        mapContent = findViewById(R.id.mapContent);
        tvMapEmpty = findViewById(R.id.tvMapEmpty);

        // Settings
        etMacLookup = findViewById(R.id.etMacLookup);
        btnMacLookup = findViewById(R.id.btnMacLookup);
        tvMacResult = findViewById(R.id.tvMacResult);
        tvStats = findViewById(R.id.tvStats);

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
        int checkedId = rgProfile.getCheckedRadioButtonId();
        if (checkedId == R.id.rbPing) {
            profile = Scanner.PROFILE_PING;
            log("Profil: Ping (ICMP/ARP uniquement)");
        } else if (checkedId == R.id.rbFull) {
            profile = Scanner.PROFILE_FULL;
            log("Profil: Full (18 ports)");
        } else {
            profile = Scanner.PROFILE_QUICK;
            log("Profil: Quick (6 ports)");
        }

        log("Scan de " + subnet + ".1-254...");
        btnScan.setText("⏹ Arrêter");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        scanner.scan(subnet, profile, new ScanListener() {
            @Override
            public void onHost(Device device) {
                adapter.add(device);
                String msg = "✓ " + device.ip;
                if (!device.openPorts.isEmpty()) {
                    msg += " [" + device.openPorts.size() + " ports]";
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

                int hosts = devices.size();
                int ports = 0;
                for (Device d : devices) {
                    ports += d.openPorts.size();
                }

                tvStatus.setText(hosts + " hôtes · " + ports + " ports ouverts");
                log("Scan terminé: " + hosts + " hôtes trouvés");

                buildMap(devices);
                updateStats();
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
            String info = d.mac.isEmpty() ? "" : d.mac + " · " + d.vendor;
            if (!d.hostname.isEmpty()) {
                info += "\n" + d.hostname;
            }
            if (!d.openPorts.isEmpty()) {
                info += "\nPorts: " + portsToString(d.openPorts);
            }
            int color = d.saved ? 0xFF4CAF50 : 0xFF0A84FF;
            addMapNode("💻 " + d.ip, info, color);
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

    private String portsToString(List<Integer> ports) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ports.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(ports.get(i));
        }
        return sb.toString();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateStats() {
        int hosts = adapter.getAll().size();
        int ports = adapter.getTotalPorts();
        int saved = adapter.getSavedCount();
        int oui = ouiCount();

        tvStats.setText("Hôtes scannés : " + hosts +
                "\nPorts ouverts : " + ports +
                "\nMémorisés : " + saved +
                "\nEntrées OUI : " + oui);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) {
            scanner.cancel();
        }
    }
}
