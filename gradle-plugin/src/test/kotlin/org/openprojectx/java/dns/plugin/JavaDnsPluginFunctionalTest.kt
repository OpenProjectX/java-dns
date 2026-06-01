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
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.openprojectx.java.dns")
            }

            javadns {
                servers.add("127.0.0.1:5353")
                hosts.put("example.test", "127.0.0.42")
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
        assertTrue(result.output.contains("fallback=false"))
    }
}
