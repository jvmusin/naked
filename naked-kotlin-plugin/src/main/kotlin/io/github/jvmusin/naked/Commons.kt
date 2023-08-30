package io.github.jvmusin.naked

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val ANNOTATION_FQN = FqName("io.github.jvmusin.naked.Naked")

val IDENTITY_FUNCTION_NAME = Name.identifier("identity")

@Suppress("ClassName")
data object NAKED_PLUGIN : GeneratedDeclarationKey()

class MyMessageCollector(
    private val messageCollector: MessageCollector,
    private val prefix: String,
    private val minLoggedSeverity: CompilerMessageSeverity = DEFAULT_SEVERITY,
) {
    fun report(
        severity: CompilerMessageSeverity = DEFAULT_SEVERITY,
        location: CompilerMessageLocation? = null,
        messageProvider: () -> String,
    ) {
        if (severity <= minLoggedSeverity) {
            messageCollector.report(severity, "($NAKED_PLUGIN) $prefix ${messageProvider()}", location)
        }
    }

    fun reportError(declaration: IrDeclaration, error: () -> String) {
        reportError(declaration.fileEntry, declaration, error)
    }

    fun reportError(fileEntry: IrFileEntry, element: IrElement, error: () -> String) {
        val sourceRangeInfo = fileEntry.getSourceRangeInfo(element.startOffset, element.endOffset)
        report(
            CompilerMessageSeverity.ERROR,
            CompilerMessageLocation.create(
                sourceRangeInfo.filePath,
                sourceRangeInfo.startLineNumber,
                sourceRangeInfo.startColumnNumber,
                null
            ),
            error
        )
    }

    fun hasErrors() = messageCollector.hasErrors()

    companion object {
        val DEFAULT_SEVERITY = CompilerMessageSeverity.STRONG_WARNING
        fun MessageCollector.my(prefix: String) = MyMessageCollector(this, prefix)
    }
}