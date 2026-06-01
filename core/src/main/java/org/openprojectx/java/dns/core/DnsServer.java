package org.openprojectx.java.dns.core;

import java.net.InetSocketAddress;

record DnsServer(byte[] address, int port) {
    static DnsServer parse(String value) {
        String host = value;
        int port = 53;

        if (value.startsWith("[") && value.contains("]")) {
            int end = value.indexOf(']');
            host = value.substring(1, end);
            if (value.length() > end + 2 && value.charAt(end + 1) == ':') {
                port = Integer.parseInt(value.substring(end + 2));
            }
        } else {
            int colon = value.lastIndexOf(':');
            if (colon > -1 && value.indexOf(':') == colon) {
                host = value.substring(0, colon);
                port = Integer.parseInt(value.substring(colon + 1));
            }
        }

        byte[] bytes = IpAddressParser.parse(host);
        if (bytes == null) {
            throw new IllegalArgumentException("DNS server must be an IP literal: " + value);
        }
        return new DnsServer(bytes, port);
    }

    InetSocketAddress socketAddress() {
        try {
            return new InetSocketAddress(java.net.InetAddress.getByAddress(address), port);
        } catch (java.net.UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }
}
