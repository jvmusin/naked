@JvmInline
@InlineThisClass
value class Holder(val value: String)

fun main() {
    println("Hello, ${Holder("world")}!") // Prints "Hello, world!"
}
