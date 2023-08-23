/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bnorm.template

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class IrPluginTest {
    @Test
    fun `IR plugin success`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt", """
//annotation class DebugLog
//
//fun main() {
//    println(greet())
//    println(greet(name = "Kotlin IR"))
//}
//
//@DebugLog
//fun greet(greeting: String = "Hello", name: String = "World"): String {
//    Thread.sleep(15)
//    return "${'$'}greeting, ${'$'}name!"
//}

annotation class InlineThisClass

@JvmInline
@InlineThisClass
value class Holder(val theValue: String)

fun <H : Holder> foo(holder: Holder): Holder {
   return holder
}

fun Holder.happyEquals(other: Holder) = this == other 

class Outside {
  var hNullable: Holder? = Holder("aba")
  var hNormal: Holder = Holder("abad")
}

fun main() {
  val hStr = "a"
  val hObj = Holder(hStr)
  foo<Holder>(hObj)
  val hStrHash = hStr.hashCode()
  val hObjHash = hObj.hashCode()
  println(hStrHash)
  println(hObjHash)
  val hStrToString = hStr.toString()
  val hObjToString = hObj.toString()

  val hStrEq = hStr.equals(hStr)
  val hObjEq = hObj.equals(hObj)

  val extracted = hObj.theValue

  val m = listOf<Holder>()

  require(hObj.happyEquals(hObj))

  hObj == hObj

  fun Holder.local(other: Holder): Holder {
    return Holder(theValue + other.theValue)
  }

  require(Holder("aba").local(Holder("caba")) == Holder("abacaba"))
  require(Outside().hNormal == Holder("abad"))
  require(Outside().hNullable == Holder("aba"))
  val o = Outside()
  o.hNullable = null
  require(o.hNullable == null)
}

""".trimIndent()
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val kClazz = result.classLoader.loadClass("MainKt")
        val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }


    @Test
    fun testOverriddenMethods() {
        test(
            """
annotation class InlineThisClass

@JvmInline
@InlineThisClass
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
annotation class InlineThisClass

@JvmInline
@InlineThisClass
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
annotation class InlineThisClass

@JvmInline
@InlineThisClass
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
annotation class InlineThisClass

@JvmInline
@InlineThisClass
value class A(val value: String)

fun main() {
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
annotation class InlineThisClass

@JvmInline
@InlineThisClass
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
annotation class InlineThisClass

@JvmInline
@InlineThisClass
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
annotation class InlineThisClass

@JvmInline
@InlineThisClass
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
    fun testFunctionArgument() {
        test(
            """
annotation class InlineThisClass

@JvmInline
@InlineThisClass
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
    fun testBulk() {
        test(
            """
annotation class InlineThisClass

@JvmInline
@InlineThisClass
value class Holder(val theValue: String)

fun <H : Holder> foo(holder: Holder): Holder {
   return holder
}

fun Holder.happyEquals(other: Holder) = this == other 

class Outside {
  var hNullable: Holder? = Holder("aba")
  var hNormal: Holder = Holder("abad")
}

fun main() {
  val hStr = "a"
  val hObj = Holder(hStr)
  foo<Holder>(hObj)
  val hStrHash = hStr.hashCode()
  val hObjHash = hObj.hashCode()
  println(hStrHash)
  println(hObjHash)
  val hStrToString = hStr.toString()
  val hObjToString = hObj.toString()

  val hStrEq = hStr.equals(hStr)
  val hObjEq = hObj.equals(hObj)

  val extracted = hObj.theValue

  val m = listOf<Holder>()

  require(hObj.happyEquals(hObj))

  hObj == hObj

  fun Holder.local(other: Holder): Holder {
    return Holder(theValue + other.theValue)
  }

  require(Holder("aba").local(Holder("caba")) == Holder("abacaba"))
  require(Outside().hNormal == Holder("abad"))
  require(Outside().hNullable == Holder("aba"))
  val o = Outside()
  o.hNullable = null
  require(o.hNullable == null)
}
    """.trimIndent()
        )
    }

    @Test
    fun testNestedClasses() = test(
        """    
annotation class InlineThisClass

@JvmInline
@InlineThisClass
value class A(val a: String)

@JvmInline
@InlineThisClass
value class C(val b: B)

@JvmInline
@InlineThisClass
value class B(val a: A)

fun main() {
require(C(B(A("aba"))).b.a == C(B(A("aba"))).b.a)
}
    """.trimIndent()
    )
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
    plugin: ComponentRegistrar = TemplateComponentRegistrar(),
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
    plugin: ComponentRegistrar = TemplateComponentRegistrar(),
): KotlinCompilation.Result {
    return compile(listOf(sourceFile), plugin)
}
