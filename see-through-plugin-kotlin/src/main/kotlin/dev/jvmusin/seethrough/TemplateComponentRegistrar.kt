package dev.jvmusin.seethrough

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName

@AutoService(ComponentRegistrar::class)
class TemplateComponentRegistrar(
    private val defaultString: String,
    private val defaultFile: String,
    private val defaultAnnotation: String,
) : ComponentRegistrar {

    @Suppress("unused") // Used by service loader
    constructor() : this(
        defaultString = "Hello, World!",
        defaultFile = "file.txt",
        defaultAnnotation = "SeeThrough",
    )

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration,
    ) {
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val string = configuration.get(TemplateCommandLineProcessor.ARG_STRING, defaultString)
        val file = configuration.get(TemplateCommandLineProcessor.ARG_FILE, defaultFile)
        val annotation = configuration.get(TemplateCommandLineProcessor.ARG_ANNOTATION, defaultAnnotation)

        IrGenerationExtension.registerExtension(project, MyIrGenerationExtension(messageCollector, FqName(annotation)))
    }
}
