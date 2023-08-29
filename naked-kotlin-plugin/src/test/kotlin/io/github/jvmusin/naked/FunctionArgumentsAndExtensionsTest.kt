package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class FunctionArgumentsAndExtensionsTest {
    @Test
    fun oneArgument() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun f(arg: A): String {
  return arg.value + arg.value
}

fun main() {
  require(f(A("abc")) == "abcabc")
}
        """.trimIndent()
    )

    @Test
    fun mixedArguments() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun f(arg1: A, arg2: Int, arg3: A): String {
  return arg1.value + arg3.value + arg2
}

fun main() {
  require(f(A("aba"), 2, A("caba")) == "abacaba2")
}
        """.trimIndent()
    )

    @Test
    fun returnType() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun f(arg: Int): A {
  return A(arg.toString())
}

fun main() {
  require(f(543) == A("543"))
}
        """.trimIndent()
    )

    @Test
    fun receiver() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun A.f(arg: Int): A {
  return A(value + arg)
}

fun main() {
  require(A("aba").f(12) == A("aba12"))
}
    """.trimIndent()
    )

    @Test
    fun expressionStyledFunction() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun A.f(other: A) = A(value + other.value)

fun main() {
  require(A("aba").f(A("caba")) == A("abacaba"))
}
    """.trimIndent()
    )
}