import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  kotlin("kapt")
  id("com.github.gmazzo.buildconfig")
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

  kapt("com.google.auto.service:auto-service:1.1.1")
  compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

  testImplementation(project(":debuglog-annotation"))
  testImplementation(kotlin("test-junit"))
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
}

buildConfig {
  packageName(group.toString())
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs += "-Xopt-in=org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI"
    freeCompilerArgs += "-Xopt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi"
  }
}