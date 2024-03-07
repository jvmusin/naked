plugins {
    kotlin("jvm") version "1.9.23"
    id("io.github.jvmusin.naked")
}

repositories {
    mavenCentral()
}

naked {
    enabled = true
}
