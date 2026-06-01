# Basic Java Dns Example

This example applies the plugin from the repository checkout via `includeBuild("../..")`.
It starts WireMock on localhost, maps `google.com` to `127.0.0.1`, calls
`http://google.com:<wiremock-port>/`, and verifies WireMock received the request
with the `google.com` host header.

```bash
env GRADLE_USER_HOME=/data/.gradle ../../gradlew run --no-configuration-cache
```

The same DNS override is also exercised by JUnit through the auto-attached
`test` task:

```bash
env GRADLE_USER_HOME=/data/.gradle ../../gradlew test --no-configuration-cache
```

Expected output includes:

```text
200
resolved-by-java-dns
Verified google.com resolved to WireMock on 127.0.0.1:
```
