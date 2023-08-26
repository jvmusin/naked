import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("signing")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

publishing {
    publications {
        create<MavenPublication>("seeThroughAnnotation") {
            from(components["kotlin"])
            pom {
                name = "SeeThrough Kotlin Compiler Plugin Annotation"
                description = "Annotation for the SeeThrough Kotlin Compiler Plugin " +
                        "which is used for marking value classes to inline."
                url = "https://github.com/jvmusin/see-through"
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["seeThroughAnnotation"])
}