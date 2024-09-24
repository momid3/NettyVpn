plugins {
    application
    kotlin("jvm") version "2.0.0"
}

group = "com.momid"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("io.netty:netty-all:4.1.65.Final")
    implementation("org.pcap4j:pcap4j-core:1.8.2")
    implementation("org.pcap4j:pcap4j-packetfactory-static:1.8.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(16)
}

application {
    mainClass = "com.momid.MainKt"
}

tasks {
    // Create a task to assemble a fat JAR
    val fatJar by creating(Jar::class) {
        archiveClassifier.set("fat") // Append "fat" to the JAR file name
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().output) {
            // Include dependencies in the JAR
            from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
            // Optionally, you can exclude specific dependencies if needed
            exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        }
        manifest {
            attributes["Main-Class"] = application.mainClass // Set the main class in the JAR manifest
        }
    }

    // Add the fat JAR task to the build task
    named("build") {
        dependsOn(fatJar)
    }
}
