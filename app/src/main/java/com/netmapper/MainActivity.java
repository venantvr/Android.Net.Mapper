package com.netmapper;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;

/**
 * Main activity for NetMapper - Network scanner application.
 * This class handles only the UI logic. All business logic is in separate classes.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    // Views - Tabs
    private View viewScan, viewDevices, viewMap, viewSettings;

    // Views - Scan
    private TextView tvSsid, tvBssid, tvSubnetInfo, tvGateway, tvStatus, tvLog, tvSubnetError;
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
    private Button btnMdnsScan, btnUpnpScan, btnSnmpScan, btnMonitor;
    private TextView tvMacResult, tvStats, tvToolsResult, tvDiscoveryResult, tvMonitorStatus;
    private android.widget.CheckBox cbDiscovery;

    // State
    private Scanner scanner;
    private ScanDbHelper dbHelper;
    private ArpCache arpCache;
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
        try {
            setContentView(R.layout.activity_main);
            scanner = new Scanner();
            scanner.setContext(this);
            dbHelper = new ScanDbHelper(this);
            arpCache = new ArpCache(dbHelper);
            initViews();
            setupNavigation();
            requestPermissions();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Check if MAC address retrieval is available on this Android version.
     * Android 11 (API 30+) blocks access to /proc/net/arp and ip neigh.
     */
    private boolean isMacRetrievalAvailable() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R;
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
        tvSubnetError = findViewById(R.id.tvSubnetError);
        rgProfile = findViewById(R.id.rgProfile);
        btnScan = findViewById(R.id.btnScan);
        progressBar = findViewById(R.id.progressBar);
        cbBanners = findViewById(R.id.cbBanners);

        // Apply IP input filters
        if (etSubnet != null) {
            IpInputFilter.applySubnetFilter(etSubnet);
            etSubnet.addTextChangedListener(IpInputFilter.createAutoFormatWatcher(etSubnet, true));
            etSubnet.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    validateSubnetInput();
                }
            });
        }
        if (etMask != null) {
            IpInputFilter.applyMaskFilter(etMask);
            etMask.addTextChangedListener(IpInputFilter.createAutoFormatWatcher(etMask, false));
            etMask.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    validateMaskInput();
                }
            });
        }

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
        adapter.setOnStatsChangedListener(this::updateStats);

        if (etFilter != null) {
            etFilter.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.setFilter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
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

        // Hide MAC-dependent elements on Android 11+ (API 30+)
        if (!isMacRetrievalAvailable()) {
            if (etWolMac != null) etWolMac.setVisibility(View.GONE);
            if (btnWol != null) btnWol.setVisibility(View.GONE);
            // MAC Lookup remains visible as it doesn't depend on network retrieval
        }
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
                    String vendor = OuiDatabase.getVendor(mac);
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
                            "Magic packet envoyé à " + mac :
                            "Erreur d'envoi WoL");
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

        // Monitor service
        btnMonitor = findViewById(R.id.btnMonitor);
        tvMonitorStatus = findViewById(R.id.tvMonitorStatus);

        if (btnMonitor != null) {
            btnMonitor.setOnClickListener(v -> toggleMonitorService());
            updateMonitorButton();
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
                        sb.append("  - ").append(svc).append("\n");
                    }
                    sb.append("\n");
                }
                sb.append("Total : ").append(results.size()).append(" hôtes");
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
                sb.append("Total : ").append(results.size()).append(" appareils");
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
                tvDiscoveryResult.setText("Erreur : entrez d'abord la plage réseau");
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
                sb.append("Total : ").append(results.size()).append(" appareils SNMP");
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
        if (d.mac.isEmpty()) {
            if (!isMacRetrievalAvailable()) {
                details.append("MAC: N/A (Android 11+)\n");
                details.append("Vendor: N/A\n");
            } else {
                details.append("MAC: N/A\n");
                details.append("Vendor: ").append(d.vendor).append("\n");
            }
        } else {
            details.append("MAC: ").append(d.mac).append("\n");
            details.append("Vendor: ").append(d.vendor).append("\n");
        }
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
                details.append("- ").append(svc).append("\n");
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
                details.append("\n  -> ").append(banner);
            }
            details.append("\n");
        }
        if (!d.sslCertInfo.isEmpty()) {
            details.append("\n--- Certificat SSL ---\n");
            details.append(d.sslCertInfo).append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_NetMapper_Dialog)
                .setTitle("Device " + d.ip)
                .setMessage(details.toString())
                .setPositiveButton("Fermer", null)
                .setNegativeButton("Notes", (dialog, which) -> showNotesEditor(d));

        // Wake-on-LAN only if MAC available (Android < 11)
        if (isMacRetrievalAvailable() && !d.mac.isEmpty()) {
            builder.setNeutralButton("Wake-on-LAN", (dialog, which) -> {
                String broadcast = gateway.substring(0, gateway.lastIndexOf('.')) + ".255";
                boolean success = NetTools.sendWoL(d.mac, broadcast);
                Toast.makeText(this, success ? "WoL envoyé" : "Erreur WoL", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
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
                .setTitle("Notes pour " + d.ip)
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

            Toast.makeText(this, "Exporté : " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
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

            Toast.makeText(this, "Exporté : " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
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

                // Store MAC in ARP cache for future use
                if (d.mac != null && !d.mac.isEmpty()) {
                    arpCache.putMac(d.ip, d.mac, ArpCache.SOURCE_SYSTEM_ARP);
                }
            }
        } catch (Exception e) {
            log("DB Error: " + e.getMessage());
        }
    }

    private void setupNavigation() {
        View navScan = findViewById(R.id.nav_scan);
        View navDevices = findViewById(R.id.nav_devices);
        View navMap = findViewById(R.id.nav_map);
        View navSettings = findViewById(R.id.nav_settings);

        View.OnClickListener navListener = v -> {
            int id = v.getId();
            selectNavItem(id);
        };

        if (navScan != null) navScan.setOnClickListener(navListener);
        if (navDevices != null) navDevices.setOnClickListener(navListener);
        if (navMap != null) navMap.setOnClickListener(navListener);
        if (navSettings != null) navSettings.setOnClickListener(navListener);

        // Select first tab by default
        selectNavItem(R.id.nav_scan);
    }

    private void selectNavItem(int id) {
        // Update views visibility
        if (viewScan != null) viewScan.setVisibility(id == R.id.nav_scan ? View.VISIBLE : View.GONE);
        if (viewDevices != null)
            viewDevices.setVisibility(id == R.id.nav_devices ? View.VISIBLE : View.GONE);
        if (viewMap != null) viewMap.setVisibility(id == R.id.nav_map ? View.VISIBLE : View.GONE);
        if (viewSettings != null)
            viewSettings.setVisibility(id == R.id.nav_settings ? View.VISIBLE : View.GONE);

        // Update nav item colors
        updateNavColors(id);

        if (id == R.id.nav_settings) {
            updateStats();
        }
    }

    private void updateNavColors(int selectedId) {
        int[] navIds = {R.id.nav_scan, R.id.nav_devices, R.id.nav_map, R.id.nav_settings};
        for (int navId : navIds) {
            View navItem = findViewById(navId);
            if (navItem != null) {
                TextView label = (TextView) ((LinearLayout) navItem).getChildAt(1);
                ImageView icon = (ImageView) ((LinearLayout) navItem).getChildAt(0);
                if (label != null) {
                    label.setTextColor(navId == selectedId ? 0xFF0A84FF : 0xFF666666);
                }
                if (icon != null) {
                    icon.setColorFilter(navId == selectedId ? 0xFF0A84FF : 0xFF666666);
                }
            }
        }
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
                    myIp = NetTools.intToIp(ip);

                    DhcpInfo dhcp = wm.getDhcpInfo();
                    if (dhcp != null) {
                        gateway = NetTools.intToIp(dhcp.gateway);
                    }

                    // Populate UI
                    tvSsid.setText("WiFi: " + (currentSsid != null ? currentSsid : "--"));
                    String vendor = OuiDatabase.getVendor(currentBssid);
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

    private void toggleScan() {
        if (scanner.isRunning()) {
            scanner.cancel();
            btnScan.setText("Lancer le scan");
            progressBar.setVisibility(View.GONE);
            log("Scan annulé");
        } else {
            startScan();
        }
    }

    private void startScan() {
        String subnet = etSubnet.getText().toString().trim();
        if (subnet.isEmpty()) {
            log("Erreur : plage réseau vide");
            return;
        }

        // Validate subnet format
        if (!IpInputFilter.isValidSubnetPrefix(subnet)) {
            log("Erreur : format de plage invalide (ex: 192.168.1)");
            if (tvSubnetError != null) {
                tvSubnetError.setText("Format invalide (ex: 192.168.1)");
                tvSubnetError.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Validate mask if provided
        String mask = etMask != null ? etMask.getText().toString().trim() : "";
        if (!mask.isEmpty() && !IpInputFilter.isValidSubnetMask(mask)) {
            log("Erreur : masque de sous-réseau invalide");
            if (tvSubnetError != null) {
                tvSubnetError.setText("Masque invalide (ex: 255.255.255.0)");
                tvSubnetError.setVisibility(View.VISIBLE);
            }
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
        btnScan.setText("Arrêter");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        final String finalProfileName = profileName;
        final boolean finalDoDiscovery = doDiscovery;
        scanner.scan(subnet, profile, grabBanners, finalDoDiscovery, new ScanListener() {
            @Override
            public void onHost(Device device) {
                adapter.add(device);
                String msg = "+ " + device.ip;
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
                btnScan.setText("Lancer le scan");
                progressBar.setVisibility(View.GONE);

                lastScanDevices = devices;

                int hosts = devices.size();
                int ports = 0;
                for (Device d : devices) {
                    ports += d.openPorts.size();
                }

                long duration = scanner.getScanDuration();
                tvStatus.setText(hosts + " hôtes - " + ports + " ports - " + (duration / 1000) + "s");
                log("Scan terminé : " + hosts + " hôtes en " + (duration / 1000) + "s");

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
        addMapNode("INTERNET", "", 0xFFF5A623);
        addMapLine();

        // Gateway
        String gwVendor = OuiDatabase.getVendor(currentBssid);
        addMapNode("GW " + gateway, currentSsid + " (" + gwVendor + ")", 0xFF0A84FF);

        // Hosts
        for (Device d : devices) {
            addMapLine();
            StringBuilder info = new StringBuilder();
            if (!d.mac.isEmpty()) {
                info.append(d.mac).append(" - ").append(d.vendor);
            }
            if (!d.hostname.isEmpty()) {
                info.append("\n").append(d.hostname);
            }
            if (!d.osGuess.isEmpty()) {
                info.append("\nOS: ").append(d.osGuess);
            }
            if (d.latencyMs > 0) {
                info.append("\nRTT: ").append(d.latencyMs).append("ms");
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
            addMapNode("HOST " + d.ip, info.toString(), color);
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
        tv.setText("|");
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
        int ouiEntries = OuiDatabase.getOuiCount();
        int servicesCount = OuiDatabase.getServicesCount();
        int arpCacheCount = arpCache.getCount();

        // Count scans in DB
        int totalScans = 0;
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM scans", null);
            if (cursor.moveToFirst()) {
                totalScans = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception ignored) {
        }

        tvStats.setText("Hôtes scannés : " + hosts +
                "\nPorts ouverts : " + ports +
                "\nMémorisés : " + saved +
                "\nEntrées OUI : " + ouiEntries +
                "\nServices connus : " + servicesCount +
                "\nCache ARP : " + arpCacheCount +
                "\nScans historiques : " + totalScans);
    }

    private void toggleMonitorService() {
        boolean isRunning = NetworkMonitorService.isServiceRunning(this);

        if (isRunning) {
            // Stop the service
            Intent stopIntent = new Intent(this, NetworkMonitorService.class);
            stopIntent.setAction("STOP");
            startService(stopIntent);
            Toast.makeText(this, "Surveillance arrêtée", Toast.LENGTH_SHORT).show();
        } else {
            // Request notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
                    return;
                }
            }
            startMonitorService();
        }

        // Update button after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(this::updateMonitorButton, 500);
    }

    private void startMonitorService() {
        Intent intent = new Intent(this, NetworkMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Surveillance activée", Toast.LENGTH_SHORT).show();
    }

    private void updateMonitorButton() {
        boolean isRunning = NetworkMonitorService.isServiceRunning(this);

        if (btnMonitor != null) {
            if (isRunning) {
                btnMonitor.setText("Arrêter la surveillance");
                btnMonitor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
            } else {
                btnMonitor.setText("Activer la surveillance");
                btnMonitor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD84315));
            }
        }

        if (tvMonitorStatus != null) {
            tvMonitorStatus.setText(isRunning ?
                    "Surveillance active - Détection des nouveaux appareils" :
                    "Surveillance inactive");
            tvMonitorStatus.setTextColor(isRunning ? 0xFF4CAF50 : 0xFF666666);
        }
    }

    /**
     * Validate subnet prefix input and show error message if invalid.
     */
    private void validateSubnetInput() {
        if (etSubnet == null || tvSubnetError == null) return;

        String subnet = etSubnet.getText().toString().trim();

        if (subnet.isEmpty()) {
            tvSubnetError.setVisibility(View.GONE);
            return;
        }

        // Check if it's a complete and valid subnet prefix
        if (IpInputFilter.isValidSubnetPrefix(subnet)) {
            tvSubnetError.setVisibility(View.GONE);
        } else {
            // Show error with hint about expected format
            String[] parts = subnet.split("\\.", -1);
            if (parts.length < 3) {
                tvSubnetError.setText("Format : XXX.XXX.XXX (ex: 192.168.1)");
            } else {
                tvSubnetError.setText("Valeurs invalides (0-255)");
            }
            tvSubnetError.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Validate subnet mask input and show error message if invalid.
     */
    private void validateMaskInput() {
        if (etMask == null || tvSubnetError == null) return;

        String mask = etMask.getText().toString().trim();

        if (mask.isEmpty()) {
            // Mask is optional, no error
            return;
        }

        // Check if it's a complete and valid subnet mask
        if (IpInputFilter.isValidSubnetMask(mask)) {
            tvSubnetError.setVisibility(View.GONE);
        } else if (!IpInputFilter.isValidIp(mask)) {
            tvSubnetError.setText("Masque invalide (ex: 255.255.255.0)");
            tvSubnetError.setVisibility(View.VISIBLE);
        } else {
            tvSubnetError.setText("Masque non contigu");
            tvSubnetError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMonitorButton();
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
