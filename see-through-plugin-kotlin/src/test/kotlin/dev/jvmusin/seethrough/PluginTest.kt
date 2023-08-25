package dev.jvmusin.seethrough

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class PluginTest {
    @Test
    fun testOverriddenMethods() {
        test(
            """
@JvmInline
@SeeThrough
value class A(val value: String)

fun main() {
  require(A("a") == A("a"))
  require(A("a").equals(A("a")))
  require(A("a").hashCode() == 97)
  require(A("a").toString() == "a")
}
        """.trimIndent()
        )
    }

    @Test
    fun testTypeParameters() {
        test(
            """

@JvmInline
@SeeThrough
value class A(val value: String)

fun <T : A> T.foo(a: T): T = A(value + a.value) as T

fun main() {
  require(A("a").foo(A("b")) == A("ab"))
}
        """.trimIndent()
        )
    }

    @Test
    fun testClassField() {
        test(
            """

@JvmInline
@SeeThrough
value class A(val value: String)

data class D(val a: A) {
  var b: A = A("b")
}

fun main() {
  require(D(A("a")).b.value == "b")
  val d = D(A("a"))
  d.b = A("c")
  require(d.b == A("c"))
}
        """.trimIndent()
        )
    }

    @Test
    fun testClassCalls() {
        test(
            """
@JvmInline
@SeeThrough
value class A(val value: String)

fun main() {
  @Suppress("RemoveRedundantQualifierName", "RedundantSuppression")
  require(A::class == kotlin.String::class)
  require(A::class.java == java.lang.String::class.java)
}
        """.trimIndent()
        )
    }

    @Test
    @Ignore("Companion objects and nested things are not supported yet")
    fun testCompanionObject() {
        test(
            """
@JvmInline
@SeeThrough
value class A(val value: String) {
  companion object {
    val B = A("b")
  }
}

fun main() {
  println(A.B)
}
        """.trimIndent()
        )
    }


    @Test
    fun testTypeAlias() {
        test(
            """

@JvmInline
@SeeThrough
value class A(val value: String)

typealias B = A

fun <T : B> T.foo(a: T): T = B(value + a.value) as T

fun main() {
  require(B("a").foo(B("b")) == B("ab"))
}
        """.trimIndent()
        )
    }

    @Test
    fun testNullableValue() {
        test(
            """

@JvmInline
@SeeThrough
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

    @Test
    fun testMethodReference() {
        test(
            """
@JvmInline
@SeeThrough
value class A(val value: String)

fun f(constructor: (String) -> A) = constructor

fun main() {
  require(f(::A)("aba") == A("aba"))
}
            """.trimIndent()
        )
    }

    @Test
    fun testFunctionArgument() {
        test(
            """
@JvmInline
@SeeThrough
value class A(val value: String)

fun foo1(arg: A): A {
  return A(arg.value + arg.value)
}
fun foo2(arg: A): A = A(arg.value + arg.value)
fun foo3(arg: A) = A(arg.value + arg.value)
fun A.foo4() = A(value + value)
fun A.foo5(arg: A) = A(value + arg.value)

fun main() {
  require(foo1(A("a")) == A("aa"))
  require(foo2(A("a")) == A("aa"))
  require(foo3(A("a")) == A("aa"))
  require(A("a").foo4() == A("aa"))
  require(A("a").foo5(A("b")) == A("ab"))
}
        """.trimIndent()
        )
    }

    @Test
    fun testNestedClasses() = test(
        """    
@JvmInline
@SeeThrough
value class A(val a: String)

@JvmInline
@SeeThrough
value class C(val b: B)

@JvmInline
@SeeThrough
value class B(val a: A)

fun main() {
require(C(B(A("aba"))).b.a == C(B(A("aba"))).b.a)
}
    """.trimIndent()
    )

    @Test
    fun testTwoFiles() {
        val result = compile(
            sourceFiles = listOf(
                SourceFile.kotlin(
                    "IntegrationTest.kt",
                    """
import SeeThrough




@JvmInline
@SeeThrough
value class Inlined(val value: String)

@JvmInline
value class NonInlined(val value: String)

fun main() {

}
""".trimIndent()
                ),
                SourceFile.kotlin(
                    "sub/other.kt",
                    """
package sub

import Inlined




fun main() {
    val i = Inlined("")
}
""".trimIndent()
                )
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}

fun test(@Language("kotlin") sourceCode: String) {
    val result = compile(sourceFile = SourceFile.kotlin("main.kt", sourceCode))

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

    val kClazz = result.classLoader.loadClass("MainKt")
    val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
    main.invoke(null)
}

fun compile(
    sourceFiles: List<SourceFile>,
    plugin: ComponentRegistrar = SeeThroughComponentRegistrar(),
): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles
        useIR = true
        componentRegistrars = listOf(plugin)
        inheritClassPath = true
    }.compile()
}

fun compile(
    sourceFile: SourceFile,
    plugin: ComponentRegistrar = SeeThroughComponentRegistrar(),
): KotlinCompilation.Result {
    return compile(listOf(sourceFile), plugin)
}
