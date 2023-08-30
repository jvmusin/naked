package io.github.jvmusin.naked

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.junit.jupiter.api.Assertions.assertEquals

fun test(@Language("kotlin") sourceCode: String) {
    val result = compile(sourceFile = SourceFile.kotlin("main.kt", sourceCode))

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

    val kClazz = result.classLoader.loadClass("MainKt")
    val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
    main.invoke(null)
}

fun compile(
    sourceFiles: List<SourceFile>,
    plugin: CompilerPluginRegistrar = NakedComponentRegistrar(),
): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles
        useIR = true
        compilerPluginRegistrars = listOf(plugin)
        inheritClassPath = true
    }.compile()
}

fun compile(
    sourceFile: SourceFile,
    plugin: CompilerPluginRegistrar = NakedComponentRegistrar(),
): KotlinCompilation.Result {
    return compile(listOf(sourceFile), plugin)
}
