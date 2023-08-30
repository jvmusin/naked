package io.github.jvmusin.naked

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getSimpleFunction

class OverriddenFunctionCallTransformer(
    private val getSymbol: IrFunctionSymbol,
    private val sourceType: IrType,
    private val innerType: IrType,
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoidWithContext() {
    private val replacements = run {
        val replacedFunctionNames = arrayOf("hashCode", "toString", "equals")
        fun fn(type: IrType, f: String) = type.getClass()!!.getSimpleFunction(f)!!
        replacedFunctionNames.associate { func ->
            fn(sourceType, func) to fn(innerType, func)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol == getSymbol) {
            return visitExpression(expression.dispatchReceiver!!)
        }
        replacements[expression.symbol]?.let { replacement ->
            val replacedCall = DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol)
                .irCall(replacement).also { call ->
                    call.dispatchReceiver = expression.dispatchReceiver
                    repeat(expression.valueArgumentsCount) { arg ->
                        call.putValueArgument(arg, expression.getValueArgument(arg))
                    }
                }
            return visitCall(replacedCall)
        }
        return super.visitCall(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        replacements[expression.reflectionTarget]?.let { replacement ->
            val newFunctionReference = DeclarationIrBuilder(pluginContext, expression.symbol)
                .irFunctionReference(
                    expression.type,
                    replacement
                )
            return visitFunctionReference(newFunctionReference)
        }
        return super.visitFunctionReference(expression)
    }
}
