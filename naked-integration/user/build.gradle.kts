plugins {
    kotlin("jvm") version "1.9.23"
    id("io.github.jvmusin.naked")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
}