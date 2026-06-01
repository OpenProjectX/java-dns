package org.openprojectx.java.dns.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DnsRuntime {
    private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> false);
    private static volatile DnsConfig config = DnsConfig.from("");
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private DnsRuntime() {
    }

    public static void configure(String agentArgs) {
        config = DnsConfig.from(agentArgs);
        CACHE.clear();
    }

    public static InetAddress[] resolve(String host) throws UnknownHostException {
        DnsConfig current = config;
        if (host == null || host.isBlank() || RESOLVING.get()) {
            return null;
        }

        RESOLVING.set(true);
        try {
            String normalizedHost = normalize(host);
            InetAddress[] override = current.hosts.get(normalizedHost);
            if (override != null) {
                return override.clone();
            }

            if (current.servers.isEmpty()) {
                return null;
            }

            CacheEntry cached = CACHE.get(normalizedHost);
            long now = System.currentTimeMillis();
            if (cached != null && cached.expiresAt > now) {
                return cached.addresses.clone();
            }

            InetAddress[] resolved = DnsClient.resolve(normalizedHost, current.servers, current.timeoutMillis);
            if (resolved.length == 0) {
                if (current.fallbackToSystem) {
                    return null;
                }
                throw new UnknownHostException(host);
            }

            CACHE.put(normalizedHost, new CacheEntry(resolved, now + current.cacheTtlMillis));
            return resolved.clone();
        } catch (UnknownHostException e) {
            if (current.fallbackToSystem) {
                return null;
            }
            throw e;
        } finally {
            RESOLVING.set(false);
        }
    }

    public static String agentArgs(List<String> servers, Map<String, String> hosts, int timeoutMillis,
                                   int cacheTtlSeconds, boolean fallbackToSystem) {
        List<String> parts = new ArrayList<>();
        if (servers != null && !servers.isEmpty()) {
            parts.add("servers=" + String.join(",", servers));
        }
        if (hosts != null && !hosts.isEmpty()) {
            List<String> entries = new ArrayList<>();
            hosts.forEach((host, value) -> entries.add(host + "/" + value.replace(',', '|')));
            parts.add("hosts=" + String.join(",", entries));
        }
        parts.add("timeoutMillis=" + timeoutMillis);
        parts.add("cacheTtlSeconds=" + cacheTtlSeconds);
        parts.add("fallback=" + fallbackToSystem);
        return String.join(";", parts);
    }

    private static String normalize(String host) {
        return host.endsWith(".")
                ? host.substring(0, host.length() - 1).toLowerCase(Locale.ROOT)
                : host.toLowerCase(Locale.ROOT);
    }

    static final class DnsConfig {
        final List<DnsServer> servers;
        final Map<String, InetAddress[]> hosts;
        final int timeoutMillis;
        final long cacheTtlMillis;
        final boolean fallbackToSystem;

        private DnsConfig(List<DnsServer> servers, Map<String, InetAddress[]> hosts, int timeoutMillis,
                          long cacheTtlMillis, boolean fallbackToSystem) {
            this.servers = servers;
            this.hosts = hosts;
            this.timeoutMillis = timeoutMillis;
            this.cacheTtlMillis = cacheTtlMillis;
            this.fallbackToSystem = fallbackToSystem;
        }

        static DnsConfig from(String agentArgs) {
            Map<String, String> options = parseOptions(agentArgs);
            List<DnsServer> servers = parseServers(firstNonBlank(
                    options.get("servers"),
                    options.get("server"),
                    System.getProperty("javadns.servers"),
                    System.getProperty("javadns.server")));
            Map<String, InetAddress[]> hosts = parseHosts(firstNonBlank(
                    options.get("hosts"),
                    System.getProperty("javadns.hosts")));
            int timeoutMillis = parseInt(firstNonBlank(options.get("timeoutMillis"),
                    System.getProperty("javadns.timeoutMillis")), 2000);
            int cacheTtlSeconds = parseInt(firstNonBlank(options.get("cacheTtlSeconds"),
                    System.getProperty("javadns.cacheTtlSeconds")), 30);
            boolean fallback = Boolean.parseBoolean(firstNonBlank(options.get("fallback"),
                    System.getProperty("javadns.fallback"), "true"));
            return new DnsConfig(servers, hosts, timeoutMillis, Math.max(0, cacheTtlSeconds) * 1000L, fallback);
        }

        private static Map<String, String> parseOptions(String agentArgs) {
            if (agentArgs == null || agentArgs.isBlank()) {
                return Collections.emptyMap();
            }

            Map<String, String> options = new LinkedHashMap<>();
            for (String part : agentArgs.split(";")) {
                int index = part.indexOf('=');
                if (index > 0) {
                    options.put(part.substring(0, index).trim(), part.substring(index + 1).trim());
                }
            }
            return options;
        }

        private static List<DnsServer> parseServers(String value) {
            if (value == null || value.isBlank()) {
                return Collections.emptyList();
            }

            List<DnsServer> servers = new ArrayList<>();
            for (String token : value.split(",")) {
                if (!token.isBlank()) {
                    servers.add(DnsServer.parse(token.trim()));
                }
            }
            return List.copyOf(servers);
        }

        private static Map<String, InetAddress[]> parseHosts(String value) {
            if (value == null || value.isBlank()) {
                return Collections.emptyMap();
            }

            Map<String, InetAddress[]> hosts = new LinkedHashMap<>();
            for (String token : value.split(",")) {
                int separator = token.indexOf('/');
                if (separator <= 0) {
                    continue;
                }

                String host = normalize(token.substring(0, separator).trim());
                String[] addressTokens = token.substring(separator + 1).split("\\|");
                List<InetAddress> addresses = new ArrayList<>();
                for (String addressToken : addressTokens) {
                    byte[] bytes = IpAddressParser.parse(addressToken.trim());
                    if (bytes != null) {
                        try {
                            addresses.add(InetAddress.getByAddress(host, bytes));
                        } catch (UnknownHostException ignored) {
                            // Parsed literal sizes are already valid.
                        }
                    }
                }
                if (!addresses.isEmpty()) {
                    hosts.put(host, addresses.toArray(InetAddress[]::new));
                }
            }
            return Map.copyOf(hosts);
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }

        private static int parseInt(String value, int defaultValue) {
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    private record CacheEntry(InetAddress[] addresses, long expiresAt) {
    }
}
