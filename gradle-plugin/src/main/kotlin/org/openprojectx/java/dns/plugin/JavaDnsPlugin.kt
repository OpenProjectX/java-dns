package org.openprojectx.java.dns.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
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
        extension.args.convention(emptyList())

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
                    val mainClassName = extension.mainClass.orNull
                        ?: error("Configure javadns.mainClass before running javaDnsRun")
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
                    task.jvmArgs("-javaagent:${agentJar().absolutePath}=${agentArgs(extension)}")
                }
            }
        }
    }

    private fun agentArgs(extension: JavaDnsExtension): String =
        DnsRuntime.agentArgs(
            extension.servers.get(),
            extension.hosts.get(),
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
}
