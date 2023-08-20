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
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.FqName

class MyIrGenerationExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
//        moduleFragment.accept(ValueClassFinderVisitor(messageCollector), null)

        repeat(8) {
            messageCollector.report(CompilerMessageSeverity.INFO, "BEFORE:")
        }
        messageCollector.report(CompilerMessageSeverity.INFO, moduleFragment.dump())

        WholeVisitor(messageCollector, pluginContext).run(moduleFragment)

        repeat(8) {
            messageCollector.report(CompilerMessageSeverity.INFO, "AFTER:")
        }
        messageCollector.report(CompilerMessageSeverity.INFO, moduleFragment.dump())
    }
}

class WholeVisitor(
        private val messageCollector: MessageCollector,
        private val pluginContext: IrPluginContext
) {
    private val holderClass = pluginContext.referenceClass(FqName("Holder"))!!
    private fun IrType.isHolder(): Boolean = classifierOrNull == holderClass
    private val underlyingReplacementType = pluginContext.irBuiltIns.stringType as IrSimpleType
    fun <T : IrExpression> T.fixType(): T = apply { type = type.fixedType() }
    fun <T : IrValueDeclaration> T.fixType(): T = apply { type = type.fixedType() }
    fun List<IrType>.fixType() = map { it.fixedType() }
    fun IrType.fixedType(): IrType {
        if (isHolder()) return underlyingReplacementType
        if (this !is IrSimpleType) return this
        return buildSimpleType {
            arguments = arguments.map {
                if (it is IrType) {
                    it.fixedType() as IrTypeArgument
                } else {
                    it
                }
            }
        }
    }

    fun run(moduleFragment: IrModuleFragment) {
        val visitor = ValueClassFinderVisitor(messageCollector)
        moduleFragment.accept(visitor, null)
        val getSymbol = requireNotNull(visitor.getSymbol)

        moduleFragment.transform(ConstructorTransformer(), null)
        moduleFragment.transform(ExpressionTypeTransformer(), null)
        moduleFragment.transform(VariableTypeTransformer(), null)
        moduleFragment.transform(FunctionDeclarationTransformer(), null)
        moduleFragment.transform(OverriddenFunctionCallTransformer(getSymbol), null)
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
//            declaration.remapTypes(object : TypeRemapper {
//                override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
//                }
//
//                override fun leaveScope() {
//                }
//
//                override fun remapType(type: IrType): IrType = type.fixType()
//            })


            declaration.fixType()

            val type = declaration.type
            if (type is IrSimpleType) {
                declaration.type = type.buildSimpleType {
                    messageCollector.report(CompilerMessageSeverity.INFO, "hello")
                    arguments = arguments.map {
                        if (it is IrSimpleType) {
                            it.fixedType() as IrTypeArgument
                        } else it
                    }
                }
            }

            return super.visitVariable(declaration.fixType())
        }
    }

    inner class FunctionDeclarationTransformer : IrElementTransformerVoidWithContext() {
        override fun visitFunctionNew(declaration: IrFunction): IrStatement {
            declaration.valueParameters.forEach { it.fixType() }
            declaration.typeParameters.forEach { tp -> tp.superTypes = tp.superTypes.fixType() }
            if (declaration.returnType.isHolder()) declaration.returnType = underlyingReplacementType
            // TODO: check the receiver of extension methods
            return super.visitFunctionNew(declaration)
        }
    }

    inner class OverriddenFunctionCallTransformer(private val getSymbol: IrSymbol) : IrElementTransformerVoidWithContext() {
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
                return super.visitExpression(expression.dispatchReceiver!!)
            }
            replacements[expression.symbol]?.let { replacement ->
                val replacedCall = DeclarationIrBuilder(pluginContext, expression.symbol).irCall(replacement).also { call ->
                    call.dispatchReceiver = expression.dispatchReceiver
                    repeat(expression.valueArgumentsCount) { arg ->
                        call.putValueArgument(arg, expression.getValueArgument(arg))
                    }
                    repeat(expression.typeArgumentsCount) {
                        expression.putTypeArgument(it, expression.getTypeArgument(it)?.fixedType())
                    }
                }
                return super.visitCall(replacedCall)
            }
            messageCollector.report(CompilerMessageSeverity.INFO, "Function call " + expression.symbol + "#" + expression.symbol.hashCode())
            return super.visitCall(expression)
        }
    }

    inner class FunctionCallTransformer() : IrElementTransformerVoidWithContext() {
        override fun visitCall(expression: IrCall): IrExpression {
            repeat(expression.typeArgumentsCount) {
                expression.putTypeArgument(it, expression.getTypeArgument(it)?.fixedType())
            }
            return super.visitCall(expression)
        }
    }
}

class ValueClassFinderVisitor(private val messageCollector: MessageCollector) : IrElementVisitorVoid {
    var getSymbol: IrSymbol? = null

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (declaration.name.asString().contains("get-")) {
            require(getSymbol == null)
            getSymbol = declaration.symbol
        }
        super.visitFunction(declaration)
    }
}
