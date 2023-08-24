@file:OptIn(FirIncompatiblePluginAPI::class)

package dev.jvmusin.seethrough

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TemplateIrGenerationExtension(
        private val messageCollector: MessageCollector,
        private val string: String,
        private val file: String
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'string' = $string")
        messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'file' = $file")

        val typeAnyNullable = moduleFragment.irBuiltins.anyNType
        val debugLogAnnotation = pluginContext.referenceClass(FqName("DebugLog"))!!
        val funPrintln = pluginContext.referenceFunctions(FqName("kotlin.io.println"))
                .single {
                    val parameters = it.owner.valueParameters
                    parameters.size == 1 && parameters[0].type == typeAnyNullable
                }
        moduleFragment.transform(DebugLogTransformer(pluginContext, debugLogAnnotation, funPrintln), null)
    }
}

class DebugLogTransformer(
        private val pluginContext: IrPluginContext,
        private val annotationClass: IrClassSymbol,
        private val logFunction: IrSimpleFunctionSymbol,
) : IrElementTransformerVoidWithContext() {
    private val typeUnit = pluginContext.irBuiltIns.unitType
    private val typeThrowable = pluginContext.irBuiltIns.throwableType

    private val classMonotonic =
            pluginContext.referenceClass(FqName("kotlin.time.TimeSource.Monotonic"))!!

    private val funMarkNow =
            pluginContext.referenceFunctions(FqName("kotlin.time.TimeSource.Monotonic.markNow")).single()

    private val funElapsedNow =
            pluginContext.referenceFunctions(FqName("kotlin.time.TimeSource.Monotonic.ValueTimeMark.elapsedNow")).single()

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val body = declaration.body
        if (body != null && declaration.hasAnnotation(annotationClass)) {
            declaration.body = irDebug(declaration, body)
        }
        return super.visitFunctionNew(declaration)
    }

    private fun irDebug(function: IrFunction, body: IrBody): IrBody {
        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            +irDebugEnter(function)

            val startTime = irTemporary(irCall(funMarkNow).also { call ->
                call.dispatchReceiver = irGetObject(classMonotonic)
            })

            val tryBlock = irBlock(resultType = function.returnType) {
                for (statement in body.statements) +statement
                if (function.returnType == typeUnit) +irDebugExit(function, startTime)
            }.transform(DebugLogReturnTransformer(function, startTime), null)

            val throwable = buildVariable(
                    scope.getLocalDeclarationParent(), startOffset, endOffset, IrDeclarationOrigin.CATCH_PARAMETER,
                    Name.identifier("t"), typeThrowable
            )

            +IrTryImpl(startOffset, endOffset, tryBlock.type).also { irTry ->
                irTry.tryResult = tryBlock
                irTry.catches += irCatch(throwable, irBlock {
                    +irDebugExit(function, startTime, irGet(throwable))
                    +irThrow(irGet(throwable))
                })
            }
        }
    }

    inner class DebugLogReturnTransformer(
            private val function: IrFunction,
            private val startTime: IrVariable
    ) : IrElementTransformerVoidWithContext() {
        override fun visitReturn(expression: IrReturn): IrExpression {
            if (expression.returnTargetSymbol != function.symbol) return super.visitReturn(expression)

            return DeclarationIrBuilder(pluginContext, function.symbol).irBlock {
                val result = irTemporary(expression.value)
                +irDebugExit(function, startTime, irGet(result))
                +expression.apply {
                    value = irGet(result)
                }
            }
        }
    }

    private fun IrBuilderWithScope.irDebugEnter(
            function: IrFunction
    ): IrCall {
        val concat = irConcat()
        concat.addArgument(irString("⇢ ${function.name}("))
        for ((index, valueParameter) in function.valueParameters.withIndex()) {
            if (index > 0) concat.addArgument(irString(", "))
            concat.addArgument(irString("${valueParameter.name}"))
            concat.addArgument(irGet(valueParameter))
        }
        concat.addArgument(irString(")"))

        return irCall(logFunction).also { call ->
            call.putValueArgument(0, concat)
        }
    }

    private fun IrBuilderWithScope.irDebugExit(
            function: IrFunction,
            startTime: IrValueDeclaration,
            result: IrExpression? = null
    ): IrCall {
        val concat = irConcat()
        concat.addArgument(irString("⇠ ${function.name} ["))
        concat.addArgument(irCall(funElapsedNow).also { call ->
            call.dispatchReceiver = irGet(startTime)
        })
        if (result != null) {
            concat.addArgument(irString("] = "))
            concat.addArgument(result)
        } else {
            concat.addArgument(irString("]"))
        }

        return irCall(logFunction).also { call ->
            call.putValueArgument(0, concat)
        }
    }
}
