package api

import io.github.jvmusin.naked.Naked

@JvmInline
@Naked
value class MyValueClass(val value: String)

data class MyDataClass(val value: MyValueClass)