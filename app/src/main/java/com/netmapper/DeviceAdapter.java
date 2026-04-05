package com.netmapper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying network devices.
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private final List<Device> all = new ArrayList<>();
    private final List<Device> shown = new ArrayList<>();
    private String filter = "";
    private OnDeviceClickListener clickListener;
    private OnStatsChangedListener statsChangedListener;

    /**
     * Callback for device click events.
     */
    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    /**
     * Callback for stats change events (e.g., when device is starred).
     */
    public interface OnStatsChangedListener {
        void onStatsChanged();
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnStatsChangedListener(OnStatsChangedListener listener) {
        this.statsChangedListener = listener;
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
                h.tvLatency.setText("RTT " + d.latencyMs + "ms");
                h.tvLatency.setVisibility(View.VISIBLE);
            } else {
                h.tvLatency.setVisibility(View.GONE);
            }
        }

        // OS guess
        if (h.tvOs != null) {
            if (!d.osGuess.isEmpty()) {
                h.tvOs.setText("OS: " + d.osGuess);
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
            if (statsChangedListener != null) {
                statsChangedListener.onStatsChanged();
            }
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

    /**
     * Add a device to the list.
     * @param d Device to add
     */
    public void add(Device d) {
        all.add(d);
        applyFilter();
    }

    /**
     * Replace all devices with a new list.
     * @param devices New list of devices
     */
    public void setAll(List<Device> devices) {
        all.clear();
        all.addAll(devices);
        applyFilter();
    }

    /**
     * Clear all devices.
     */
    public void clear() {
        all.clear();
        shown.clear();
        notifyDataSetChanged();
    }

    /**
     * Set the filter string.
     * @param f Filter string (matched against IP, MAC, hostname, vendor, OS)
     */
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

    /**
     * Get all devices (unfiltered).
     * @return List of all devices
     */
    public List<Device> getAll() {
        return all;
    }

    /**
     * Get count of saved (starred) devices.
     * @return Number of saved devices
     */
    public int getSavedCount() {
        int count = 0;
        for (Device d : all) {
            if (d.saved) count++;
        }
        return count;
    }

    /**
     * Get total count of open ports across all devices.
     * @return Total number of open ports
     */
    public int getTotalPorts() {
        int count = 0;
        for (Device d : all) {
            count += d.openPorts.size();
        }
        return count;
    }
}
