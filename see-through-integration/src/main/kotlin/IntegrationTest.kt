import dev.jvmusin.seethrough.SeeThrough

@JvmInline
value class Wrapper(val data: String)

@JvmInline
@SeeThrough
value class InlinedWrapper(val data: String)

fun main() {
    println("Hello, ${Wrapper("World")}!") // Prints "Hello, Wrapper(data=World)!"
    println("Hello, ${InlinedWrapper("World")}!") // Prints "Hello, World!"
}
