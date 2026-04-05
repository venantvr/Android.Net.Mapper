package com.netmapper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite helper for storing scan history.
 */
public class ScanDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "netmapper.db";
    private static final int DB_VERSION = 2; // Incremented for arp_cache table

    public ScanDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Scan history table
        db.execSQL("CREATE TABLE scans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp INTEGER," +
                "subnet TEXT," +
                "profile TEXT," +
                "hosts_count INTEGER," +
                "ports_count INTEGER," +
                "duration_ms INTEGER)");

        // Devices found in scans
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

        // ARP cache table (for Android 11+ compatibility)
        db.execSQL("CREATE TABLE arp_cache (" +
                "ip TEXT PRIMARY KEY," +
                "mac TEXT," +
                "vendor TEXT," +
                "last_seen INTEGER," +
                "source TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add arp_cache table
            db.execSQL("CREATE TABLE IF NOT EXISTS arp_cache (" +
                    "ip TEXT PRIMARY KEY," +
                    "mac TEXT," +
                    "vendor TEXT," +
                    "last_seen INTEGER," +
                    "source TEXT)");
        }
    }
}
