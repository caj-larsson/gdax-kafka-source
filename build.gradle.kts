plugins {
    application
    kotlin("jvm") version "1.2.41"
}

application {
    mainClassName = "bitkonnekt.GdaxKafkaSourceKt"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.apache.kafka:kafka-clients:1.1.0")
    compile("ch.qos.logback:logback-classic:1.2.2")
    compile("com.neovisionaries:nv-websocket-client:2.5")
    compile("io.github.cdimascio:java-dotenv:3.1.1")
    compile("com.google.code.gson:gson:2.8.+")
}

description = "A Kafka producer for the streaming GDAX API"
version = "0.1.0"

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Main-Class"] = application.mainClassName
    }
    from(configurations.runtime.map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks["jar"] as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
