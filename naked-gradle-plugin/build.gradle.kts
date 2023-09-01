plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    id("com.github.gmazzo.buildconfig")
    id("com.gradle.plugin-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

dependencies {
    implementation(kotlin("gradle-plugin-api"))
}

buildConfig {
    val project = project(":naked-kotlin-plugin")
    packageName(project.group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")

    val annotationProject = project(":naked-annotation")
    buildConfigField("String", "ANNOTATION_LIBRARY_GROUP", "\"${annotationProject.group}\"")
    buildConfigField("String", "ANNOTATION_LIBRARY_NAME", "\"${annotationProject.name}\"")
    buildConfigField("String", "ANNOTATION_LIBRARY_VERSION", "\"${annotationProject.version}\"")
}

gradlePlugin {
    website = "https://github.com/jvmusin/naked"
    vcsUrl = "https://github.com/jvmusin/naked"
    plugins {
        create("nakedPlugin") {
            id = rootProject.extra["kotlin_plugin_id"] as String
            displayName = "Naked Kotlin Compiler Plugin"
            description = "Kotlin Compiler Plugin which allows to inline value classes to avoid unnecessary boxing " +
                    "when value classes are used as nullable or generic type"
            implementationClass = "io.github.jvmusin.naked.NakedGradlePlugin"
            tags = listOf("kotlin", "kotlin-compiler", "kotlin-compiler-plugin", "kotlin-plugin", "annotation")
        }
    }
}
