plugins {
    java
    id("org.openprojectx.java.dns")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.wiremock:wiremock-standalone:3.9.2")
}

javadns {
    mainClass.set("example.Main")
    hosts.put("google.com", "127.0.0.1")
    fallbackToSystem.set(false)
}
