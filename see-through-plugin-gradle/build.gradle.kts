import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    id("com.github.gmazzo.buildconfig")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
}

buildConfig {
    val project = project(":see-through-plugin-kotlin")
    packageName(project.group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")

    val annotationProject = project(":see-through-annotation")
    buildConfigField("String", "ANNOTATION_LIBRARY_GROUP", "\"${annotationProject.group}\"")
    buildConfigField("String", "ANNOTATION_LIBRARY_NAME", "\"${annotationProject.name}\"")
    buildConfigField("String", "ANNOTATION_LIBRARY_VERSION", "\"${annotationProject.version}\"")
}

gradlePlugin {
    website = "https://github.com/jvmusin/see-through"
    vcsUrl = "https://github.com/jvmusin/see-through"
    plugins {
        create("seeThrough") {
            id = rootProject.extra["kotlin_plugin_id"] as String
            displayName = "SeeThrough Kotlin Compiler Plugin"
            description = "Kotlin Compiler Plugin which allows to inline value classes to avoid unnecessary boxing " +
                    "when value classes are used as nullable or generic type."
            implementationClass = "dev.jvmusin.seethrough.SeeThroughGradlePlugin"
            tags = listOf("kotlin", "kotlin-compiler", "kotlin-compiler-plugin", "kotlin-plugin", "annotation")
        }
    }
}
