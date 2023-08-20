package com.bnorm.template

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.functionByName
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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.FqName

class MyIrGenerationExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.accept(ValueClassFinderVisitor(), null)
        WholeVisitor(messageCollector, pluginContext).run(moduleFragment)
        messageCollector.report(CompilerMessageSeverity.INFO, moduleFragment.dump())
    }
}

class WholeVisitor(
        private val messageCollector: MessageCollector,
        private val pluginContext: IrPluginContext
) {
    private val holderClass = pluginContext.referenceClass(FqName("Holder"))!!
    private fun IrType.isHolder(): Boolean = classifierOrNull == holderClass
    private val underlyingReplacementType = pluginContext.irBuiltIns.stringType
    private val stringHashCode = pluginContext.irBuiltIns.stringClass.functionByName("hashCode")
    fun <T : IrExpression> T.fixType(): T = apply { if (type.isHolder()) type = underlyingReplacementType }
    fun <T : IrValueDeclaration> T.fixType(): T = apply { if (type.isHolder()) type = underlyingReplacementType }
    fun List<IrType>.fixType(): List<IrType> = map { if (it.isHolder()) underlyingReplacementType else it }

    fun run(moduleFragment: IrModuleFragment) {
        moduleFragment.transform(ConstructorTransformer(), null)
        moduleFragment.transform(ExpressionTypeTransformer(), null)
        moduleFragment.transform(VariableTypeTransformer(), null)
        moduleFragment.transform(FunctionDeclarationTransformer(), null)
        moduleFragment.transform(FunctionCallTransformer(), null)
    }

    inner class ConstructorTransformer : IrElementTransformerVoidWithContext() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            if (expression.type.isHolder()) {
                return visitExpression(expression.valueArguments.single()!!)
            }
            return super.visitConstructorCall(expression)
        }
    }

    inner class ExpressionTypeTransformer : IrElementTransformerVoidWithContext() {
        override fun visitExpression(expression: IrExpression): IrExpression {
            return super.visitExpression(expression.fixType())
        }
    }

    inner class VariableTypeTransformer : IrElementTransformerVoidWithContext() {
        override fun visitVariable(declaration: IrVariable): IrStatement {
            return super.visitVariable(declaration.fixType())
        }
    }

    inner class FunctionDeclarationTransformer : IrElementTransformerVoidWithContext() {
        override fun visitFunctionNew(declaration: IrFunction): IrStatement {
            declaration.valueParameters.forEach { it.fixType() }
            declaration.typeParameters.forEach { tp -> tp.superTypes = tp.superTypes.fixType() }
            if (declaration.returnType.isHolder()) declaration.returnType = underlyingReplacementType
            return super.visitFunctionNew(declaration)
        }
    }

    inner class FunctionCallTransformer : IrElementTransformerVoidWithContext() {
        private val holderHashCode = pluginContext.referenceFunctions(FqName("Holder.hashCode")).single()
        private val stringHashCode = pluginContext.referenceFunctions(FqName("kotlin.String.hashCode")).single()

        private val holderToString = pluginContext.referenceFunctions(FqName("Holder.toString")).single()
        private val stringToString = pluginContext.referenceFunctions(FqName("kotlin.String.toString")).single()

        private val holderEquals = pluginContext.referenceFunctions(FqName("Holder.equals")).single()
        private val stringEquals = pluginContext.referenceFunctions(FqName("kotlin.String.equals")).single()

        override fun visitCall(expression: IrCall): IrExpression {
            if (expression.symbol == holderHashCode) {
                val replacedCall = DeclarationIrBuilder(pluginContext, expression.symbol).irCall(stringHashCode).also { call ->
                    call.dispatchReceiver = expression.dispatchReceiver
                }
                return super.visitCall(replacedCall)
            }
            if (expression.symbol == holderToString) {
                val replacedCall = DeclarationIrBuilder(pluginContext, expression.symbol).irCall(stringToString).also { call ->
                    call.dispatchReceiver = expression.dispatchReceiver
                }
                return super.visitCall(replacedCall)
            }
            if (expression.symbol == holderEquals) {
                val replacedCall = DeclarationIrBuilder(pluginContext, expression.symbol).irCall(stringEquals).also { call ->
                    call.dispatchReceiver = expression.dispatchReceiver
                    call.putValueArgument(0, expression.getValueArgument(0))
                }
                return super.visitCall(replacedCall)
            }
            return super.visitCall(expression)
        }
    }
}

class ValueClassFinderVisitor : IrElementVisitorVoid {
    override fun visitClass(declaration: IrClass, data: Nothing?) {
        if (declaration.isValue) {
            return super.visitClass(declaration, data)
        }
        return super.visitClass(declaration, data)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (declaration.name.asString() == "hashCode") {
            return declaration.accept(HashCodeVisitor(), null)
        }
        super.visitFunction(declaration)
    }

    class HashCodeVisitor : IrElementVisitorVoid {
        override fun visitCall(expression: IrCall) {
            super.visitCall(expression)
        }
    }
}
