package com.netmapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * mDNS/Bonjour service discovery.
 */
public class MdnsDiscovery {
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

    /**
     * Discover mDNS services for a specific IP.
     * @param targetIp The target IP address
     * @param timeoutMs Timeout in milliseconds
     * @return List of discovered service names
     */
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
            } catch (Exception ignored) {
            }
        }

        return services;
    }

    /**
     * Discover all mDNS services on the network.
     * @param timeoutMs Total timeout in milliseconds
     * @return Map of IP addresses to list of services
     */
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
            } catch (Exception ignored) {
            }
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
