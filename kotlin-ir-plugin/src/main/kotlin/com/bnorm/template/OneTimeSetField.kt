package com.bnorm.template

import kotlin.reflect.KProperty

class OneTimeSetField<T : Any>(private val requirement: T.() -> Boolean = { true }) {
    private var field: T? = null
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return requireNotNull(field)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        require(requirement(value))
        field = value
    }
}
