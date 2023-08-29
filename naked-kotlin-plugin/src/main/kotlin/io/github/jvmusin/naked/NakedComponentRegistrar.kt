package io.github.jvmusin.naked

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName

@AutoService(ComponentRegistrar::class)
class NakedComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration,
    ) {
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val enabled = configuration.get(NakedCommandLineProcessor.ARG_ENABLED)

        messageCollector.report(CompilerMessageSeverity.INFO, "Enabled is $enabled")
        if (enabled == null || enabled == "true") {
            IrGenerationExtension.registerExtension(
                project,
                NakedIrGenerationExtension(messageCollector)
            )
        }
    }
}
