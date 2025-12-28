package org.haleclipse.openai.codex.extensions.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.haleclipse.openai.codex.util.PluginConstants
import java.io.File

/**
 * Extension configuration manager.
 * Simplified to always use Codex extension - no config file needed.
 * Checks if extension is installed in user directory.
 */
@Service(Service.Level.PROJECT)
class ExtensionConfigurationManager(private val project: Project) {

    private val logger = Logger.getInstance(ExtensionConfigurationManager::class.java)

    companion object {
        // Default extension ID - always Codex
        const val DEFAULT_EXTENSION_ID = "codex"

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
        val installed = isExtensionInstalled()
        return if (installed) {
            "Configuration Status: Valid ($DEFAULT_EXTENSION_ID - installed)"
        } else {
            "Configuration Status: Extension not installed"
        }
    }

    /**
     * Check if the Codex extension is installed in user directory
     */
    fun isExtensionInstalled(): Boolean {
        val extensionPath = getExtensionInstallPath()
        val packageJson = File(extensionPath, "package.json")
        return packageJson.exists()
    }

    /**
     * Get extension install path
     */
    fun getExtensionInstallPath(): String {
        return "${VsixManager.getBaseDirectory()}/$DEFAULT_EXTENSION_ID"
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
     * Get configuration error - returns error if extension not installed
     */
    fun getConfigurationError(): String? {
        return if (isExtensionInstalled()) {
            null
        } else {
            "Codex extension not installed. Please install from VSIX."
        }
    }

    /**
     * Get configuration file path - returns extension install path
     */
    fun getConfigurationFilePath(): String = getExtensionInstallPath()

    /**
     * Get configuration load time - returns null
     */
    fun getConfigurationLoadTime(): Long? = null

    /**
     * Get recovery suggestions for missing extension
     */
    fun getRecoverySuggestions(): List<String> {
        return listOf(
            "1. 点击 '选择 VSIX 文件安装' 按钮安装 Codex 扩展",
            "2. 从 https://www.vsixhub.com/vsix/163404/ 下载 VSIX 文件",
            "3. 扩展将安装到 ${getExtensionInstallPath()}"
        )
    }

    /**
     * Dispose the configuration manager
     */
    fun dispose() {
        logger.info("Disposing extension configuration manager")
    }
}
