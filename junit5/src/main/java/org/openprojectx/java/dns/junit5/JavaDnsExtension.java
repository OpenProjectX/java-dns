package org.openprojectx.java.dns.junit5;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class JavaDnsExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        JavaDnsJUnit5.attach();
    }
}
