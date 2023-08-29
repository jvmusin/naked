package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class AsTypealiasTest {
    @Test
    fun test() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

typealias B = A

fun B.plus(other: B): B = B(value + other.value)

fun main() {
  require(B("a").plus(B("b")) == B("ab"))
}
        """.trimIndent()
    )
}