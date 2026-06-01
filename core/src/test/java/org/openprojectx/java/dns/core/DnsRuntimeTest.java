package org.openprojectx.java.dns.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DnsRuntimeTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesConfiguredHostOverride() throws Exception {
        DnsRuntime.configure("hosts=example.test/127.0.0.42|127.0.0.43;fallback=false");

        InetAddress[] addresses = DnsRuntime.resolve("example.test");

        assertEquals(2, addresses.length);
        assertArrayEquals(new byte[]{127, 0, 0, 42}, addresses[0].getAddress());
        assertArrayEquals(new byte[]{127, 0, 0, 43}, addresses[1].getAddress());
    }

    @Test
    void returnsNullWhenUnconfiguredSoSystemResolverCanRun() throws Exception {
        DnsRuntime.configure("");

        assertNull(DnsRuntime.resolve("example.test"));
    }

    @Test
    void formatsAgentArgsForPlugins() {
        String args = DnsRuntime.agentArgs(
                List.of("127.0.0.1:5353"),
                Map.of("example.test", "127.0.0.42,127.0.0.43"),
                "/tmp/hosts",
                500,
                5,
                false);

        assertEquals("servers=127.0.0.1:5353;hosts=example.test/127.0.0.42|127.0.0.43;hostsFile=/tmp/hosts;timeoutMillis=500;cacheTtlSeconds=5;fallback=false", args);
    }

    @Test
    void resolvesHostsFileEntries() throws Exception {
        Path hosts = tempDir.resolve("hosts");
        Files.writeString(hosts, """
                # comment
                127.0.0.44 google.com www.google.com
                """);

        DnsRuntime.configure("hostsFile=" + hosts + ";fallback=false");

        assertArrayEquals(new byte[]{127, 0, 0, 44}, DnsRuntime.resolve("google.com")[0].getAddress());
        assertArrayEquals(new byte[]{127, 0, 0, 44}, DnsRuntime.resolve("www.google.com")[0].getAddress());
    }
}
