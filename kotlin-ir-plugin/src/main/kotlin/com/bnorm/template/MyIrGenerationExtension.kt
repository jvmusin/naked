@file:OptIn(FirIncompatiblePluginAPI::class)

package com.bnorm.template

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.name.FqName

class MyIrGenerationExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(MyTransformer(messageCollector, pluginContext), null)
        messageCollector.report(CompilerMessageSeverity.INFO, moduleFragment.dump())
    }
}

class MyTransformer(
        private val messageCollector: MessageCollector,
        private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private val holderClass = pluginContext.referenceClass(FqName("Holder"))!!
    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        messageCollector.report(CompilerMessageSeverity.INFO, "In visitConstructorCall with " + expression.dump())
        if (expression.type == holderClass || true) {
            return expression.valueArguments.single()!!
        }
        return super.visitConstructorCall(expression)
    }

    override fun visitCall(expression: IrCall): IrExpression {
//        messageCollector.report(CompilerMessageSeverity.INFO, "In visitCall with " + expression.dump())
        return super.visitCall(expression)
    }
}