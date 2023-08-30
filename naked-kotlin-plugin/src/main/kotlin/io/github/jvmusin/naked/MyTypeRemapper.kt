package io.github.jvmusin.naked

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.remapTypes

class MyTypeRemapper(private val mappings: Map<IrType, IrType>) {
    private val realTypeRemapper = createTypeRemapper(mappings)
    fun remapTypesInPlace(irElement: IrElement) {
        if (isRemappingNeeded(irElement)) {
            irElement.remapTypes(realTypeRemapper)
        }
    }

    fun isRemappingNeeded(irElement: IrElement): Boolean {
        val containsTypes = WholeVisitor.ClassCollectorVisitor.findAllTypes(irElement)
        return containsTypes.any { it in mappings }
    }

    private companion object {
        private fun createTypeRemapper(mapping: Map<IrType, IrType>): TypeRemapper {
            return object : TypeRemapper {
                override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
                }

                override fun leaveScope() {
                }

                override fun remapType(type: IrType): IrType {
                    mapping[type]?.let { return it }
                    if (type !is IrSimpleType) return type
                    return type.buildSimpleType {
                        arguments = arguments.map {
                            when (it) {
                                is IrType -> remapType(it) as IrTypeArgument
                                is IrTypeProjection -> makeTypeProjection(remapType(it.type), it.variance)
                                is IrStarProjection -> it
                            }
                        }
                    }
                }
            }
        }
    }
}
