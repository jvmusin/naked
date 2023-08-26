plugins {
    kotlin("jvm") version "1.9.0"
    id("dev.jvmusin.naked") version "0.0.1"
}

repositories {
    mavenCentral()
}

naked {
    enabled = true
}
