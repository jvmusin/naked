package io.github.jvmusin.naked

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UseInCollectionsTest {
    @Nested
    inner class Arrays {
        @Nested
        inner class Instantiation {
            @Test
            fun empty() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val array = arrayOf<A>()
}
                """.trimIndent()
            )

            @Test
            fun explicitType() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  @Suppress("RemoveExplicitTypeArguments", "RedundantSuppression")
  val array = arrayOf<A>(A("aba"))
}
            """.trimIndent()
            )

            @Test
            fun implicitType() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val array = arrayOf(A("aba"))
}
            """.trimIndent()
            )

            @Test
            fun withGenerator() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val array = Array(2) { A(it.toString()) }
}
                """.trimIndent()
            )
        }

        @Nested
        inner class Usage {
            @Test
            fun test() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val array = Array(2) { A(it.toString()) }
  require(array[0] == A("0"))
  require(array[1] == A("1"))
  array[0] = A("aba")
  require(array[0] == A("aba"))
  require(array.joinToString(", ") == "aba, 1")
  array[0] = A("1")
  array.forEach { x -> require(x == A("1")) }
  for (x in array) require(x == A("1"))
}
                """.trimIndent()
            )
        }
    }

    @Nested
    inner class Lists {
        @Nested
        inner class Instantiation {
            @Test
            fun empty() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val list = listOf<A>()
}
                """.trimIndent()
            )

            @Test
            fun explicitType() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  @Suppress("RemoveExplicitTypeArguments", "RedundantSuppression")
  val list = listOf<A>(A("aba"))
}
            """.trimIndent()
            )

            @Test
            fun implicitType() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val list = listOf(A("aba"))
}
            """.trimIndent()
            )

            @Test
            fun withGenerator() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val list = List(2) { A(it.toString()) }
}
                """.trimIndent()
            )
        }

        @Nested
        inner class Usage {
            @Test
            fun test() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val list = MutableList(2) { A(it.toString()) }
  require(list[0] == A("0"))
  require(list[1] == A("1"))
  list[0] = A("aba")
  require(list[0] == A("aba"))
  require(list.joinToString(", ") == "aba, 1")
  list[0] = A("1")
  list.forEach { x -> require(x == A("1")) }
  for (x in list) require(x == A("1"))
}
                """.trimIndent()
            )
        }
    }

    @Nested
    inner class Maps {
        @Nested
        inner class Instantiation {
            @Test
            fun empty() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val map1 = mapOf<A, Int>()
  val map2 = mapOf<Int, A>()
}
                """.trimIndent()
            )

            @Test
            fun explicitType() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  @Suppress("RemoveExplicitTypeArguments", "RedundantSuppression")
  val map1 = mapOf<A, Int>(A("aba") to 123)
  val map2 = mapOf<Int, A>(123 to A("caba"))
}
            """.trimIndent()
            )

            @Test
            fun implicitType() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val map1 = mapOf(A("aba") to "caba")
  val map2 = mapOf("aba" to A("caba"))
}
            """.trimIndent()
            )
        }

        @Nested
        inner class Usage {
            @Test
            fun testWhenKeyIsValueClass() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val map = mutableMapOf<A, Int>()
  map[A("aba")] = 123
  require(map[A("aba")] == 123)

  map[A("aba")] = 456
  require(map[A("aba")] == 456)

  for ((k, v) in map) {
    require(k == A("aba"))
    @Suppress("KotlinConstantConditions", "RedundantSuppression") // TODO: WTF?? Report this
    require(v == 456)
  }

  map.forEach { (k, v) -> 
    require(k == A("aba"))
    @Suppress("KotlinConstantConditions", "RedundantSuppression") // TODO: WTF?? Report this
    require(v == 456)
  }

  map.forEach { k, v -> 
    require(k == A("aba"))
    @Suppress("KotlinConstantConditions", "RedundantSuppression") // TODO: WTF?? Report this
    require(v == 456)
  }
}
                """.trimIndent()
            )

            @Test
            fun testWhenValueIsValueClass() = test(
                """
@JvmInline
@${ANNOTATION_FQN}
value class A(val value: String)

fun main() {
  val map = mutableMapOf<Int, A>()
  map[123] = A("aba")
  require(map[123] == A("aba"))

  map[123] = A("caba")
  require(map[123] == A("caba"))

  for ((k, v) in map) {
    @Suppress("KotlinConstantConditions", "RedundantSuppression") // TODO: WTF?? Report this
    require(k == 123)
    require(v == A("caba"))
  }

  map.forEach { (k, v) ->
    @Suppress("KotlinConstantConditions", "RedundantSuppression") // TODO: WTF?? Report this
    require(k == 123)
    require(v == A("caba"))
  }

  map.forEach { k, v ->
    @Suppress("KotlinConstantConditions", "RedundantSuppression") // TODO: WTF?? Report this
    require(k == 123)
    require(v == A("caba"))
  }
}
                """.trimIndent()
            )
        }
    }
}