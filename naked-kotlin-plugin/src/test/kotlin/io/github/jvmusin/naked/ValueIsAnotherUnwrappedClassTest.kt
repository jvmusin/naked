package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class ValueIsAnotherUnwrappedClassTest {
    @Test
    fun test() = testExpectFail(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val a: String)

@JvmInline
@${ANNOTATION_FQN}
value class C(val b: B)

@JvmInline
@${ANNOTATION_FQN}
value class B(val a: A)

fun main() {
require(C(B(A("aba"))).b.a == C(B(A("aba"))).b.a)
}
        """.trimIndent(), "C: Sequentially nested inline value classes are not allowed"
    )
}