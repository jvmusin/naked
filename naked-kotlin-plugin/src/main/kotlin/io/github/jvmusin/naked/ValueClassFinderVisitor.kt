package io.github.jvmusin.naked

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ValueClassFinderVisitor(
    private val annotationClass: IrClassSymbol,
) : IrElementVisitorVoid {
    private val result = mutableListOf<ClassThings>()

    fun findClasses(moduleFragment: IrModuleFragment): List<ClassThings> {
        moduleFragment.acceptVoid(this)
        return getFoundClassesSortedFromLeafs()
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation(annotationClass)) {
            require(declaration.isValue)
            val property = declaration.declarations.filterIsInstance<IrProperty>().single()
            val getProperty = property.getter!!
            val innerType = getProperty.returnType.also { require(!it.isPrimitiveType()) }
            val constructor = declaration.constructors.single()
            result += ClassThings(
                declaration.symbol,
                declaration.defaultType,
                getProperty.symbol,
                constructor.symbol,
                innerType,
            )
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
