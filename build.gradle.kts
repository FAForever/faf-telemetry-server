import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    val kotlinVersion = "2.4.10"

    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("com.google.devtools.ksp") version "2.3.13"
    id("com.gradleup.shadow") version "9.6.1"
    id("io.micronaut.application") version "5.0.2"
    id("io.micronaut.docker") version "5.0.2"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

version = "0.1"
group = "com.faforever.ice"

repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut:micronaut-websocket")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("tools.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.yaml:snakeyaml")
}

application {
    mainClass.set("com.faforever.ice.telemetry.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks {
    named<DockerBuildImage>("dockerBuild") {
        images.empty()
        images.add("faforever/faf-telemetry-server")
    }
}

graalvmNative.toolchainDetection.set(false)

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.faforever.ice.*")
    }
}

docker {
    registryCredentials {
        val envUsername = System.getenv("DOCKER_USERNAME")
        val envPassword = System.getenv("DOCKER_PASSWORD")

        if (envUsername != null && envPassword != null) {
            println("Setting up Docker registry login")
            username.set(envUsername)
            password.set(envPassword)
        } else {
            println("No docker credentials defined")
        }
    }
}
