import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
    id("com.vanniktech.maven.publish") version "0.25.3"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

tasks.withType<KotlinCompile> {
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
    testImplementation(kotlin("test"))
    testImplementation(kotlin("compiler-embeddable"))
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
}

tasks.test {
    useJUnitPlatform()
}

buildConfig {
    packageName(group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)

    signAllPublications()

    coordinates(group.toString(), project.name, "$version")

    pom {
        name = "Naked Kotlin Compiler Plugin"
        description =
            "Kotlin Compiler Plugin which allows inlining value-classes in Kotlin with just a single annotation."
        inceptionYear = "2023"
        url = "https://github.com/jvmusin/naked"
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("jvmusin")
                name.set("Rustam Musin")
                url.set("https://github.com/jvmusin")
            }
        }
        scm {
            url.set("https://github.com/jvmusin/naked")
            connection.set("scm:git:git://github.com/jvmusin/naked.git")
            developerConnection.set("scm:git:ssh://git@github.com/jvmusin/naked.git")
        }
    }
}