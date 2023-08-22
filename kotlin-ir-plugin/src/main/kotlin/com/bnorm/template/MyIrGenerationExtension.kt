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
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.remapTypes
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.FqName

class MyIrGenerationExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
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
                when (it) {
                    is IrType -> it.fixedType() as IrTypeArgument
                    else -> it
                }
            }
        }
    }

    inline fun <reified T> T.fixedTypeMaybe(): T {
        return when (this) {
            is IrType -> this.fixedType() as T
            else -> this
        }
    }

    fun run(moduleFragment: IrModuleFragment) {
        val visitor = ValueClassFinderVisitor(messageCollector)
        moduleFragment.accept(visitor, null)
        val getSymbol = requireNotNull(visitor.getSymbol)

        moduleFragment.transform(ConstructorTransformer(), null)
        moduleFragment.transform(OverriddenFunctionCallTransformer(getSymbol), null)
        moduleFragment.transform(TypeRemapperTransformer(), null)
    }

    inner class ConstructorTransformer : IrElementTransformerVoidWithContext() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            if (expression.type.isHolder()) {
                return visitExpression(expression.valueArguments.single()!!)
            }
            return super.visitConstructorCall(expression)
        }
    }

    inner class TypeRemapperTransformer : IrElementTransformerVoidWithContext() {
        override fun visitFunctionNew(declaration: IrFunction): IrStatement {
            declaration.remapTypes(object : TypeRemapper {
                override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
                }

                override fun leaveScope() {
                }

                override fun remapType(type: IrType): IrType = type.fixedType()
            })
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
