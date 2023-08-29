package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class MultipleClassesCompilationTest {
    @Test
    fun twoClassesInSameFile() = test("""
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

@JvmInline
@${ANNOTATION_FQN}
value class B(val value: String)

fun main() {
  val a = A("aba")
  val b = B("caba")
  require(a == A("aba"))
  require(b == B("caba"))
}
    """.trimIndent())
}