package io.github.jvmusin.naked

import io.github.jvmusin.naked.MyMessageCollector.Companion.my
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irFunctionReference
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.remapTypes
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class NakedIrGenerationExtension(
    private val messageCollector: MessageCollector,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        if (pluginContext.referenceClass(ANNOTATION_FQN) == null) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "($NAKED_PLUGIN) Annotation class $ANNOTATION_FQN not found"
            )
            return
        }

        WholeVisitor(messageCollector.my(moduleFragment.name.asString()), pluginContext).run(moduleFragment)
    }
}

class WholeVisitor(
    private val messageCollector: MyMessageCollector,
    private val pluginContext: IrPluginContext,
) {
    fun run(moduleFragment: IrModuleFragment) {
        messageCollector.report { "Starting on module" }

        val classes = findAnnotatedClasses(moduleFragment)
        classes.forEach { ValueClassValidator.validate(it, messageCollector) }
        if (messageCollector.hasErrors()) {
            messageCollector.report { "Build has errors, skipping" }

            return
        }
//        classes.forEach { generateIdentityFunction(it) }

        val allClasses = ClassCollectorVisitor.findAllClasses(moduleFragment)
        val classesThings = allClasses.mapNotNull { ClassThingsProvider.getClassThings(it, messageCollector) }
        if (classesThings.isEmpty()) {
            messageCollector.report { "No eligible classes found, skipping" }
            return
        }

        messageCollector.report { "Will update usages of ${classesThings.map { it.classType.classFqName }}" }

        moduleFragment.transformVoid(
            ExternalDeclarationTypeTransformer(
                classesThings,
                messageCollector
            )
        )
        for (classThings in classesThings) {
            moduleFragment.transformVoid(
                ConstructorCallTransformer(
                    classThings.constructorSymbol,
                )
            )

            moduleFragment.transformVoid(
                OverriddenFunctionCallTransformer(
                    classThings.getSymbol,
                    classThings.classType,
                    classThings.innerType,
                    pluginContext
                )
            )

            moduleFragment.transformVoid(
                TypeRemapperTransformer(
                    classThings.classType,
                    classThings.innerType
                )
            )
        }

        messageCollector.report { "Updated usages of ${classesThings.map { it.classType.classFqName }}" }
    }

    @Suppress("unused")
    private fun generateIdentityFunction(irClass: IrClass): IrSimpleFunction {
        val innerType = irClass.inlineClassRepresentation!!.underlyingType
        return irClass.addFunction {
            name = IDENTITY_FUNCTION_NAME
            returnType = innerType
            origin = IrDeclarationOrigin.GeneratedByPlugin(NAKED_PLUGIN)
        }.also { f ->
            val self = f.addValueParameter("self", innerType)
            f.body = DeclarationIrBuilder(pluginContext, f.symbol).irBlockBody {
                +irReturn(irGet(self))
            }
        }
    }

    private fun findAnnotatedClasses(moduleFragment: IrModuleFragment): List<IrClass> {
        val classes = mutableListOf<IrClass>()
        moduleFragment.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitClass(declaration: IrClass) {
                if (declaration.hasAnnotation(ANNOTATION_FQN)) {
                    classes += declaration
                }
                super.visitClass(declaration)
            }
        })
        return classes
    }

    inner class ConstructorCallTransformer(
        private val constructorSymbol: IrConstructorSymbol,
    ) : IrElementTransformerVoidWithContext() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            if (expression.symbol == constructorSymbol) {
                return visitExpression(expression.valueArguments.single()!!)
            }
            return super.visitConstructorCall(expression)
        }

        @Suppress("unused")
        private fun generateIdentityFunctionReference(
            expression: IrFunctionReference,
            identityFunctionSymbol: IrFunctionSymbol,
        ): IrFunctionReference {
            return DeclarationIrBuilder(pluginContext, expression.symbol)
                .irFunctionReference(
                    expression.type,
                    identityFunctionSymbol
                )
        }

        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            if (expression.symbol == constructorSymbol) {
                messageCollector.reportError(
                    currentFile.fileEntry,
                    expression
                ) { "Constructor reference is not allowed" }
            }
//            if (expression.symbol == constructorSymbol) {
//                return visitFunctionReference(generateIdentityFunctionReference(expression))
//            }
            return super.visitFunctionReference(expression)
        }
    }

    class ExternalDeclarationTypeTransformer(
        things: List<ClassThings>,
        private val messageCollector: MyMessageCollector,
    ) : IrElementTransformerVoidWithContext() {
        private val mapping = things.associate { it.classType to it.innerType }.let { map ->
            map + map.toList().associate { it.first.makeNullable() to it.second.makeNullable() }
        }
        private val typeRemapper = MyTypeRemapper(mapping)
        override fun visitDeclarationReference(expression: IrDeclarationReference): IrExpression {
            if ((expression.symbol.owner as IrDeclaration).origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB) {
                val symbol = expression.symbol
                if (typeRemapper.isRemappingNeeded(symbol.owner)) {
                    val before = symbol.toString()
                    typeRemapper.remapTypesInPlace(symbol.owner)
                    val after = symbol.toString()
                    messageCollector.report {
                        "Remapped types for external declaration: {{{$before}}}   -->   {{{$after}}}"
                    }
                }
            }
            return super.visitDeclarationReference(expression)
        }
    }

    class TypeRemapperTransformer(
        private val classType: IrType,
        innerValueType: IrType,
    ) : IrElementTransformerVoidWithContext() {
        private val myTypeRemapper = MyTypeRemapper(
            mapOf(
                classType to innerValueType,
                classType.makeNullable() to innerValueType.makeNullable()
            )
        )

        override fun visitClassNew(declaration: IrClass): IrStatement {
            if (declaration.symbol == classType.classOrNull) return declaration
            return super.visitClassNew(declaration)
        }

        override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
            myTypeRemapper.remapTypesInPlace(declaration)
            return super.visitDeclaration(declaration)
        }

        override fun visitExpression(expression: IrExpression): IrExpression {
            myTypeRemapper.remapTypesInPlace(expression)
            return super.visitExpression(expression)
        }
    }

    class ClassCollectorVisitor : IrElementTransformerVoidWithContext() {
        private val visitedTypes = hashSetOf<IrType>()
        private val typeRemapper = object : TypeRemapper {
            override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
            }

            override fun leaveScope() {
            }

            override fun remapType(type: IrType): IrType {
                if (!visitedTypes.add(type)) return type
                if (type !is IrSimpleType) return type
                for (it in type.arguments) {
                    when (it) {
                        is IrType -> remapType(it)
                        is IrTypeProjection -> remapType(it.type)
                        is IrStarProjection -> {}
                    }
                }
                return type
            }
        }

        override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
            declaration.remapTypes(typeRemapper)
            return super.visitDeclaration(declaration)
        }

        override fun visitExpression(expression: IrExpression): IrExpression {
            expression.remapTypes(typeRemapper)
            return super.visitExpression(expression)
        }

        companion object {
            fun findAllClasses(irElement: IrElement): Set<IrClass> {
                val visitor = ClassCollectorVisitor()
                irElement.transformVoid(visitor)
                return visitor.visitedTypes.mapNotNullTo(hashSetOf()) { it.getClass() }
            }

            fun findAllTypes(irElement: IrElement): Set<IrType> {
                val visitor = ClassCollectorVisitor()
                irElement.transformVoid(visitor)
                return visitor.visitedTypes
            }
        }
    }

    private companion object {
        private fun IrElement.transformVoid(transformer: IrElementTransformerVoid) {
            transform(transformer, null)
        }
    }
}
