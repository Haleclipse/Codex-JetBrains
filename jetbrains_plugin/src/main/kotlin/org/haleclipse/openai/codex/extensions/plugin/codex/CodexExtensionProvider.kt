// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.extensions.plugin.codex

import com.intellij.openapi.project.Project
import org.haleclipse.openai.codex.extensions.common.ExtensionType
import org.haleclipse.openai.codex.extensions.config.ExtensionConfiguration
import org.haleclipse.openai.codex.extensions.core.ExtensionManagerFactory
import org.haleclipse.openai.codex.extensions.config.ExtensionProvider
import org.haleclipse.openai.codex.extensions.config.ExtensionMetadata
import org.haleclipse.openai.codex.util.PluginConstants
import org.haleclipse.openai.codex.util.PluginConstants.ConfigFiles.getUserConfigDir
import org.haleclipse.openai.codex.util.PluginResourceUtil
import java.io.File

/**
 * OpenAI Codex extension provider implementation
 */
class CodexExtensionProvider : ExtensionProvider {

    override fun getExtensionId(): String = "codex"

    override fun getDisplayName(): String = "OpenAI Codex"

    override fun getDescription(): String = "OpenAI Codex AI-powered code assistant"

    override fun initialize(project: Project) {
        // Initialize Codex extension configuration
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        extensionConfig.initialize()

        // Initialize extension manager factory
        val extensionManagerFactory = ExtensionManagerFactory.getInstance(project)
        extensionManagerFactory.initialize()
    }

    override fun isAvailable(project: Project): Boolean {
        // Check if codex extension files exist
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(ExtensionType.CODEX)

        // First check project paths
        val possiblePaths = listOf(
            "${getUserConfigDir()}/plugins/${config.codeDir}"
        )

        if (possiblePaths.any { File(it).exists() }) {
            return true
        }

        // Then check plugin resources (for built-in extensions)
        try {
            val pluginResourcePath = PluginResourceUtil.getResourcePath(
                PluginConstants.PLUGIN_ID,
                config.codeDir
            )
            if (pluginResourcePath != null && File(pluginResourcePath).exists()) {
                return true
            }
        } catch (e: Exception) {
            // Ignore exceptions when checking plugin resources
        }

        // For development/testing, always return true if we can't find the files
        return false
    }

    override fun getConfiguration(project: Project): ExtensionMetadata {
        val extensionConfig = ExtensionConfiguration.getInstance(project)
        val config = extensionConfig.getConfig(ExtensionType.CODEX)

        return object : ExtensionMetadata {
            override fun getCodeDir(): String = config.codeDir
            override fun getPublisher(): String = config.publisher
            override fun getVersion(): String = config.version
            override fun getMainFile(): String = config.mainFile
            override fun getActivationEvents(): List<String> = config.activationEvents
            override fun getEngines(): Map<String, String> = config.engines
            override fun getCapabilities(): Map<String, Any> = config.capabilities
            override fun getExtensionDependencies(): List<String> = config.extensionDependencies
        }
    }

    override fun dispose() {
        // Cleanup resources if needed
    }
}
