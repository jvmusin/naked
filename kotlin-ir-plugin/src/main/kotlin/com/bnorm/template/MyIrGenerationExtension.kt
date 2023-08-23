package com.bnorm.template

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.remapTypes
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName

class MyIrGenerationExtension(private val messageCollector: MessageCollector, private val annotationFqn: FqName) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val annotationClass = requireNotNull(pluginContext.referenceClass(annotationFqn)) {
            "Annotation class $annotationFqn not found."
        }

        messageCollector.report(CompilerMessageSeverity.INFO, moduleFragment.dump())
        WholeVisitor(messageCollector, pluginContext, annotationClass).run(moduleFragment)
        messageCollector.report(CompilerMessageSeverity.INFO, moduleFragment.dump())
    }
}

class WholeVisitor(
        private val messageCollector: MessageCollector,
        private val pluginContext: IrPluginContext,
        private val annotationClass: IrClassSymbol,
) {
    fun run(moduleFragment: IrModuleFragment) {
        val classFinder = ValueClassFinderVisitor(messageCollector, annotationClass)
        moduleFragment.acceptVoid(classFinder)

        for ((classType, data) in classFinder.foundClasses) {
            moduleFragment.transform(ConstructorTransformer(data.constructorSymbol), null)
            moduleFragment.transform(OverriddenFunctionCallTransformer(data.getSymbol), null)

            // TODO: Check nullable backing fields
            moduleFragment.transform(TypeRemapperTransformer(classType, data.innerValueType), null)
            moduleFragment.transform(TypeRemapperTransformer(classType.makeNullable(), data.innerValueType.makeNullable()), null)
        }
    }

    inner class ConstructorTransformer(private val constructorSymbol: IrConstructorSymbol) : IrElementTransformerVoidWithContext() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            if (expression.symbol == constructorSymbol) {
                return visitExpression(expression.valueArguments.single()!!)
            }
            return super.visitConstructorCall(expression)
        }
    }

    inner class TypeRemapperTransformer(private val classType: IrType, private val innerValueType: IrType) : IrElementTransformerVoidWithContext() {
        private val typeRemapper = object : TypeRemapper {
            override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
            }

            override fun leaveScope() {
            }

            override fun remapType(type: IrType): IrType {
                if (type == classType) return innerValueType
                if (type !is IrSimpleType) return type
                return type.buildSimpleType {
                    arguments = arguments.map {
                        when (it) {
                            is IrType -> remapType(it) as IrTypeArgument
                            else -> it
                        }
                    }
                }
            }
        }

        override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
            declaration.remapTypes(typeRemapper)
            return super.visitDeclaration(declaration)
        }
    }

    inner class OverriddenFunctionCallTransformer(private val getSymbol: IrFunctionSymbol) : IrElementTransformerVoidWithContext() {
        private val replacements = run {
            val replacedFunctionNames = arrayOf("hashCode", "toString", "equals")
            val sourceType = "Holder"
            val targetType = "kotlin.String"
            fun fn(type: String, f: String) = pluginContext.referenceFunctions(FqName("${type}.${f}")).single()
            replacedFunctionNames.associate { func ->
                fn(sourceType, func) to fn(targetType, func)
            }
        }

        override fun visitCall(expression: IrCall): IrExpression {
            if (expression.symbol == getSymbol) {
                return visitExpression(expression.dispatchReceiver!!)
            }
            replacements[expression.symbol]?.let { replacement ->
                val replacedCall = DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol).irCall(replacement).also { call ->
                    call.dispatchReceiver = expression.dispatchReceiver
                    repeat(expression.valueArgumentsCount) { arg ->
                        call.putValueArgument(arg, expression.getValueArgument(arg))
                    }
                }
                return visitCall(replacedCall)
            }
            return super.visitCall(expression)
        }
    }
}

data class ValueClassFields(
        val getSymbol: IrFunctionSymbol,
        val constructorSymbol: IrConstructorSymbol,
        val innerValueType: IrType,
)

class ValueClassFinderVisitor(private val messageCollector: MessageCollector, private val annotationClass: IrClassSymbol) : IrElementVisitorVoid {
    val foundClasses = mutableMapOf<IrType, ValueClassFields>()

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation(annotationClass)) {
            val visitor = ClassVisitor()
            declaration.acceptVoid(visitor)
            foundClasses[declaration.symbol.defaultType] = ValueClassFields(visitor.getSymbol!!, visitor.constructorSymbol!!, visitor.innerType!!)
        }
        super.visitClass(declaration)
    }

    inner class ClassVisitor : IrElementVisitorVoid {
        var getSymbol: IrFunctionSymbol? = null
        var constructorSymbol: IrConstructorSymbol? = null
        var innerType: IrType? = null

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitConstructor(declaration: IrConstructor) {
            require(constructorSymbol == null)
            constructorSymbol = declaration.symbol

            val innerType = declaration.valueParameters.single().type
            require(!innerType.isPrimitiveType())
            this.innerType = innerType

            super.visitConstructor(declaration)
        }

        override fun visitFunction(declaration: IrFunction) {
            // TODO: Use some other method
            if (declaration.name.asString().contains("get-")) {
                require(getSymbol == null)
                getSymbol = declaration.symbol
            }
            super.visitFunction(declaration)
        }
    }
}
