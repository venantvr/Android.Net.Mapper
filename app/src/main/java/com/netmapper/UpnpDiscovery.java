package com.netmapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * UPnP/SSDP device discovery.
 */
public class UpnpDiscovery {
    private static final String SSDP_ADDR = "239.255.255.250";
    private static final int SSDP_PORT = 1900;

    private static final String SSDP_MSEARCH =
            "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: ssdp:all\r\n" +
                    "\r\n";

    /**
     * Discover UPnP device info for a specific IP.
     * @param targetIp The target IP address
     * @param timeoutMs Timeout in milliseconds
     * @return Device info string or empty string
     */
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

    /**
     * Discover all UPnP devices on the network.
     * @param timeoutMs Total timeout in milliseconds
     * @return Map of IP addresses to device info
     */
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
        } catch (Exception ignored) {
        }

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
