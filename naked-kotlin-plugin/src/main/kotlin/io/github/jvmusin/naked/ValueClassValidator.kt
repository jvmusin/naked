package io.github.jvmusin.naked

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*

object ValueClassValidator {
    fun validate(declaration: IrClass, messageCollector: MyMessageCollector) {
        fun check(value: Boolean, lazyMessage: () -> String) {
            if (!value) {
                messageCollector.reportError(declaration) {
                    "${declaration.name.asString()}: ${lazyMessage()}"
                }
            }
        }

        val inlineClassRepresentation = declaration.inlineClassRepresentation
        check(inlineClassRepresentation != null) {
            "This class is not an inline value class"
        }

        check(declaration.properties.count() == 1) {
            "Additional properties are not allowed, found ${declaration.properties.map { it.name }.toList()}"
        }

        val definedFunctions = declaration.functions
            .filter { it.origin == IrDeclarationOrigin.DEFINED }
            .toList()
        check(definedFunctions.isEmpty()) {
            "Defined functions are not allowed, found ${definedFunctions.map { it.name }}"
        }

        check(declaration.constructors.count() == 1) {
            "Secondary constructors are not allowed"
        }

        check(declaration.constructors.none { it.valueParameters.any(IrValueParameter::hasDefaultValue) }) {
            "Default value in constructor is not allowed"
        }

        check(declaration.superTypes.singleOrNull()?.isAny() == true) {
            "Implementing interfaces is not allowed, found ${declaration.superTypes.map { it.classFqName }}"
        }

        check(declaration.typeParameters.isEmpty()) {
            "Defining type parameters is not allowed, found ${declaration.typeParameters.map { it.name }}"
        }

        val companionObject = declaration.companionObject()
        check(companionObject == null) {
            "Companion objects not allowed, found ${companionObject!!.name}"
        }

        check(declaration.declarations.none { it is IrAnonymousInitializer }) {
            "Anonymous initializers not allowed"
        }

        val innerClasses = declaration.declarations.filterIsInstance<IrClass>()
        check(innerClasses.isEmpty()) {
            "Inner classes are not allowed, found ${innerClasses.map { it.name }}"
        }

        val underlyingType = declaration.inlineClassRepresentation?.underlyingType
        check(underlyingType?.getClass()?.hasAnnotation(ANNOTATION_FQN) != true) {
            "Sequentially nested inline value classes are not allowed"
        }
    }
}