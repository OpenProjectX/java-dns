plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junitJupiter)
    testImplementation(gradleTestKit())
    testRuntimeOnly(libs.junitPlatformLauncher)
}

gradlePlugin {
    plugins {
        create("javadns") {
            id = "org.openprojectx.java.dns"
            implementationClass = "org.openprojectx.java.dns.plugin.JavaDnsPlugin"
            displayName = "Java Dns"
            description = "Java Dns Gradle plugin"
        }
    }
}
