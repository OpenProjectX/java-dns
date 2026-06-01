import net.researchgate.release.ReleaseExtension
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.plugins.signing.Sign

plugins {
    `maven-publish`
    signing
    id("org.asciidoctor.jvm.convert") version "4.0.2"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("net.researchgate.release") version "3.1.0"
}

tasks.named<AsciidoctorTask>("asciidoctor") {
    group = "documentation"
    description = "Generates HTML documentation from AsciiDoc sources"
    notCompatibleWithConfigurationCache("Asciidoctor task configuration is not configuration-cache compatible in this build")
    setSourceDir(file("doc"))
    sources {
        include("user-guide.adoc")
    }
    setOutputDir(layout.buildDirectory.dir("docs").get().asFile)
    doLast {
        copy {
            from(layout.buildDirectory.file("docs/user-guide.html"))
            into(layout.buildDirectory.dir("docs"))
            rename { "index.html" }
        }
    }
}

val syncDocsVersion by tasks.registering {
    val versionedDocFiles = layout.files(
        layout.projectDirectory.file("README.md"),
        layout.projectDirectory.file("doc/user-guide.adoc"),
    )

    group = "documentation"
    description = "Syncs plugin version snippets in README and user guide to project.version"

    inputs.property("pluginVersion", project.version.toString())
    outputs.files(versionedDocFiles)
    inputs.files(versionedDocFiles)

    doLast {
        val pluginVersion = inputs.properties["pluginVersion"].toString()
        val pluginVersionSnippetRegex =
            Regex("""(id\("${Regex.escape("org.openprojectx.java.dns")}"\) version ")([^"]+)(")""")

        inputs.files.files.forEach { file ->
            if (!file.exists()) return@forEach

            val original = file.readText()
            val updated = pluginVersionSnippetRegex.replace(original) { match ->
                "${match.groupValues[1]}$pluginVersion${match.groupValues[3]}"
            }

            if (original != updated) {
                file.writeText(updated)
            }
        }
    }
}

allprojects {
    group = "org.openprojectx.java.dns"
}

subprojects {
    tasks.register<DependencyReportTask>("allDependencies") {}

    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension>("java") {
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType(Javadoc::class.java).configureEach {
            isFailOnError = false
        }

        extensions.configure<PublishingExtension>("publishing") {
            publications {
                if (project.name != "plugin" && findByName("mavenJava") == null) {
                    create<MavenPublication>("mavenJava") {
                        from(components["java"])
                        artifactId = project.name
                    }
                }
            }

            publications.withType<MavenPublication>().configureEach {
                pom {
                    name.set(
                        when (project.name) {
                            "plugin" -> "Java Dns"
                            "core" -> "Java Dns Core"
                            "maven-plugin" -> "Java Dns Maven Plugin"
                            else -> project.name
                        }
                    )
                    description.set("Java Dns Gradle plugin")
                    url.set("https://github.com/OpenProjectX/java-dns")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("OpenProjectX")
                            name.set("OpenProjectX")
                            email.set("admin@openprojectx.org")
                        }
                    }

                    scm {
                        url.set("https://github.com/OpenProjectX/java-dns")
                        connection.set("scm:git:https://github.com/OpenProjectX/java-dns.git")
                        developerConnection.set("scm:git:ssh://git@github.com:OpenProjectX/java-dns.git")
                    }
                }
            }
        }
    }

    extensions.configure<SigningExtension>("signing") {
        val keyFile = System.getenv("SIGNING_KEY_FILE")
        val keyPass = System.getenv("SIGNING_KEY_PASSWORD")

        if (!keyFile.isNullOrBlank()) {
            val keyText = file(keyFile).readText()
            useInMemoryPgpKeys(keyText, keyPass)

            val publishing = extensions.findByType(PublishingExtension::class.java)
            if (publishing != null) {
                sign(publishing.publications)
            }
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}

configure<ReleaseExtension> {
    buildTasks.set(listOf("syncDocsVersion", "publishToSonatype", "closeAndReleaseSonatypeStagingRepository"))
    versionPropertyFile.set("gradle.properties")
    tagTemplate.set("\$name-\$version")

    with(git) {
        requireBranch.set("master")
    }
}
