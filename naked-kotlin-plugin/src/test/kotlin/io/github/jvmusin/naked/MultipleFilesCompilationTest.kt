package io.github.jvmusin.naked

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.Test
import kotlin.test.assertEquals

class MultipleFilesCompilationTest {
    @Test
    fun testTwoFiles() {
        val file1 = """
            package a
            
            @JvmInline
            @${ANNOTATION_FQN}
            value class V(val value: String)
        """.trimIndent()

        val file2 = """
            package b
            
            import a.V
            
            fun main() {
              print(V("hello"))
            }
        """.trimIndent()

        val result = compile(
            listOf(
                SourceFile.kotlin("V.kt", file1),
                SourceFile.kotlin("main.kt", file2),
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}