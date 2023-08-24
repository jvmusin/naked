@JvmInline
@SeeThrough
value class Inlined(val value: String)

@JvmInline
value class NonInlined(val value: String)

interface Base {
    val v: Inlined
}

fun forEach(action: (i: Inlined, info: Unit) -> Unit) {
    listOf<Inlined>(Inlined("rr")).forEach { action(it, Unit) }
}

class Derived(override val v: Inlined) : Base

fun main() {
    println(Derived(Inlined("23")))
    forEach { i, info -> println(i) }
}
