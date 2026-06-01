# Contributing

## Development Setup

Use JDK 17 or newer. This repository is built with the checked-in Gradle wrapper:

```bash
env GRADLE_USER_HOME=/data/.gradle ./gradlew test
```

The build has three main modules:

- `core`: Java agent, DNS runtime, and agent tests.
- `gradle-plugin`: Gradle DSL and TestKit tests.
- `maven-plugin`: Maven `javadns:run` goal.

## Workflow

1. Keep changes scoped to the module you are working on.
2. Add or update tests when changing resolver behavior, agent argument formatting, or build-tool plugin behavior.
3. Run `env GRADLE_USER_HOME=/data/.gradle ./gradlew test` before submitting changes.
4. For agent changes, also smoke test a real `-javaagent` launch with a host override.

Example smoke test:

```bash
env GRADLE_USER_HOME=/data/.gradle ./gradlew :core:jar
javac /tmp/JavaDnsSmoke.java
java '-javaagent:core/build/libs/core-0.1.0-SNAPSHOT.jar=hosts=example.test/127.0.0.42;fallback=false' \
  -cp /tmp JavaDnsSmoke
```

Where `/tmp/JavaDnsSmoke.java` contains:

```java
import java.net.InetAddress;

public class JavaDnsSmoke {
    public static void main(String[] args) throws Exception {
        System.out.println(InetAddress.getByName("example.test").getHostAddress());
    }
}
```

Expected output includes `127.0.0.42`.

## Code Style

- Prefer Java for agent/runtime code and Kotlin for Gradle plugin code, matching the existing modules.
- Keep agent code conservative: avoid resolving names while configuring DNS servers or host overrides.
- Keep plugin configuration lazy where practical and avoid unnecessary work during Gradle configuration.
- Use clear tests for argument formatting and launched JVM behavior.

## Release Notes

When changing user-facing behavior, update `README.md` and any affected example under `examples/`.
