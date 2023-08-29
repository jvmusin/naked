package io.github.jvmusin.naked

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.name.FqName

val ANNOTATION_FQN = FqName("io.github.jvmusin.naked.Naked")

fun MessageCollector.reportError(declaration: IrDeclaration, error: String) {
    reportError(declaration.fileEntry, declaration, error)
}

fun MessageCollector.reportError(fileEntry: IrFileEntry, element: IrElement, error: String) {
    val sourceRangeInfo = fileEntry.getSourceRangeInfo(element.startOffset, element.endOffset)
    report(
        CompilerMessageSeverity.ERROR,
        "(${ANNOTATION_FQN.shortName()}) $error",
        CompilerMessageLocation.create(
            sourceRangeInfo.filePath,
            sourceRangeInfo.startLineNumber,
            sourceRangeInfo.startColumnNumber,
            null
        )
    )
}
