package io.github.jvmusin.naked

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

data class ClassThings(
    val classSymbol: IrClassSymbol,
    val classType: IrType,
    val getSymbol: IrFunctionSymbol,
    val constructorSymbol: IrConstructorSymbol,
    val innerType: IrType,
)