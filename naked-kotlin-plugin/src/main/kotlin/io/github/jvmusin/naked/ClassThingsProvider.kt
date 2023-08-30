package io.github.jvmusin.naked

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.util.*

object ClassThingsProvider {
    fun getClassThings(declaration: IrClass, messageCollector: MyMessageCollector): ClassThings? {
        if (!declaration.hasAnnotation(ANNOTATION_FQN)) return null

        val functions = declaration.functions

        messageCollector.report {
            "For class ${declaration.name} found functions ${functions.map { it.name }.toList()}"
        }

        val valueClassRepresentation = declaration.inlineClassRepresentation!!
        return ClassThings(
            declaration.symbol,
            declaration.defaultType,
            declaration.getPropertyGetter(valueClassRepresentation.underlyingPropertyName.asString())!!,
            declaration.constructors.single().symbol,
            valueClassRepresentation.underlyingType,
        )
    }
}
