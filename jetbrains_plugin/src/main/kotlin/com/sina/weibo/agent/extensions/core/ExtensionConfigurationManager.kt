package com.sina.weibo.agent.extensions.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.common.ExtensionType

/**
 * Extension configuration manager.
 * Simplified to always use Codex extension - no config file needed.
 */
@Service(Service.Level.PROJECT)
class ExtensionConfigurationManager(private val project: Project) {

    private val logger = Logger.getInstance(ExtensionConfigurationManager::class.java)

    companion object {
        // Default extension ID - always Codex
        private const val DEFAULT_EXTENSION_ID = "codex"

        /**
         * Get extension configuration manager instance
         */
        fun getInstance(project: Project): ExtensionConfigurationManager {
            return project.getService(ExtensionConfigurationManager::class.java)
                ?: error("ExtensionConfigurationManager not found")
        }
    }

    /**
     * Initialize the configuration manager
     */
    fun initialize() {
        logger.info("Initializing extension configuration manager (default: $DEFAULT_EXTENSION_ID)")
    }

    /**
     * Configuration is always valid (hardcoded to Codex)
     */
    fun isConfigurationValid(): Boolean = true

    /**
     * Configuration is always loaded
     */
    fun isConfigurationLoaded(): Boolean = true

    /**
     * Get current extension ID - always returns "codex"
     */
    fun getCurrentExtensionId(): String = DEFAULT_EXTENSION_ID

    /**
     * Set current extension ID - no-op, always uses Codex
     */
    fun setCurrentExtensionId(extensionId: String) {
        logger.info("Extension type is fixed to $DEFAULT_EXTENSION_ID, ignoring request to set: $extensionId")
    }

    /**
     * Get configuration status
     */
    fun getConfigurationStatus(): String {
        return "Configuration Status: Valid ($DEFAULT_EXTENSION_ID)"
    }

    /**
     * Reload configuration - no-op
     */
    fun reloadConfiguration() {
        logger.info("Configuration reload requested (no-op, fixed to $DEFAULT_EXTENSION_ID)")
    }

    /**
     * Create default configuration - no-op, always uses Codex
     */
    fun createDefaultConfiguration() {
        logger.info("createDefaultConfiguration called (no-op, fixed to $DEFAULT_EXTENSION_ID)")
    }

    /**
     * Check for configuration changes - no-op
     */
    fun checkConfigurationChange() {
        // No-op: configuration is hardcoded
    }

    /**
     * Get configuration error - always returns null (no error)
     */
    fun getConfigurationError(): String? = null

    /**
     * Get configuration file path - returns descriptive string
     */
    fun getConfigurationFilePath(): String = "(no config file - hardcoded to $DEFAULT_EXTENSION_ID)"

    /**
     * Get configuration load time - returns null
     */
    fun getConfigurationLoadTime(): Long? = null

    /**
     * Dispose the configuration manager
     */
    fun dispose() {
        logger.info("Disposing extension configuration manager")
    }
}
