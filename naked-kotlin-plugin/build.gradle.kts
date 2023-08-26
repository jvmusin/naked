import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
    id("maven-publish")
    id("signing")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions {
        freeCompilerArgs += "-Xopt-in=org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI"
        freeCompilerArgs += "-Xopt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi"
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    kapt("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    testImplementation(project(":naked-annotation"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable") // also kotlin?
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
}

buildConfig {
    packageName(group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
}

publishing {
    publications {
        create<MavenPublication>("nakedKotlinPlugin") {
            from(components["kotlin"])
            pom {
                name = "Naked Kotlin Compiler Plugin"
                description = "Kotlin Compiler Plugin which allows inlining value-class in Kotlin with just a single annotation."
                url = "https://github.com/jvmusin/naked"
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

signing {
    sign(publishing.publications["nakedKotlinPlugin"])
}