package io.github.jvmusin.naked

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class NakedGradleExtension(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
