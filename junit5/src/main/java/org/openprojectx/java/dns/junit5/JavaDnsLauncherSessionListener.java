package org.openprojectx.java.dns.junit5;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

public final class JavaDnsLauncherSessionListener implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        JavaDnsJUnit5.attach();
    }
}
