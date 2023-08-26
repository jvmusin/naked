package dev.jvmusin.seethrough

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class SeeThroughGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project): Unit = with(target) {
        extensions.create("seeThrough", TemplateGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        kotlinCompilation.dependencies {
            implementation("${BuildConfig.ANNOTATION_LIBRARY_GROUP}:${BuildConfig.ANNOTATION_LIBRARY_NAME}:${BuildConfig.ANNOTATION_LIBRARY_VERSION}")
        }
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(TemplateGradleExtension::class.java)
        return project.provider { listOf(SubpluginOption("enabled", extension.enabled.get().toString())) }
    }
}
