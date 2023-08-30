package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class ConstructorCallTest {
    @Test
    fun testNormalCall() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  require(A("aba") == A("aba"))
}
        """.trimIndent()
    )

    @Test
    fun testReferenceCall() = testExpectFail(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val constructor = ::A
  require(constructor("aba") == A("aba"))
}
        """.trimIndent(), "Constructor reference is not allowed"
    )
}