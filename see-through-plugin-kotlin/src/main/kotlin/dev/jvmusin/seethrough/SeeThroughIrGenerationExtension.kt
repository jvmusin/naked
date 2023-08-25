package dev.jvmusin.seethrough

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.remapTypes
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class SeeThroughIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val annotationFqn: FqName,
) : IrGenerationExtension {
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
        val classes = ValueClassFinderVisitor(annotationClass).findClasses(moduleFragment)

        for (classThings in classes) {
            val identityFunctionGenerator = IdentityFunctionGenerator(
                messageCollector,
                classThings.classSymbol,
                classThings.innerType
            )
            moduleFragment.transform(identityFunctionGenerator)
            val identityFunctionSymbol = identityFunctionGenerator.identityFunctionSymbol

            moduleFragment.transform(
                ConstructorCallTransformer(
                    messageCollector,
                    classThings.constructorSymbol,
                    identityFunctionSymbol
                )
            )
            moduleFragment.transform(
                OverriddenFunctionCallTransformer(
                    classThings.getSymbol,
                    classThings.classType,
                    classThings.innerType
                )
            )

            moduleFragment.transform(
                TypeRemapperTransformer(
                    classThings.classType,
                    classThings.innerType
                )
            )
            moduleFragment.transform(
                TypeRemapperTransformer(
                    classThings.classType.makeNullable(),
                    classThings.innerType.makeNullable()
                )
            )
        }
    }

    inner class IdentityFunctionGenerator(
        private val messageCollector: MessageCollector,
        private val classSymbol: IrClassSymbol,
        private val innerType: IrType,
    ) : IrElementTransformerVoidWithContext() {
        var identityFunctionSymbol by OneTimeSetField<IrSimpleFunctionSymbol>()
        override fun visitClassNew(declaration: IrClass): IrStatement {
            if (declaration.symbol == classSymbol) {
                val identityFunction = declaration.addFunction {
                    name = Name.identifier("identity")
                    returnType = innerType
                }.also { f ->
                    val parameter = f.addValueParameter("self", innerType)
                    f.body = DeclarationIrBuilder(pluginContext, f.symbol).irBlockBody {
                        +irReturn(irGet(parameter))
                    }
                }
                messageCollector.report(CompilerMessageSeverity.INFO, identityFunction.dump())
                identityFunctionSymbol = identityFunction.symbol
            }
            return super.visitClassNew(declaration)
        }
    }

    inner class ConstructorCallTransformer(
        private val messageCollector: MessageCollector,
        private val constructorSymbol: IrConstructorSymbol,
        private val identityFunctionSymbol: IrFunctionSymbol,
    ) : IrElementTransformerVoidWithContext() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            if (expression.symbol == constructorSymbol) {
                return visitExpression(expression.valueArguments.single()!!)
            }
            return super.visitConstructorCall(expression)
        }

        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            if (expression.symbol == constructorSymbol) {
                val newFunctionReference = DeclarationIrBuilder(pluginContext, expression.symbol)
                    .irFunctionReference(
                        expression.type,
                        identityFunctionSymbol
                    )
                return visitFunctionReference(newFunctionReference)
            }
            return super.visitFunctionReference(expression)
        }
    }

    inner class TypeRemapperTransformer(
        private val classType: IrType,
        private val innerValueType: IrType,
    ) : IrElementTransformerVoidWithContext() {
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

        override fun visitClassNew(declaration: IrClass): IrStatement {
            if (declaration.hasAnnotation(annotationClass)) return declaration
            return super.visitClassNew(declaration)
        }

        override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
            declaration.remapTypes(typeRemapper)
            return super.visitDeclaration(declaration)
        }
    }

    inner class OverriddenFunctionCallTransformer(
        private val getSymbol: IrFunctionSymbol,
        private val sourceType: IrType,
        private val innerType: IrType,
    ) : IrElementTransformerVoidWithContext() {
        private val replacements = run {
            val replacedFunctionNames = arrayOf("hashCode", "toString", "equals")
            val sourceType = sourceType.classFqName!!.asString()
            val targetType = innerType.classFqName!!.asString()
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
            replacements[expression.reflectionTarget]?.let { replacement->
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

    private companion object {
        private fun IrModuleFragment.transform(transformer: IrElementTransformerVoid) {
            transform(transformer, null)
        }
    }
}

