// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.extensions.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.haleclipse.openai.codex.extensions.common.ExtensionChangeListener
import org.haleclipse.openai.codex.extensions.config.ExtensionProvider
import org.haleclipse.openai.codex.extensions.plugin.codex.CodexExtensionProvider
import org.haleclipse.openai.codex.extensions.ui.buttons.DynamicButtonManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Global extension manager
 * Manages the Codex extension provider
 */
@Service(Service.Level.PROJECT)
class ExtensionManager(private val project: Project) {
    private val LOG = Logger.getInstance(ExtensionManager::class.java)

    // Registered extension providers
    private val extensionProviders = ConcurrentHashMap<String, ExtensionProvider>()

    // Current active extension provider (always Codex)
    @Volatile
    private var currentProvider: ExtensionProvider? = null

    companion object {
        /**
         * Get extension manager instance
         */
        fun getInstance(project: Project): ExtensionManager {
            return project.getService(ExtensionManager::class.java)
                ?: error("ExtensionManager not found")
        }
    }

    /**
     * Initialize extension manager
     * @param configuredExtensionId Ignored, always uses Codex
     */
    fun initialize(configuredExtensionId: String? = null) {
        LOG.info("Initializing extension manager with Codex")

        // Register Codex extension provider
        registerExtensionProviders()

        // Always use Codex
        val provider = extensionProviders["codex"]
        if (provider != null) {
            currentProvider = provider
            LOG.info("Set Codex as the extension provider")
        } else {
            LOG.error("Codex extension provider not found!")
        }

        LOG.info("Extension manager initialized")
    }

    /**
     * Initialize extension manager with default behavior
     */
    fun initialize() {
        initialize(null)
    }

    /**
     * Check if configuration is valid for this extension manager
     */
    fun isConfigurationValid(): Boolean {
        return currentProvider != null && currentProvider!!.isAvailable(project)
    }

    /**
     * Get configuration validation error message if any
     */
    fun getConfigurationError(): String? {
        return if (currentProvider == null) {
            "No extension provider set"
        } else if (!currentProvider!!.isAvailable(project)) {
            "Extension provider '${currentProvider!!.getExtensionId()}' is not available"
        } else null
    }

    /**
     * Check if extension manager is properly initialized with a valid provider
     */
    fun isProperlyInitialized(): Boolean {
        return currentProvider != null && currentProvider!!.isAvailable(project)
    }

    fun getAllExtensions(): List<ExtensionProvider> {
        return listOf(CodexExtensionProvider())
    }

    /**
     * Register extension providers
     */
    private fun registerExtensionProviders() {
        getAllExtensions().forEach { registerExtensionProvider(it) }
    }

    /**
     * Register an extension provider
     */
    fun registerExtensionProvider(provider: ExtensionProvider) {
        extensionProviders[provider.getExtensionId()] = provider
        LOG.info("Registered extension provider: ${provider.getExtensionId()}")
    }

    /**
     * Get current extension provider
     */
    fun getCurrentProvider(): ExtensionProvider? {
        return currentProvider
    }

    /**
     * Set current extension provider
     * Note: Always uses Codex
     */
    fun setCurrentProvider(extensionId: String, forceRestart: Boolean? = false): Boolean {
        LOG.info("Extension is fixed to Codex")

        val provider = extensionProviders["codex"]
        if (provider != null && provider.isAvailable(project)) {
            currentProvider = provider
            provider.initialize(project)

            // Update configuration
            try {
                val configManager = ExtensionConfigurationManager.getInstance(project)
                configManager.setCurrentExtensionId("codex")
            } catch (e: Exception) {
                LOG.warn("Failed to update configuration manager", e)
            }

            // Update button configuration
            try {
                if (forceRestart == false) {
                    val buttonManager = DynamicButtonManager.getInstance(project)
                    buttonManager.setCurrentExtension("codex")
                }
            } catch (e: Exception) {
                LOG.warn("Failed to update button configuration", e)
            }

            // Update context menu configuration
            try {
                if (forceRestart == false) {
                    val contextMenuManager = org.haleclipse.openai.codex.extensions.ui.contextmenu.DynamicContextMenuManager.getInstance(project)
                    contextMenuManager.setCurrentExtension("codex")
                }
            } catch (e: Exception) {
                LOG.warn("Failed to update context menu configuration", e)
            }

            // Notify listeners about configuration change
            try {
                project.messageBus.syncPublisher(ExtensionChangeListener.EXTENSION_CHANGE_TOPIC)
                    .onExtensionChanged("codex")
            } catch (e: Exception) {
                LOG.warn("Failed to notify extension change listeners", e)
            }

            LOG.info("Configuration updated to Codex extension provider")
            return true
        } else {
            LOG.warn("Codex extension provider not available")
            return false
        }
    }

    /**
     * Switch extension provider with restart
     */
    fun switchExtensionProvider(extensionId: String, forceRestart: Boolean = false): CompletableFuture<Boolean> {
        val extensionSwitcher = ExtensionSwitcher.Companion.getInstance(project)
        return extensionSwitcher.switchExtension("codex", forceRestart)
    }

    /**
     * Get all available extension providers
     */
    fun getAvailableProviders(): List<ExtensionProvider> {
        return extensionProviders.values.filter { it.isAvailable(project) }
    }

    /**
     * Get all registered extension providers
     */
    fun getAllProviders(): List<ExtensionProvider> {
        return extensionProviders.values.toList()
    }

    /**
     * Get extension provider by ID
     */
    fun getProvider(extensionId: String): ExtensionProvider? {
        return extensionProviders[extensionId]
    }

    /**
     * Initialize current extension provider
     */
    fun initializeCurrentProvider() {
        currentProvider?.initialize(project)
    }

    /**
     * Dispose all extension providers
     */
    fun dispose() {
        LOG.info("Disposing extension manager")
        extensionProviders.values.forEach { it.dispose() }
        extensionProviders.clear()
        currentProvider = null
    }
}
