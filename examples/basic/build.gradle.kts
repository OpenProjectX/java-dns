plugins {
    application
    id("org.openprojectx.java.dns")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.wiremock:wiremock-standalone:3.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.2")
}

javadns {
    hostsFile.set(layout.projectDirectory.file("dns.hosts"))
    fallbackToSystem.set(false)
}

application {
    mainClass.set("example.Main")
}

tasks.test {
    useJUnitPlatform()
}
