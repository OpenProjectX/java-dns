plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.byteBuddy)
    implementation(libs.byteBuddyAgent)
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "org.openprojectx.java.dns.core.JavaDnsAgent",
            "Agent-Class" to "org.openprojectx.java.dns.core.JavaDnsAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
}
