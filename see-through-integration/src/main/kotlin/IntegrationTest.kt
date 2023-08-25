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
    require(Inlined::hashCode.invoke(Inlined("a")) == "a".hashCode())
    require(Inlined::toString.invoke(Inlined("a")) == "a")
    require(Inlined::equals.invoke(Inlined("a"), Inlined("a")))
    require(!Inlined::equals.invoke(Inlined("a"), Inlined("b")))
}
