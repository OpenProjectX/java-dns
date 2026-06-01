package org.openprojectx.java.dns.core;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public final class JavaDnsAgent {
    private JavaDnsAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        install(agentArgs, instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        install(agentArgs, instrumentation);
    }

    private static void install(String agentArgs, Instrumentation instrumentation) {
        appendHelpersToBootstrap(instrumentation);
        configureBootstrapRuntime(agentArgs);

        new AgentBuilder.Default()
                .ignore(ElementMatchers.none())
                .with(new ErrorLoggingListener())
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.named("java.net.InetAddress"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(InetAddressAllByNameAdvice.class)
                                        .on(ElementMatchers.named("getAllByName")
                                                .and(ElementMatchers.isStatic())
                                                .and(ElementMatchers.isPublic())
                                                .and(ElementMatchers.takesArguments(String.class))))
                                .visit(Advice.to(InetAddressByNameAdvice.class)
                                        .on(ElementMatchers.named("getByName")
                                                .and(ElementMatchers.isStatic())
                                                .and(ElementMatchers.isPublic())
                                                .and(ElementMatchers.takesArguments(String.class)))))
                .installOn(instrumentation);

        new AgentBuilder.Default()
                .ignore(ElementMatchers.none())
                .with(new ErrorLoggingListener())
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.named("java.net.InetAddress$PlatformNameService"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(DnsLookupAdvice.class)
                                .on(ElementMatchers.named("lookupAllHostAddr")
                                        .and(ElementMatchers.takesArguments(String.class)))))
                .installOn(instrumentation);
    }

    private static void appendHelpersToBootstrap(Instrumentation instrumentation) {
        try {
            File helperJar = Files.createTempFile("java-dns-bootstrap-", ".jar").toFile();
            helperJar.deleteOnExit();
            try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(helperJar))) {
                for (String className : bootstrapHelperClasses()) {
                    String resource = className.replace('.', '/') + ".class";
                    try (InputStream input = JavaDnsAgent.class.getClassLoader().getResourceAsStream(resource)) {
                        if (input == null) {
                            continue;
                        }
                        jar.putNextEntry(new ZipEntry(resource));
                        input.transferTo(jar);
                        jar.closeEntry();
                    }
                }
            }
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(helperJar));
        } catch (Exception ignored) {
            // The agent can still work when the helper classes are visible from the system loader.
        }
    }

    private static List<String> bootstrapHelperClasses() {
        return List.of(
                "org.openprojectx.java.dns.core.DnsRuntime",
                "org.openprojectx.java.dns.core.DnsRuntime$DnsConfig",
                "org.openprojectx.java.dns.core.DnsRuntime$CacheEntry",
                "org.openprojectx.java.dns.core.DnsClient",
                "org.openprojectx.java.dns.core.DnsServer",
                "org.openprojectx.java.dns.core.IpAddressParser"
        );
    }

    private static void configureBootstrapRuntime(String agentArgs) {
        try {
            Class<?> runtime = Class.forName("org.openprojectx.java.dns.core.DnsRuntime", true, null);
            runtime.getMethod("configure", String.class).invoke(null, agentArgs);
        } catch (Exception e) {
            DnsRuntime.configure(agentArgs);
        }
    }

    private static final class ErrorLoggingListener extends AgentBuilder.Listener.Adapter {
        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                            Throwable throwable) {
            if (typeName != null && typeName.startsWith("java.net.InetAddress")) {
                System.err.println("java-dns agent failed to transform " + typeName + ": " + throwable);
            }
        }
    }

    public static final class DnsLookupAdvice {
        private DnsLookupAdvice() {
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean enter(@Advice.Argument(0) String host,
                                    @Advice.Local("javaDnsResolved") InetAddress[] resolved)
                throws UnknownHostException {
            InetAddress[] addresses = DnsRuntime.resolve(host);
            if (addresses == null) {
                return false;
            }

            resolved = addresses;
            return true;
        }

        @Advice.OnMethodExit(onThrowable = UnknownHostException.class)
        public static void exit(@Advice.Local("javaDnsResolved") InetAddress[] resolved,
                                @Advice.Return(readOnly = false) InetAddress[] returned,
                                @Advice.Thrown(readOnly = false) UnknownHostException thrown) {
            if (resolved != null) {
                returned = resolved;
                thrown = null;
            }
        }
    }

    public static final class InetAddressAllByNameAdvice {
        private InetAddressAllByNameAdvice() {
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean enter(@Advice.Argument(0) String host,
                                    @Advice.Local("javaDnsResolved") InetAddress[] resolved)
                throws UnknownHostException {
            InetAddress[] addresses = DnsRuntime.resolve(host);
            if (addresses == null) {
                return false;
            }

            resolved = addresses;
            return true;
        }

        @Advice.OnMethodExit(onThrowable = UnknownHostException.class)
        public static void exit(@Advice.Local("javaDnsResolved") InetAddress[] resolved,
                                @Advice.Return(readOnly = false) InetAddress[] returned,
                                @Advice.Thrown(readOnly = false) UnknownHostException thrown) {
            if (resolved != null) {
                returned = resolved;
                thrown = null;
            }
        }
    }

    public static final class InetAddressByNameAdvice {
        private InetAddressByNameAdvice() {
        }

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean enter(@Advice.Argument(0) String host,
                                    @Advice.Local("javaDnsResolved") InetAddress[] resolved)
                throws UnknownHostException {
            InetAddress[] addresses = DnsRuntime.resolve(host);
            if (addresses == null) {
                return false;
            }

            resolved = addresses;
            return true;
        }

        @Advice.OnMethodExit(onThrowable = UnknownHostException.class)
        public static void exit(@Advice.Local("javaDnsResolved") InetAddress[] resolved,
                                @Advice.Return(readOnly = false) InetAddress returned,
                                @Advice.Thrown(readOnly = false) UnknownHostException thrown) {
            if (resolved != null && resolved.length > 0) {
                returned = resolved[0];
                thrown = null;
            }
        }
    }
}
