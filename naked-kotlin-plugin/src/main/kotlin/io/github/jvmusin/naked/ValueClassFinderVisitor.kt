package io.github.jvmusin.naked

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ValueClassFinderVisitor(
    private val annotationClass: IrClassSymbol,
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector,
) : IrElementVisitorVoid {
    private val result = mutableListOf<ClassThings>()

    fun findClasses(moduleFragment: IrModuleFragment): List<ClassThings> {
        moduleFragment.acceptVoid(this)
        return getFoundClassesSortedFromLeafs()
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private fun sanityCheck(declaration: IrClass): Boolean {
        val errors = mutableListOf<String>()

        fun check(value: Boolean, lazyMessage: () -> String) {
            if (!value) errors += lazyMessage()
        }

        check(declaration.isValue) {
            "Only value classes can be marked with the annotation $ANNOTATION_FQN"
        }

        check(declaration.properties.count() == 1) {
            "Additional properties are not allowed, found ${declaration.properties.map { it.name }.toList()}"
        }

        val definedFunctions = declaration.functions.filter { it.origin == IrDeclarationOrigin.DEFINED }.toList()
        check(definedFunctions.isEmpty()) {
            "Defined functions are not allowed, found ${definedFunctions.map { it.name }}"
        }

        check(declaration.constructors.count() == 1) {
            "Secondary constructors are not allowed"
        }

        check(declaration.constructors.none { it.valueParameters.any(IrValueParameter::hasDefaultValue) }) {
            "Default value in constructor is not allowed"
        }

        check(declaration.superTypes == listOf(pluginContext.irBuiltIns.anyType)) {
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

        if (errors.isNotEmpty()) {
            for (error in errors) messageCollector.reportError(declaration, "${declaration.name.asString()}: $error")
            return false
        }

        return true
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation(annotationClass) && sanityCheck(declaration)) {
            val valueClassRepresentation = declaration.valueClassRepresentation!!
            if (valueClassRepresentation is InlineClassRepresentation) {
                result += ClassThings(
                    declaration.symbol,
                    declaration.defaultType,
                    declaration.getPropertyGetter(valueClassRepresentation.underlyingPropertyName.asString())!!,
                    declaration.constructors.single().symbol,
                    valueClassRepresentation.underlyingType,
                )
            } else {
                messageCollector.reportError(declaration, "Multi-field value class?? Tell me how you did that!")
            }
        }
        super.visitClass(declaration)
    }

    private fun getFoundClassesSortedFromLeafs(): List<ClassThings> {
        val thingsOfClass = result.associateBy { it.classType }.toMutableMap()
        val sorted = mutableListOf<ClassThings>()
        while (thingsOfClass.isNotEmpty()) {
            val type = generateSequence(thingsOfClass.keys.first()) { thingsOfClass[it]?.innerType }
                .toList().dropLast(1).last()
            val classThings = thingsOfClass.remove(type)!!
            sorted += classThings
        }
        return sorted
    }

    data class ClassThings(
        val classSymbol: IrClassSymbol,
        val classType: IrType,
        val getSymbol: IrFunctionSymbol,
        val constructorSymbol: IrConstructorSymbol,
        val innerType: IrType,
    )
}
