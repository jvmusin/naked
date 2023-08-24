@JvmInline
@SeeThrough
value class Inlined(val value: String)

@JvmInline
value class NonInlined(val value: String)

fun main() {
    println("Hello, ${Inlined("world")}!") // Prints "Hello, world!"
    println("Hello, ${NonInlined("world")}!") // Prints "Hello, NonInlined(value=world)!"
}
