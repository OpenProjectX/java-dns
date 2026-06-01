package org.openprojectx.java.dns.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.openprojectx.java.dns.core.DnsRuntime;
import org.openprojectx.java.dns.core.JavaDnsAgent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class JavaDnsMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "javadns.mainClass", required = true)
    private String mainClass;

    @Parameter
    private List<String> args = new ArrayList<>();

    @Parameter
    private List<String> servers = new ArrayList<>();

    @Parameter
    private Map<String, String> hosts = Collections.emptyMap();

    @Parameter(property = "javadns.hostsFile")
    private File hostsFile;

    @Parameter(property = "javadns.timeoutMillis", defaultValue = "2000")
    private int timeoutMillis;

    @Parameter(property = "javadns.cacheTtlSeconds", defaultValue = "30")
    private int cacheTtlSeconds;

    @Parameter(property = "javadns.fallback", defaultValue = "true")
    private boolean fallbackToSystem;

    @Parameter(property = "javadns.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("java-dns: skipped");
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-javaagent:" + agentJar().getAbsolutePath() + "=" + agentArgs());
        command.add("-cp");
        command.add(classpath());
        command.add(mainClass);
        command.addAll(args == null ? List.of() : args);

        try {
            Process process = new ProcessBuilder(command)
                    .inheritIO()
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException("java-dns process failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while waiting for java-dns process", e);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run java-dns process", e);
        }
    }

    private String agentArgs() {
        return DnsRuntime.agentArgs(
                servers,
                hosts,
                hostsFile == null ? null : hostsFile.getAbsolutePath(),
                timeoutMillis,
                cacheTtlSeconds,
                fallbackToSystem);
    }

    private String classpath() throws MojoExecutionException {
        try {
            List<String> elements = new ArrayList<>(project.getRuntimeClasspathElements());
            return String.join(File.pathSeparator, elements);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Runtime classpath is not available", e);
        }
    }

    private File agentJar() throws MojoExecutionException {
        try {
            File file = new File(JavaDnsAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!file.isFile()) {
                throw new MojoExecutionException("java-dns agent must be loaded from a jar: " + file.getAbsolutePath());
            }
            return file;
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot locate java-dns agent jar", e);
        }
    }

    private String javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath();
    }
}
