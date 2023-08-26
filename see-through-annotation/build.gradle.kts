import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") version "0.25.3"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.DEFAULT)

    signAllPublications()

    coordinates(group.toString(), project.name, version.toString())

    pom {
        name = "SeeThrough Compiler Plugin Annotation"
        description = "Annotation for the SeeThrough Kotlin Compiler Plugin " +
                "which is used for marking value classes to inline."
        inceptionYear = "2023"
        url = "https://github.com/jvmusin/see-through"
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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
            url.set("https://github.com/jvmusin/see-through")
            connection.set("scm:git:git://github.com/jvmusin/see-through.git")
            developerConnection.set("scm:git:ssh://git@github.com/jvmusin/see-through.git")
        }
    }
}
