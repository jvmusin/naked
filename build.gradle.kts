buildscript {
    extra["kotlin_plugin_id"] = "dev.jvmusin.naked"
}

plugins {
    kotlin("jvm") version "1.9.10" apply false
    id("org.jetbrains.dokka") version "1.8.20" apply false
    id("com.gradle.plugin-publish") version "1.2.1" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("com.github.gmazzo.buildconfig") version "4.1.2" apply false
}

allprojects {
    group = "dev.jvmusin.naked"
    version = "0.0.1"
}

subprojects {
    repositories {
        mavenCentral()
    }
}
