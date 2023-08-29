package io.github.jvmusin.naked

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

fun testExpectFail(@Language("kotlin") sourceCode: String, expectedErrorMessage: String) {
    val result = compile(sourceFile = SourceFile.kotlin("main.kt", sourceCode))
    Assertions.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assert(expectedErrorMessage in result.messages) {
        "Not found '$expectedErrorMessage' in the output. The output:\n${result.messages}"
    }
}

fun testExpectFail(@Language("kotlin") sourceCode: String, expectedErrorMessage: Regex) {
    val result = compile(sourceFile = SourceFile.kotlin("main.kt", sourceCode))
    Assertions.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assert(result.messages.lines().any { it matches expectedErrorMessage }) {
        "Not found pattern '${expectedErrorMessage.pattern}' in the output. The output:\n${result.messages}"
    }
}

class SanityChecksTest {
    @Test
    fun onlyValueClassCanBeAnnotated() = testExpectFail(
        """
@${ANNOTATION_FQN}
class A(val value: String)
       """.trimIndent(), "A: Only value classes can be marked with the annotation $ANNOTATION_FQN"
    )

    @Test
    fun onlyOnePropertyAllowed() = testExpectFail(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  val x: Int get() = 123
}
       """.trimIndent(), "A: Additional properties are not allowed, found [value, x]"
    )

    @Test
    fun definedFunctionsNotAllowed() = testExpectFail(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  fun foo(): String = "I'm not allowed here!"
}
       """.trimIndent(), "A: Defined functions are not allowed, found [foo]"
    )

    @Test
    fun implementingInterfacesNotAllowed() = testExpectFail(
        """
interface ImplementMe
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) : ImplementMe
       """.trimIndent(), "A: Implementing interfaces is not allowed, found [ImplementMe]"
    )

    @Test
    fun typeParametersNotAllowed() = testExpectFail(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A<T>(val value: T)
       """.trimIndent(), "A: Defining type parameters is not allowed, found [T]"
    )

    @Test
    fun additionalConstructorsNotAllowed() = testExpectFail(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  constructor(x: Int) : this(x.toString())
}
       """.trimIndent(), "A: Secondary constructors are not allowed"
    )

    @Test
    fun companionObjectNotAllowed() = testExpectFail(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  companion object Alla
}
       """.trimIndent(), "A: Companion objects not allowed, found Alla"
    )

    @Test
    fun anonymousInitializersNotAllowed() = testExpectFail(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  init {
    check(value.length > 2)
  }
}
       """.trimIndent(), "A: Anonymous initializers not allowed"
    )
}
