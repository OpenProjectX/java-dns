package org.openprojectx.java.dns.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

final class DnsClient {
    private static final int TYPE_A = 1;
    private static final int TYPE_AAAA = 28;
    private static final int CLASS_IN = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    private DnsClient() {
    }

    static InetAddress[] resolve(String host, List<DnsServer> servers, int timeoutMillis) throws UnknownHostException {
        List<InetAddress> addresses = new ArrayList<>();
        for (DnsServer server : servers) {
            addresses.addAll(query(host, server, TYPE_A, timeoutMillis));
            addresses.addAll(query(host, server, TYPE_AAAA, timeoutMillis));
            if (!addresses.isEmpty()) {
                break;
            }
        }
        return addresses.toArray(InetAddress[]::new);
    }

    private static List<InetAddress> query(String host, DnsServer server, int type, int timeoutMillis)
            throws UnknownHostException {
        int id = RANDOM.nextInt(0x10000);
        byte[] request = request(host, type, id);
        byte[] response = new byte[1500];

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMillis);
            socket.send(new DatagramPacket(request, request.length, server.socketAddress()));
            DatagramPacket packet = new DatagramPacket(response, response.length);
            socket.receive(packet);
            return parse(host, type, id, packet.getData(), packet.getLength());
        } catch (SocketTimeoutException e) {
            return List.of();
        } catch (IOException e) {
            UnknownHostException unknownHostException = new UnknownHostException(host);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    private static byte[] request(String host, int type, int id) throws UnknownHostException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeShort(out, id);
        writeShort(out, 0x0100);
        writeShort(out, 1);
        writeShort(out, 0);
        writeShort(out, 0);
        writeShort(out, 0);

        for (String label : host.split("\\.")) {
            byte[] bytes = label.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            if (bytes.length == 0 || bytes.length > 63) {
                throw new UnknownHostException(host);
            }
            out.write(bytes.length);
            out.writeBytes(bytes);
        }
        out.write(0);
        writeShort(out, type);
        writeShort(out, CLASS_IN);
        return out.toByteArray();
    }

    private static List<InetAddress> parse(String host, int type, int id, byte[] packet, int length)
            throws UnknownHostException {
        if (length < 12 || readUnsignedShort(packet, 0) != id) {
            return List.of();
        }

        int answerCount = readUnsignedShort(packet, 6);
        int offset = 12;
        offset = skipName(packet, length, offset);
        offset += 4;

        List<InetAddress> addresses = new ArrayList<>();
        for (int i = 0; i < answerCount && offset < length; i++) {
            offset = skipName(packet, length, offset);
            int recordType = readUnsignedShort(packet, offset);
            int recordClass = readUnsignedShort(packet, offset + 2);
            int recordLength = readUnsignedShort(packet, offset + 8);
            offset += 10;

            if (recordType == type && recordClass == CLASS_IN &&
                    ((type == TYPE_A && recordLength == 4) || (type == TYPE_AAAA && recordLength == 16))) {
                byte[] address = new byte[recordLength];
                System.arraycopy(packet, offset, address, 0, recordLength);
                addresses.add(InetAddress.getByAddress(host, address));
            }
            offset += recordLength;
        }
        return addresses;
    }

    private static int skipName(byte[] packet, int length, int offset) throws UnknownHostException {
        while (offset < length) {
            int labelLength = packet[offset] & 0xff;
            if ((labelLength & 0xc0) == 0xc0) {
                return offset + 2;
            }
            if (labelLength == 0) {
                return offset + 1;
            }
            offset += labelLength + 1;
        }
        throw new UnknownHostException("Malformed DNS packet");
    }

    private static int readUnsignedShort(byte[] packet, int offset) {
        return ((packet[offset] & 0xff) << 8) | (packet[offset + 1] & 0xff);
    }

    private static void writeShort(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xff);
        out.write(value & 0xff);
    }
}
