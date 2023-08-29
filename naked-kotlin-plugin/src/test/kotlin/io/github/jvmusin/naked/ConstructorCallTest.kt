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
        """.trimIndent(), "^e: .*/sources/main.kt:5:20 \\(${ANNOTATION_FQN.shortName()}\\) Constructor reference is not allowed$".toRegex()
    )
}