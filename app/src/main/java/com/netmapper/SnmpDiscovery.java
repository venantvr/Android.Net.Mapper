package com.netmapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SNMP device discovery.
 */
public class SnmpDiscovery {
    private static final int SNMP_PORT = 161;
    private static final String[] COMMUNITIES = {"public", "private"};

    // Common SNMP OIDs
    private static final byte[] OID_SYS_DESCR = {0x2B, 0x06, 0x01, 0x02, 0x01, 0x01, 0x01, 0x00}; // 1.3.6.1.2.1.1.1.0
    private static final byte[] OID_SYS_NAME = {0x2B, 0x06, 0x01, 0x02, 0x01, 0x01, 0x05, 0x00};  // 1.3.6.1.2.1.1.5.0

    /**
     * Query a device for SNMP information.
     * @param ip The target IP address
     * @param timeoutMs Timeout in milliseconds
     * @return SNMP info string or empty string
     */
    public static String queryDevice(String ip, int timeoutMs) {
        StringBuilder result = new StringBuilder();

        for (String community : COMMUNITIES) {
            try {
                // Query sysName
                String sysName = snmpGet(ip, community, OID_SYS_NAME, timeoutMs);
                if (sysName != null && !sysName.isEmpty()) {
                    result.append("Name: ").append(sysName);

                    // Query sysDescr
                    String sysDescr = snmpGet(ip, community, OID_SYS_DESCR, timeoutMs);
                    if (sysDescr != null && !sysDescr.isEmpty()) {
                        result.append("\nDescr: ").append(sysDescr);
                    }

                    result.append("\nCommunity: ").append(community);
                    return result.toString();
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    /**
     * Scan a network subnet for SNMP-enabled devices.
     * @param subnet The subnet base (e.g., "192.168.1")
     * @param timeoutMs Timeout per host in milliseconds
     * @return Map of IP addresses to SNMP info
     */
    public static Map<String, String> scanNetwork(String subnet, int timeoutMs) {
        Map<String, String> results = new HashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(32);

        for (int i = 1; i <= 254; i++) {
            final String ip = subnet + "." + i;
            pool.submit(() -> {
                String info = queryDevice(ip, timeoutMs);
                if (!info.isEmpty()) {
                    synchronized (results) {
                        results.put(ip, info);
                    }
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        return results;
    }

    private static String snmpGet(String ip, String community, byte[] oid, int timeoutMs) {
        try {
            byte[] packet = buildSnmpGetRequest(community, oid);

            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs);

            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, address, SNMP_PORT);
            socket.send(sendPacket);

            byte[] buffer = new byte[1500];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);

            socket.close();

            return parseSnmpResponse(receivePacket.getData(), receivePacket.getLength());
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] buildSnmpGetRequest(String community, byte[] oid) {
        // Build SNMP v1 GET request
        byte[] communityBytes = community.getBytes();

        // Variable bindings (just the OID with null value)
        int varbindLen = oid.length + 4; // OID + null type + null length
        int varbindListLen = varbindLen + 2;

        // PDU
        int pduLen = varbindListLen + 2 + 12; // varbind list + sequence header + request-id + error stuff

        // Message
        int messageLen = pduLen + 2 + communityBytes.length + 2 + 3; // pdu + header + community + header + version

        byte[] packet = new byte[messageLen + 2];
        int pos = 0;

        // SEQUENCE (message)
        packet[pos++] = 0x30;
        packet[pos++] = (byte) messageLen;

        // Version (SNMPv1 = 0)
        packet[pos++] = 0x02; // INTEGER
        packet[pos++] = 0x01; // length
        packet[pos++] = 0x00; // value

        // Community string
        packet[pos++] = 0x04; // OCTET STRING
        packet[pos++] = (byte) communityBytes.length;
        System.arraycopy(communityBytes, 0, packet, pos, communityBytes.length);
        pos += communityBytes.length;

        // GetRequest PDU
        packet[pos++] = (byte) 0xA0; // GET-REQUEST
        packet[pos++] = (byte) pduLen;

        // Request ID
        packet[pos++] = 0x02; // INTEGER
        packet[pos++] = 0x04; // length
        packet[pos++] = 0x00;
        packet[pos++] = 0x00;
        packet[pos++] = 0x00;
        packet[pos++] = 0x01;

        // Error status
        packet[pos++] = 0x02;
        packet[pos++] = 0x01;
        packet[pos++] = 0x00;

        // Error index
        packet[pos++] = 0x02;
        packet[pos++] = 0x01;
        packet[pos++] = 0x00;

        // Variable bindings SEQUENCE
        packet[pos++] = 0x30;
        packet[pos++] = (byte) varbindListLen;

        // VarBind SEQUENCE
        packet[pos++] = 0x30;
        packet[pos++] = (byte) varbindLen;

        // OID
        packet[pos++] = 0x06; // OBJECT IDENTIFIER
        packet[pos++] = (byte) oid.length;
        System.arraycopy(oid, 0, packet, pos, oid.length);
        pos += oid.length;

        // NULL value
        packet[pos++] = 0x05;
        packet[pos++] = 0x00;

        byte[] result = new byte[pos];
        System.arraycopy(packet, 0, result, 0, pos);
        return result;
    }

    private static String parseSnmpResponse(byte[] data, int length) {
        try {
            // Simple parser - look for OCTET STRING values
            for (int i = 0; i < length - 2; i++) {
                if (data[i] == 0x04) { // OCTET STRING
                    int strLen = data[i + 1] & 0xFF;
                    if (strLen > 0 && strLen < 200 && i + 2 + strLen <= length) {
                        String value = new String(data, i + 2, strLen);
                        // Filter out non-printable strings
                        if (isPrintable(value) && value.length() > 1) {
                            return value.trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isPrintable(String s) {
        for (char c : s.toCharArray()) {
            if (c < 32 || c > 126) {
                if (c != '\n' && c != '\r' && c != '\t') {
                    return false;
                }
            }
        }
        return true;
    }
}
