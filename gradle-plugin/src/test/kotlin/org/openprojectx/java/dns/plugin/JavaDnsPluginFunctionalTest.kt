package org.openprojectx.java.dns.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JavaDnsPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    @Test
    fun `prints configured agent args`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"\n")
        val hostsFile = projectDir.resolve("dns.hosts")
        hostsFile.writeText("127.0.0.44 google.com\n")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.openprojectx.java.dns")
            }

            javadns {
                servers.add("127.0.0.1:5353")
                hosts.put("example.test", "127.0.0.42")
                hostsFile.set(layout.projectDirectory.file("dns.hosts"))
                timeoutMillis.set(500)
                cacheTtlSeconds.set(5)
                fallbackToSystem.set(false)
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("printJavaDnsAgentArgs")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printJavaDnsAgentArgs")?.outcome)
        assertTrue(result.output.contains("servers=127.0.0.1:5353"))
        assertTrue(result.output.contains("hosts=example.test/127.0.0.42"))
        assertTrue(result.output.contains("hostsFile=${hostsFile.absolutePath}"))
        assertTrue(result.output.contains("fallback=false"))
    }

    @Test
    fun `automatically attaches to application run task`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"\n")
        projectDir.resolve("dns.hosts").writeText("127.0.0.45 google.com\n")
        projectDir.resolve("src/main/java/example").mkdirs()
        projectDir.resolve("src/main/java/example/Main.java").writeText(
            """
            package example;

            import java.net.InetAddress;

            public class Main {
                public static void main(String[] args) throws Exception {
                    System.out.println(InetAddress.getByName("google.com").getHostAddress());
                }
            }
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                application
                id("org.openprojectx.java.dns")
            }

            application {
                mainClass.set("example.Main")
            }

            javadns {
                hostsFile.set(layout.projectDirectory.file("dns.hosts"))
                fallbackToSystem.set(false)
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("run", "--no-configuration-cache")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":run")?.outcome)
        assertTrue(result.output.contains("127.0.0.45"))
    }

    @Test
    fun `can attach to exec task through java tool options`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"\n")
        projectDir.resolve("dns.hosts").writeText("127.0.0.46 google.com\n")
        projectDir.resolve("src/main/java/example").mkdirs()
        projectDir.resolve("src/main/java/example/Main.java").writeText(
            """
            package example;

            import java.net.InetAddress;

            public class Main {
                public static void main(String[] args) throws Exception {
                    System.out.println(InetAddress.getByName("google.com").getHostAddress());
                }
            }
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
                id("org.openprojectx.java.dns")
            }

            javadns {
                hostsFile.set(layout.projectDirectory.file("dns.hosts"))
                fallbackToSystem.set(false)
                autoAttachExec.set(true)
            }

            tasks.register<Exec>("execJava") {
                dependsOn("classes")
                commandLine(
                    "${System.getProperty("java.home")}/bin/java",
                    "-cp",
                    sourceSets.main.get().runtimeClasspath.asPath,
                    "example.Main"
                )
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("execJava", "--no-configuration-cache")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":execJava")?.outcome)
        assertTrue(result.output.contains("127.0.0.46"))
    }
}
