import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") version "0.28.0"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)

    signAllPublications()

    coordinates(group.toString(), project.name, "$version")

    pom {
        name = "Naked Kotlin Compiler Plugin Annotation"
        description = "Annotation for the Naked Kotlin Compiler Plugin " +
                "which is used for marking value classes to inline."
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
