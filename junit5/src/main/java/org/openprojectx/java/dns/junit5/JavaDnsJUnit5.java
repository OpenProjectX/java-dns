package org.openprojectx.java.dns.junit5;

import org.openprojectx.java.dns.core.DnsRuntime;
import org.openprojectx.java.dns.core.JavaDnsRuntimeAttacher;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JavaDnsJUnit5 {
    public static final String ENABLED_PROPERTY = "javadns.junit.enabled";

    private JavaDnsJUnit5() {
    }

    public static void attach() {
        if (!enabled()) {
            return;
        }

        JavaDnsRuntimeAttacher.attach(agentArgs());
    }

    private static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "true"));
    }

    private static String agentArgs() {
        return DnsRuntime.agentArgs(
                listProperty("javadns.servers"),
                mapProperty("javadns.hosts"),
                System.getProperty("javadns.hostsFile"),
                intProperty("javadns.timeoutMillis", 2000),
                intProperty("javadns.cacheTtlSeconds", 30),
                Boolean.parseBoolean(System.getProperty("javadns.fallback", "true")));
    }

    private static List<String> listProperty(String name) {
        String value = System.getProperty(name, "");
        if (value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static Map<String, String> mapProperty(String name) {
        String value = System.getProperty(name, "");
        if (value.isBlank()) {
            return Map.of();
        }

        Map<String, String> hosts = new LinkedHashMap<>();
        for (String entry : value.split(",")) {
            int separator = entry.indexOf('/');
            if (separator > 0 && separator < entry.length() - 1) {
                hosts.put(entry.substring(0, separator).trim(), entry.substring(separator + 1).trim());
            }
        }
        return hosts;
    }

    private static int intProperty(String name, int defaultValue) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
