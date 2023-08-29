package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class UseAsClassFieldTest {
    @Test
    fun test() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

data class Data(var a: A, val b: String, val c: Int)

fun main() {
  val d = Data(A("aba"), "caba", 42)
  require(d.a.toString() == "aba")
  d.a = A("qwerty")
  require(d.a == A("qwerty"))
}
        """.trimIndent()
    )
}