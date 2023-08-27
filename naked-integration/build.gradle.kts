plugins {
    kotlin("jvm") version "1.9.10"
    id("io.github.jvmusin.naked") version "0.0.1"
}

repositories {
    mavenCentral()
}

naked {
    enabled = true
}
