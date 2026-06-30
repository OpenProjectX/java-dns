# java-dns

`java-dns` provides a Java 17 `-javaagent` that instruments JDK DNS resolution with Byte Buddy. It lets a launched JVM resolve selected hosts through configured host overrides or custom DNS servers without changing application code.

The repository publishes four modules:

- `core`: the Java agent and DNS runtime.
- `gradle-plugin`: Gradle plugin for launching a Java application with the agent.
- `maven-plugin`: Maven plugin for launching a Java application with the agent.
- `junit5`: JUnit 5 integration that attaches the agent at test runtime.

## Requirements

- JDK 17 or newer.
- Gradle users: Gradle 9.x is used by this repository wrapper.
- Maven users: Maven 3.9.x is targeted by the plugin module.

## Agent Usage

Build the agent jar:

```bash
env GRADLE_USER_HOME=/data/.gradle ./gradlew :core:jar
```

Run a JVM with host overrides:

```bash
java '-javaagent:core/build/libs/core-0.1.0-SNAPSHOT.jar=hosts=example.test/127.0.0.42;fallback=false' \
  -cp build/classes/java/main \
  com.example.Main
```

Agent options are separated by semicolons:

- `hosts=host/address|address,other.host/address`: resolves specific hosts to IP literals.
- `hostsFile=/path/to/hosts`: loads hosts-style mappings such as `127.0.0.1 google.com`.
- `servers=127.0.0.1:5353,8.8.8.8`: queries custom DNS servers by UDP.
- `timeoutMillis=2000`: per-query timeout.
- `cacheTtlSeconds=30`: positive DNS cache TTL inside the agent.
- `fallback=true`: if the custom resolver has no answer, continue with the JVM system resolver.

DNS servers must be IP literals. IPv6 servers can be written as `[2001:4860:4860::8888]:53`.
When both `hosts` and `hostsFile` define the same name, the file entry wins.

## Gradle Plugin

Apply the plugin:

```kotlin
plugins {
    java
    id("org.openprojectx.java.dns") version "0.1.2"
}

javadns {
    hosts.put("example.test", "127.0.0.42")
    hostsFile.set(layout.projectDirectory.file("dns.hosts"))
    servers.add("127.0.0.1:5353")
    fallbackToSystem.set(true)
}
```

The plugin automatically attaches the agent to Gradle `Test` and `JavaExec`
tasks, including the `run` task from the `application` plugin:

```bash
./gradlew test run
```

Generic `Exec` tasks are not modified by default because many of them do not
launch Java. To opt in, set `autoAttachExec.set(true)`; the plugin will append
the agent through `JAVA_TOOL_OPTIONS`.

`javaDnsRun` is also available. It uses `javadns.mainClass` when configured, or
the `application` plugin's `mainClass` when present.

Print the generated agent argument string:

```bash
./gradlew printJavaDnsAgentArgs
```

## Maven Plugin

Example configuration:

```xml
<plugin>
  <groupId>org.openprojectx.java.dns</groupId>
  <artifactId>maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <configuration>
    <mainClass>com.example.Main</mainClass>
    <hostsFile>${project.basedir}/dns.hosts</hostsFile>
    <hosts>
      <example.test>127.0.0.42</example.test>
    </hosts>
    <servers>
      <server>127.0.0.1:5353</server>
    </servers>
    <fallbackToSystem>true</fallbackToSystem>
  </configuration>
</plugin>
```

Run:

```bash
mvn javadns:run
```

You can also pass simple properties:

```bash
mvn javadns:run -Djavadns.mainClass=com.example.Main -Djavadns.timeoutMillis=500
```

## JUnit 5 Runtime Attach

Add the JUnit 5 module to the test runtime classpath to attach the agent
automatically when the JUnit Platform launcher session opens:

```kotlin
dependencies {
    testRuntimeOnly("org.openprojectx.java.dns:junit5:0.1.2-SNAPSHOT")
}
```

Configure it with system properties:

```bash
./gradlew test \
  -Djavadns.hosts=google.com/127.0.0.1 \
  -Djavadns.hostsFile=/path/to/dns.hosts \
  -Djavadns.fallback=false
```

Supported properties:

- `javadns.hosts`: comma-separated `host/address` entries.
- `javadns.hostsFile`: hosts-style file path, for example `127.0.0.1 google.com`.
- `javadns.servers`: comma-separated DNS servers.
- `javadns.timeoutMillis`: per-query timeout.
- `javadns.cacheTtlSeconds`: positive DNS cache TTL.
- `javadns.fallback`: whether to continue with the JVM system resolver.
- `javadns.junit.enabled`: set to `false` to disable automatic JUnit attach.

For explicit per-suite attachment, add the module as `testImplementation` and
use the extension:

```java
import org.junit.jupiter.api.extension.ExtendWith;
import org.openprojectx.java.dns.junit5.JavaDnsExtension;

@ExtendWith(JavaDnsExtension.class)
class MyTest {
}
```

The JUnit integration uses Byte Buddy runtime self-attach. It instruments before
test discovery and execution when loaded through the JUnit Platform service
loader, but this is still later than JVM startup. Use `-javaagent` when DNS
instrumentation must be active before any test JVM code runs.

## Local Development

Run all tests:

```bash
env GRADLE_USER_HOME=/data/.gradle ./gradlew test
```

Run the basic example from this checkout:

```bash
cd examples/basic
env GRADLE_USER_HOME=/data/.gradle ../../gradlew run --no-configuration-cache
```

The example starts WireMock locally, maps `google.com` to `127.0.0.1`, sends an
HTTP request to `http://google.com:<wiremock-port>/`, and verifies WireMock
received the request.

## Limitations

- The agent targets the JDK 17 `java.net.InetAddress` resolver implementation.
- Custom DNS queries are UDP-only.
- Host overrides and DNS server values must use IP literals; names are intentionally not resolved while configuring the resolver.
