package dev.jvmusin.seethrough

import kotlin.reflect.KProperty

class OneTimeSetField<T : Any>(private val requirement: T.() -> Boolean = { true }) {
    private var field: T? = null
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return requireNotNull(field)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        require(requirement(value))
        require(field === null)
        field = value
    }
}
