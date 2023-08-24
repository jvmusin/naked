buildscript {
    extra["kotlin_plugin_id"] = "dev.jvmusin.see-through"
}

plugins {
    kotlin("jvm") version "1.9.0" apply false
    id("org.jetbrains.dokka") version "1.8.20" apply false
    id("com.gradle.plugin-publish") version "1.0.0" apply false
    id("com.github.gmazzo.buildconfig") version "4.1.2" apply false
}

allprojects {
    group = "dev.jvmusin.seethrough"
    version = "0.0.1"
}

subprojects {
    repositories {
        mavenCentral()
    }
}
