package dev.jvmusin.seethrough

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class TemplateGradleExtension(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
