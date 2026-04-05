package com.netmapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Network tools: ping, traceroute, Wake-on-LAN.
 */
public class NetTools {

    /**
     * Send a Wake-on-LAN magic packet to wake up a device.
     * @param macAddress MAC address of the device (format: AA:BB:CC:DD:EE:FF)
     * @param broadcastIp Broadcast IP address (e.g., 192.168.1.255)
     * @return true if packet was sent successfully
     */
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

    /**
     * Perform a traceroute to the specified host.
     * Uses ping with increasing TTL values.
     * @param host Destination host
     * @param maxHops Maximum number of hops
     * @return List of hops (format: "N: IP" or "N: *")
     */
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
                        reader.close();
                        errReader.close();
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

    /**
     * Ping a host and return the output.
     * @param host Host to ping
     * @param count Number of pings
     * @return Ping output
     */
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

    /**
     * Convert integer IP address to string format.
     * @param ip Integer IP (little-endian as returned by Android)
     * @return IP string (e.g., "192.168.1.1")
     */
    public static String intToIp(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }
}
