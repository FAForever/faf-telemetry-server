import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.kotlin.kapt") version "1.7.20"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.micronaut.application") version "3.6.2"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
}

version = "0.1"
group = "com.faforever.ice"

val kotlinVersion = project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    kapt("io.micronaut:micronaut-http-validation")
    implementation("io.micronaut:micronaut-websocket")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    runtimeOnly("ch.qos.logback:logback-classic")
    implementation("io.micronaut:micronaut-validation")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
}

application {
    mainClass.set("com.faforever.ice.ApplicationKt")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

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
