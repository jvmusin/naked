import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}