package org.openprojectx.java.dns.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.openprojectx.java.dns.core.DnsRuntime
import org.openprojectx.java.dns.core.JavaDnsAgent
import java.io.File

class JavaDnsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "javadns",
            JavaDnsExtension::class.java
        )
        extension.servers.convention(emptyList())
        extension.hosts.convention(emptyMap())
        extension.timeoutMillis.convention(2000)
        extension.cacheTtlSeconds.convention(30)
        extension.fallbackToSystem.convention(true)
        extension.autoAttachJavaExec.convention(true)
        extension.autoAttachTest.convention(true)
        extension.autoAttachExec.convention(false)
        extension.args.convention(emptyList())

        project.tasks.withType(JavaExec::class.java).configureEach { task ->
            task.doFirst {
                if (extension.autoAttachJavaExec.get()) {
                    task.jvmArgs(javaAgentArg(extension))
                }
            }
        }

        project.tasks.withType(Test::class.java).configureEach { task ->
            task.doFirst {
                if (extension.autoAttachTest.get()) {
                    task.jvmArgs(javaAgentArg(extension))
                }
            }
        }

        project.tasks.withType(Exec::class.java).configureEach { task ->
            task.doFirst {
                if (extension.autoAttachExec.get()) {
                    val current = (task.environment["JAVA_TOOL_OPTIONS"] as? String).orEmpty()
                    task.environment(
                        "JAVA_TOOL_OPTIONS",
                        listOf(current, javaAgentArg(extension)).filter { it.isNotBlank() }.joinToString(" ")
                    )
                }
            }
        }

        project.tasks.register("printJavaDnsAgentArgs") { task ->
            task.group = "javadns"
            task.description = "Prints the java-dns agent argument string."
            task.doLast {
                println(agentArgs(extension))
            }
        }

        project.plugins.withType(JavaPlugin::class.java) {
            project.tasks.register("javaDnsRun", JavaExec::class.java) { task ->
                task.group = "javadns"
                task.description = "Runs the project's main class with the java-dns agent attached."
                task.dependsOn(project.tasks.named(JavaPlugin.CLASSES_TASK_NAME))

                task.doFirst {
                    val mainClassName = extension.mainClass.orNull ?: applicationMainClass(project)
                        ?: error("Configure javadns.mainClass or the application plugin's mainClass before running javaDnsRun")
                    task.mainClass.set(mainClassName)
                    task.args(extension.args.get())
                    task.classpath = project.files(
                        project.extensions.getByName("sourceSets")
                            .let { sourceSets ->
                                val main = sourceSets.javaClass.getMethod("getByName", String::class.java)
                                    .invoke(sourceSets, "main")
                                main.javaClass.getMethod("getRuntimeClasspath").invoke(main)
                            }
                    )
                }
            }
        }
    }

    private fun javaAgentArg(extension: JavaDnsExtension): String =
        "-javaagent:${agentJar().absolutePath}=${agentArgs(extension)}"

    private fun agentArgs(extension: JavaDnsExtension): String =
        DnsRuntime.agentArgs(
            extension.servers.get(),
            extension.hosts.get(),
            extension.hostsFile.orNull?.asFile?.absolutePath,
            extension.timeoutMillis.get(),
            extension.cacheTtlSeconds.get(),
            extension.fallbackToSystem.get()
        )

    private fun agentJar(): File {
        val location = JavaDnsAgent::class.java.protectionDomain.codeSource.location
            ?: error("Cannot locate java-dns agent jar")
        val file = File(location.toURI())
        require(file.isFile) {
            "java-dns agent must be loaded from a jar to use -javaagent: ${file.absolutePath}"
        }
        return file
    }

    private fun applicationMainClass(project: Project): String? {
        val application = project.extensions.findByName("application") ?: return null
        val property = application.javaClass.methods
            .firstOrNull { it.name == "getMainClass" && it.parameterCount == 0 }
            ?.invoke(application)
        return property?.javaClass?.methods
            ?.firstOrNull { it.name == "getOrNull" && it.parameterCount == 0 }
            ?.invoke(property) as? String
    }
}
