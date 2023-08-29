package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class GetValueTest {
    @Test
    fun test() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  require(A("aba").value == "aba")
}
        """.trimIndent()
    )
}
