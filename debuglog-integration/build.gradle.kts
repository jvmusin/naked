plugins {
    kotlin("jvm") version "1.9.0"
    id("com.bnorm.template.kotlin-ir-plugin") version "0.1.0-SNAPSHOT"
}

repositories {
    mavenCentral()
}

template {
    stringProperty.set("qw")
    fileProperty.set(file("."))
}
