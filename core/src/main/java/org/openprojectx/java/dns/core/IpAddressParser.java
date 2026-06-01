package org.openprojectx.java.dns.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

final class IpAddressParser {
    private IpAddressParser() {
    }

    static byte[] parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.indexOf(':') >= 0) {
            return parseIpv6(value);
        }
        return parseIpv4(value);
    }

    private static byte[] parseIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < parts.length; i++) {
            try {
                int part = Integer.parseInt(parts[i]);
                if (part < 0 || part > 255) {
                    return null;
                }
                bytes[i] = (byte) part;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return bytes;
    }

    private static byte[] parseIpv6(String value) {
        try {
            InetAddress address = InetAddress.getByName(value);
            byte[] bytes = address.getAddress();
            return bytes.length == 16 ? bytes : null;
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
