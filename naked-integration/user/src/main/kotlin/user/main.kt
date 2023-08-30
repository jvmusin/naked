package user

import api.MyDataClass
import api.MyValueClass

fun main() {
    val word = "abacaba"
    println(MyValueClass(word))
    println("Is the word written - $word?")
    val value = MyValueClass(word)
    val data = MyDataClass(value)
    println(data)
    println(value.hashCode())
    println(data.hashCode())
}