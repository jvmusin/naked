package dev.jvmusin.seethrough

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class ValueClassFinderVisitor(
    private val annotationClass: IrClassSymbol
) : IrElementVisitorVoid {
    private val stack = ArrayDeque<ClassThings>()
    private val result = mutableListOf<ClassThings>()

    fun getFoundClassesSortedFromLeafs(): List<ClassThings> {
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

    private inline fun update(action: ClassThings.() -> Unit) = stack.lastOrNull()?.action()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation(annotationClass)) {
            require(declaration.isValue)
            stack += ClassThings(declaration.symbol.defaultType)
            super.visitClass(declaration)
            result += stack.removeLast()
            return
        }
        super.visitClass(declaration)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        if (declaration.returnType == stack.lastOrNull()?.classType) {
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

    inner class ClassThings(val classType: IrType) {
        var getSymbol: IrFunctionSymbol by OneTimeSetField()
        var constructorSymbol: IrConstructorSymbol by OneTimeSetField()
        var innerType: IrType by OneTimeSetField { !isPrimitiveType() }
    }
}
