// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.buttons

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.plugin.codex.CodexButtonProvider

/**
 * Dynamic button manager that controls which buttons are visible based on the current extension type.
 * This manager works in conjunction with DynamicExtensionActionsGroup to provide dynamic button functionality.
 */
@Service(Service.Level.PROJECT)
class DynamicButtonManager(private val project: Project) {

    private val logger = Logger.getInstance(DynamicButtonManager::class.java)

    // Current extension ID (always codex)
    @Volatile
    private var currentExtensionId: String? = "codex"

    companion object {
        /**
         * Get dynamic button manager instance
         */
        fun getInstance(project: Project): DynamicButtonManager {
            return project.getService(DynamicButtonManager::class.java)
                ?: error("DynamicButtonManager not found")
        }
    }

    /**
     * Initialize the dynamic button manager
     */
    fun initialize() {
        logger.info("Initializing dynamic button manager for Codex")
        currentExtensionId = "codex"
        logger.info("Dynamic button manager initialized with extension: $currentExtensionId")
    }

    /**
     * Set the current extension and update button configuration
     */
    fun setCurrentExtension(extensionId: String) {
        logger.info("Extension is fixed to codex")
        currentExtensionId = "codex"
        refreshActionToolbars()
    }

    /**
     * Get the current extension ID
     */
    fun getCurrentExtensionId(): String? {
        return currentExtensionId
    }

    /**
     * Get button configuration for the current extension
     */
    fun getButtonConfiguration(): ButtonConfiguration {
        val buttonProvider = getButtonProvider(currentExtensionId)
        return buttonProvider?.getButtonConfiguration() ?: DefaultButtonConfiguration()
    }

    /**
     * Get button provider for the specified extension.
     *
     * @param extensionId The extension ID
     * @return Button provider instance or null if not found
     */
    private fun getButtonProvider(extensionId: String?): ExtensionButtonProvider? {
        if (extensionId == null) return null

        return when (extensionId) {
            "codex" -> CodexButtonProvider()
            else -> null
        }
    }

    /**
     * Check if a specific button should be visible for the current extension
     */
    fun isButtonVisible(buttonType: ButtonType): Boolean {
        val config = getButtonConfiguration()
        return config.isButtonVisible(buttonType)
    }

    /**
     * Refresh all action toolbars to reflect current button configuration
     */
    private fun refreshActionToolbars() {
        try {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                try {
                    val actionManager = ActionManager.getInstance()
                    val dynamicGroup = actionManager.getAction("Codex.DynamicExtensionActions")
                    dynamicGroup?.let { group ->
                        logger.debug("Triggering UI refresh for dynamic actions group")
                    }
                    logger.debug("Action toolbars refresh scheduled for extension: $currentExtensionId")
                } catch (e: Exception) {
                    logger.warn("Failed to schedule action toolbar refresh", e)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to refresh action toolbars", e)
        }
    }

    /**
     * Dispose the dynamic button manager
     */
    fun dispose() {
        logger.info("Disposing dynamic button manager")
        currentExtensionId = null
    }
}

/**
 * Button types that can be configured
 */
enum class ButtonType {
    PLUS,
    PROMPTS,
    MCP,
    HISTORY,
    MARKETPLACE,
    SETTINGS
}

/**
 * Button configuration interface
 */
interface ButtonConfiguration {
    fun isButtonVisible(buttonType: ButtonType): Boolean
    fun getVisibleButtons(): List<ButtonType>
}

/**
 * Default button configuration - shows minimal buttons
 */
class DefaultButtonConfiguration : ButtonConfiguration {
    override fun isButtonVisible(buttonType: ButtonType): Boolean {
        return when (buttonType) {
            ButtonType.PLUS,
            ButtonType.SETTINGS -> true
            else -> false
        }
    }

    override fun getVisibleButtons(): List<ButtonType> {
        return listOf(
            ButtonType.PLUS,
            ButtonType.SETTINGS
        )
    }
}
