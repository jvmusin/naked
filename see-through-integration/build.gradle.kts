plugins {
    kotlin("jvm") version "1.9.0"
    id("dev.jvmusin.see-through") version "0.0.1"
}

repositories {
    mavenCentral()
}

seeThrough {
    enabled = true
}
