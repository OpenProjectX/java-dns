package org.openprojectx.java.dns.core;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.Instrumentation;

public final class JavaDnsRuntimeAttacher {
    private JavaDnsRuntimeAttacher() {
    }

    public static void attach(String agentArgs) {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        JavaDnsAgent.agentmain(agentArgs, instrumentation);
    }
}
