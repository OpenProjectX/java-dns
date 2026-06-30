package org.openprojectx.java.dns.junit5;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class JavaDnsJUnit5Test {
    @Test
    void attachesRuntimeAgentFromSystemProperties() throws Exception {
        Path hostsFile = Files.createTempFile("java-dns-junit5-", ".hosts");
        Files.writeString(hostsFile, "127.0.0.47 junit5.test\n");

        System.setProperty("javadns.hostsFile", hostsFile.toString());
        System.setProperty("javadns.fallback", "false");
        try {
            JavaDnsJUnit5.attach();

            assertArrayEquals(new byte[]{127, 0, 0, 47}, InetAddress.getByName("junit5.test").getAddress());
        } finally {
            System.clearProperty("javadns.hostsFile");
            System.clearProperty("javadns.fallback");
        }
    }
}
