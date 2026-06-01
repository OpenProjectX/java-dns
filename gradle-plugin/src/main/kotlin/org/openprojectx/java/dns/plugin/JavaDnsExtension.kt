package org.openprojectx.java.dns.plugin

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

abstract class JavaDnsExtension {
    abstract val servers: ListProperty<String>
    abstract val hosts: MapProperty<String, String>
    abstract val timeoutMillis: Property<Int>
    abstract val cacheTtlSeconds: Property<Int>
    abstract val fallbackToSystem: Property<Boolean>
    abstract val mainClass: Property<String>
    abstract val args: ListProperty<String>
}
