package io.github.jvmusin.naked

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OverriddenObjectMethodsTest {
    @Nested
    inner class Calls {
        @Test
        fun testEqualsOperator() = test(
            """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  require(A("aba") == A("aba"))
  require(A("aba") != A("caba"))
}
            """.trimIndent()
        )

        @Test
        fun testEqualsFunction() = test(
            """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  @Suppress("ReplaceCallWithBinaryOperator", "RedundantSuppression")
  require(A("aba").equals(A("aba")))
  @Suppress("ReplaceCallWithBinaryOperator", "RedundantSuppression")
  require(!A("aba").equals(A("caba")))
}
           """.trimIndent()
        )

        @Test
        fun testHashCode() = test(
            """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  require(A("aba").hashCode() == "aba".hashCode())
}
            """.trimIndent()
        )

        @Test
        fun testToString() = test(
            """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  require(A("aba").toString() == "aba")
}
            """.trimIndent()
        )
    }

    @Nested
    inner class References {
        @Test
        fun testEquals() = test(
            """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val ref = A::equals
  require(ref(A("aba"), A("aba")))
  require(!ref(A("aba"), A("caba")))
}
            """.trimIndent()
        )

        @Test
        fun testHashCode() = test(
            """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val ref = A::hashCode
  require(ref(A("aba")) == "aba".hashCode())
}
            """.trimIndent()
        )

        @Test
        fun testToString() = test(
            """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val ref = A::toString
  require(ref(A("aba")) == "aba")
}
            """.trimIndent()
        )
    }
}