package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class NullableInnerTypeTest {
    @Test
    fun test() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String?)

fun main() {
require(A("a") != A(null))
require(A(null) != A("a"))
require(A(null) == A(null))
require(A("a") != A("b"))
require(A("a") == A("a"))
}
        """.trimIndent()
    )
}