import io.github.jvmusin.naked.Naked

@JvmInline
value class Wrapper(val data: String)

@JvmInline
@Naked
value class InlinedWrapper(val data: String)

fun main() {
    println("Hello, ${Wrapper("World")}!") // Prints "Hello, Wrapper(data=World)!"
    println("Hello, ${InlinedWrapper("World")}!") // Prints "Hello, World!"
}
