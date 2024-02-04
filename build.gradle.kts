import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    extra["kotlin_plugin_id"] = "io.github.jvmusin.naked"
}

plugins {
    kotlin("jvm") version "1.9.10" apply false
    id("org.jetbrains.dokka") version "1.8.20" apply false
    id("com.gradle.plugin-publish") version "1.2.1" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("com.github.gmazzo.buildconfig") version "5.3.5" apply false
}

allprojects {
    group = "io.github.jvmusin.naked"
    version = "0.0.4"
}

subprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}
