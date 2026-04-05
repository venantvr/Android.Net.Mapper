package com.netmapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent ARP cache using SQLite.
 * On Android 11+, /proc/net/arp is no longer accessible, so this class provides
 * a local database to store MAC addresses discovered through various methods.
 *
 * Table: arp_cache
 * - ip TEXT PRIMARY KEY
 * - mac TEXT
 * - vendor TEXT
 * - last_seen INTEGER (timestamp)
 * - source TEXT ("system_arp", "mdns", "upnp", "snmp", "manual")
 */
public class ArpCache {

    // Source constants
    public static final String SOURCE_SYSTEM_ARP = "system_arp";
    public static final String SOURCE_MDNS = "mdns";
    public static final String SOURCE_UPNP = "upnp";
    public static final String SOURCE_SNMP = "snmp";
    public static final String SOURCE_MANUAL = "manual";

    private final ScanDbHelper dbHelper;

    /**
     * Entry in the ARP cache.
     */
    public static class Entry {
        public String ip;
        public String mac;
        public String vendor;
        public long lastSeen;
        public String source;

        public Entry(String ip, String mac, String vendor, long lastSeen, String source) {
            this.ip = ip;
            this.mac = mac;
            this.vendor = vendor;
            this.lastSeen = lastSeen;
            this.source = source;
        }
    }

    public ArpCache(Context context) {
        this.dbHelper = new ScanDbHelper(context);
    }

    public ArpCache(ScanDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Get MAC address for an IP.
     * @param ip The IP address to look up
     * @return MAC address or null if not found
     */
    public String getMac(String ip) {
        if (ip == null || ip.isEmpty()) return null;

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query("arp_cache",
                    new String[]{"mac"},
                    "ip = ?",
                    new String[]{ip},
                    null, null, null);

            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    /**
     * Get full entry for an IP.
     * @param ip The IP address to look up
     * @return Entry or null if not found
     */
    public Entry getEntry(String ip) {
        if (ip == null || ip.isEmpty()) return null;

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query("arp_cache",
                    new String[]{"ip", "mac", "vendor", "last_seen", "source"},
                    "ip = ?",
                    new String[]{ip},
                    null, null, null);

            if (cursor.moveToFirst()) {
                return new Entry(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        cursor.getString(4)
                );
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    /**
     * Store or update a MAC address in the cache.
     * @param ip The IP address
     * @param mac The MAC address
     * @param source The source of this information
     */
    public void putMac(String ip, String mac, String source) {
        if (ip == null || ip.isEmpty() || mac == null || mac.isEmpty()) return;

        // Normalize MAC address
        mac = mac.toUpperCase();
        if (mac.length() < 17) return;
        if (mac.startsWith("00:00:00") || mac.equals("00:00:00:00:00:00")) return;

        String vendor = OuiDatabase.getVendor(mac);

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("ip", ip);
            values.put("mac", mac);
            values.put("vendor", vendor);
            values.put("last_seen", System.currentTimeMillis());
            values.put("source", source != null ? source : SOURCE_MANUAL);

            db.insertWithOnConflict("arp_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception ignored) {
        }
    }

    /**
     * Get all entries in the cache.
     * @return List of all entries
     */
    public List<Entry> getAllEntries() {
        List<Entry> entries = new ArrayList<>();

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query("arp_cache",
                    new String[]{"ip", "mac", "vendor", "last_seen", "source"},
                    null, null, null, null,
                    "last_seen DESC");

            while (cursor.moveToNext()) {
                entries.add(new Entry(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        cursor.getString(4)
                ));
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        return entries;
    }

    /**
     * Remove old entries from the cache.
     * @param maxAgeDays Maximum age in days (entries older than this will be deleted)
     * @return Number of entries deleted
     */
    public int cleanup(int maxAgeDays) {
        if (maxAgeDays <= 0) return 0;

        long cutoff = System.currentTimeMillis() - (maxAgeDays * 24L * 60L * 60L * 1000L);

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            return db.delete("arp_cache", "last_seen < ?", new String[]{String.valueOf(cutoff)});
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Clear all entries from the cache.
     * @return Number of entries deleted
     */
    public int clear() {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            return db.delete("arp_cache", null, null);
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Get the number of entries in the cache.
     * @return Number of entries
     */
    public int getCount() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT COUNT(*) FROM arp_cache", null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    /**
     * Enrich a device with MAC information from the cache.
     * @param device The device to enrich
     * @return true if MAC was found and applied
     */
    public boolean enrichDevice(Device device) {
        if (device == null || device.ip == null) return false;
        if (device.mac != null && !device.mac.isEmpty()) return false; // Already has MAC

        Entry entry = getEntry(device.ip);
        if (entry != null && entry.mac != null && !entry.mac.isEmpty()) {
            device.mac = entry.mac;
            device.vendor = entry.vendor;
            return true;
        }
        return false;
    }
}
