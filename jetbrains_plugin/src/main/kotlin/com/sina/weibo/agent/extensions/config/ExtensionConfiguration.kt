// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.config

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.common.ExtensionType
import java.util.Properties
import java.io.File
import com.sina.weibo.agent.util.PluginConstants
import com.sina.weibo.agent.util.ConfigFileUtils

/**
 * Extension configuration manager for Codex
 * Manages configuration for the Codex extension
 */
@Service(Service.Level.PROJECT)
class ExtensionConfiguration(private val project: Project) {
    private val LOG = Logger.getInstance(ExtensionConfiguration::class.java)

    // Current active extension type (always CODEX)
    @Volatile
    private var currentExtensionType: ExtensionType = ExtensionType.CODEX

    // Extension configurations cache
    private val extensionConfigs = mutableMapOf<ExtensionType, ExtensionConfig>()

    companion object {
        /**
         * Get extension configuration instance
         */
        fun getInstance(project: Project): ExtensionConfiguration {
            return project.getService(ExtensionConfiguration::class.java)
                ?: error("ExtensionConfiguration not found")
        }
    }

    /**
     * Initialize extension configuration
     */
    fun initialize() {
        LOG.info("Initializing Codex extension configuration")

        // Load configuration for CODEX
        loadConfiguration(ExtensionType.CODEX)

        // Always use CODEX
        currentExtensionType = ExtensionType.CODEX

        LOG.info("Extension configuration initialized, current type: ${currentExtensionType.code}")
    }

    /**
     * Get current active extension type
     */
    fun getCurrentExtensionType(): ExtensionType {
        return currentExtensionType
    }

    /**
     * Set current active extension type
     */
    fun setCurrentExtensionType(extensionType: ExtensionType) {
        LOG.info("Extension type is fixed to CODEX")
        currentExtensionType = ExtensionType.CODEX
    }

    /**
     * Get configuration for current extension type
     */
    fun getCurrentConfig(): ExtensionConfig {
        return getConfig(currentExtensionType)
    }

    /**
     * Get configuration for specific extension type
     */
    fun getConfig(extensionType: ExtensionType): ExtensionConfig {
        return extensionConfigs[extensionType] ?: ExtensionConfig.getDefault(extensionType)
    }

    /**
     * Load configuration for specific extension type
     */
    private fun loadConfiguration(extensionType: ExtensionType) {
        try {
            val config = ExtensionConfig.loadFromProperties(extensionType)
            extensionConfigs[extensionType] = config
            LOG.info("Loaded configuration for ${extensionType.code}")
        } catch (e: Exception) {
            LOG.warn("Failed to load configuration for ${extensionType.code}, using default", e)
            extensionConfigs[extensionType] = ExtensionConfig.getDefault(extensionType)
        }
    }
}

/**
 * Extension configuration data class for Codex
 */
data class ExtensionConfig(
    val extensionType: ExtensionType,
    val codeDir: String,
    val displayName: String,
    val description: String,
    val publisher: String,
    val version: String,
    val mainFile: String,
    val activationEvents: List<String>,
    val engines: Map<String, String>,
    val capabilities: Map<String, Any>,
    val extensionDependencies: List<String>
) {
    companion object {
        /**
         * Get default configuration for extension type
         */
        fun getDefault(extensionType: ExtensionType): ExtensionConfig {
            return when (extensionType) {
                ExtensionType.CODEX -> ExtensionConfig(
                    extensionType = extensionType,
                    codeDir = "codex",
                    displayName = "OpenAI Codex",
                    description = "OpenAI Codex AI-powered code assistant",
                    publisher = "openai",
                    version = "0.4.56",
                    mainFile = "./out/extension.js",
                    activationEvents = listOf("onStartupFinished"),
                    engines = mapOf("vscode" to "^1.0.0"),
                    capabilities = emptyMap(),
                    extensionDependencies = emptyList()
                )
            }
        }

        /**
         * Load configuration from properties file
         */
        fun loadFromProperties(extensionType: ExtensionType): ExtensionConfig {
            // This would load from a properties file specific to the extension type
            // For now, return default configuration
            return getDefault(extensionType)
        }
    }
}
