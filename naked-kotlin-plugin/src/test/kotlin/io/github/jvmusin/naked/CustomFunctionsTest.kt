package io.github.jvmusin.naked

import kotlin.test.Ignore
import kotlin.test.Test

@Ignore("Custom functions inside value classes are not allowed, covered with tests")
class CustomFunctionsTest {
    @Test
    fun testImplicitReturnType() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  fun foo() = A(value + "-foo")
}

fun main() {
  require(A("hello").foo() == A("hello-foo"))
}
    """.trimIndent()
    )

    @Test
    fun testExplicitReturnType() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  @Suppress("ConvertToStringTemplate", "RedundantSuppression")
  fun foo(): A = A(value + "-foo")
}

fun main() {
  require(A("hello").foo() == A("hello-foo"))
}
    """.trimIndent()
    )

    @Test
    fun withParametersImplicitReturnType() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  fun foo(p: A, x: Int) = A(value + p + x)
}

fun main() {
  require(A("hello").foo(A("-foo"), 42) == A("hello-foo42"))
}
        """.trimIndent()
    )

    @Test
    fun withParametersExplicitReturnType() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String) {
  fun foo(p: A, x: Int): A = A(value + p + x)
}

fun main() {
  require(A("hello").foo(A("-foo"), 42) == A("hello-foo42"))
}
        """.trimIndent()
    )
}