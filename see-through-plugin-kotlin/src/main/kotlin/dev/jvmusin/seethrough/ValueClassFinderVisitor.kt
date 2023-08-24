package dev.jvmusin.seethrough

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ValueClassFinderVisitor(
    private val annotationClass: IrClassSymbol
) : IrElementVisitorVoid {
    private val result = mutableListOf<ClassThings>()

    fun findClasses(moduleFragment: IrModuleFragment): List<ClassThings> {
        moduleFragment.acceptVoid(this)
        return getFoundClassesSortedFromLeafs()
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    inner class Diver(private val classThings: ClassThings) : IrElementVisitorVoid {
        private inline fun update(action: ClassThings.() -> Unit) = classThings.action()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitConstructor(declaration: IrConstructor) {
            if (declaration.returnType == classThings.classType) {
                update { constructorSymbol = declaration.symbol }
                update { innerType = declaration.valueParameters.single().type }
            }
            super.visitConstructor(declaration)
        }

        override fun visitFunction(declaration: IrFunction) {
            // TODO: Use some other method
            if (declaration.name.asString().contains("get-")) {
                update { getSymbol = declaration.symbol }
            }
            super.visitFunction(declaration)
        }
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation(annotationClass)) {
            require(declaration.isValue)
            val things = ClassThings(declaration.symbol.defaultType)
            declaration.acceptVoid(Diver(things))
            result += things
            return
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

    inner class ClassThings(val classType: IrType) {
        var getSymbol: IrFunctionSymbol by OneTimeSetField()
        var constructorSymbol: IrConstructorSymbol by OneTimeSetField()
        var innerType: IrType by OneTimeSetField { !isPrimitiveType() }
    }
}
