@file:OptIn(FirIncompatiblePluginAPI::class)

package com.bnorm.template

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.name.FqName

class MyIrGenerationExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val holderClass = pluginContext.referenceClass(FqName("Holder"))!!
        moduleFragment.transform(MyTransformer(messageCollector, pluginContext, holderClass), null)
        messageCollector.report(CompilerMessageSeverity.INFO, moduleFragment.dump())
    }
}

class MyTransformer(
        private val messageCollector: MessageCollector,
        private val pluginContext: IrPluginContext,
        private val holderClass: IrClassSymbol
) : IrElementTransformerVoidWithContext() {
    private fun IrType.isHolder(): Boolean = classifierOrNull == holderClass
    private val underlyingReplacementType = pluginContext.irBuiltIns.stringType
    fun <T : IrExpression > T.fixType(): T = apply { if (type.isHolder()) type = underlyingReplacementType }
    fun <T : IrValueDeclaration> T.fixType(): T = apply { if (type.isHolder()) type = underlyingReplacementType }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        if (expression.type.isHolder()) {
            return expression.valueArguments.single()!!
        }
        return super.visitConstructorCall(expression)
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        declaration.valueParameters.forEach { it.fixType() }
        if (declaration.returnType.isHolder()) declaration.returnType = underlyingReplacementType
        return super.visitFunctionNew(declaration)
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        return super.visitReturn(expression.fixType())
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        return super.visitFunctionAccess(expression.fixType())
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        return super.visitGetValue(expression.fixType())
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return super.visitVariable(declaration.fixType())
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return super.visitCall(expression)
    }
}