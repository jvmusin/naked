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
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
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

@JvmInline
value class Holder(val theValue: String)

fun <H : Holder> foo(holder: Holder): Holder {
   return holder
} // TODO: test local functions

fun Holder.happyEquals(other: Holder) = this == other 


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
}

""".trimIndent()
                )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val kClazz = result.classLoader.loadClass("MainKt")
        val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }
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
