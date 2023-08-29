package io.github.jvmusin.naked

import org.junit.jupiter.api.Test

class ClassMethodTest {
    @Test
    fun classCallsReturnUnderlyingType() = test(
        """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

@Suppress("RemoveRedundantQualifierName", "RedundantSuppression")
fun main() {
  require(A::class == kotlin.String::class)
  require(A("aba")::class == kotlin.String::class)
}
        """.trimIndent()
    )
}
