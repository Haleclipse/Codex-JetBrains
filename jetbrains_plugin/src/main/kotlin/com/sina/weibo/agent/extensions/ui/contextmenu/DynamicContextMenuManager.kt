// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.contextmenu

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.plugin.codex.CodexContextMenuProvider

/**
 * Dynamic context menu manager that controls which context menu actions are available
 * based on the current extension type.
 * This manager works in conjunction with DynamicExtensionContextMenuGroup to provide
 * dynamic context menu functionality.
 */
@Service(Service.Level.PROJECT)
class DynamicContextMenuManager(private val project: Project) {

    private val logger = Logger.getInstance(DynamicContextMenuManager::class.java)

    // Current extension ID (always codex)
    @Volatile
    private var currentExtensionId: String? = "codex"

    companion object {
        /**
         * Get dynamic context menu manager instance
         */
        fun getInstance(project: Project): DynamicContextMenuManager {
            return project.getService(DynamicContextMenuManager::class.java)
                ?: error("DynamicContextMenuManager not found")
        }
    }

    /**
     * Initialize the dynamic context menu manager
     */
    fun initialize() {
        logger.info("Initializing dynamic context menu manager for Codex")
        currentExtensionId = "codex"
        logger.info("Dynamic context menu manager initialized with extension: $currentExtensionId")
    }

    /**
     * Set the current extension and update context menu configuration
     */
    fun setCurrentExtension(extensionId: String) {
        logger.info("Extension is fixed to codex")
        currentExtensionId = "codex"
        refreshContextMenus()
    }

    /**
     * Get the current extension ID
     */
    fun getCurrentExtensionId(): String? {
        return "codex"
    }

    /**
     * Get context menu configuration for the current extension
     */
    fun getContextMenuConfiguration(): ContextMenuConfiguration {
        val contextMenuProvider = getContextMenuProvider(currentExtensionId)
        return contextMenuProvider?.getContextMenuConfiguration() ?: DefaultContextMenuConfiguration()
    }

    /**
     * Get context menu actions for the current extension
     */
    fun getContextMenuActions(): List<com.intellij.openapi.actionSystem.AnAction> {
        val contextMenuProvider = getContextMenuProvider(currentExtensionId)
        return contextMenuProvider?.getContextMenuActions(project) ?: emptyList()
    }

    /**
     * Get context menu provider for the specified extension.
     *
     * @param extensionId The extension ID
     * @return Context menu provider instance or null if not found
     */
    private fun getContextMenuProvider(extensionId: String?): ExtensionContextMenuProvider? {
        if (extensionId == null) return null

        return when (extensionId) {
            "codex" -> CodexContextMenuProvider()
            else -> null
        }
    }

    /**
     * Check if a specific context menu action should be visible for the current extension
     */
    fun isActionVisible(actionType: ContextMenuActionType): Boolean {
        val config = getContextMenuConfiguration()
        return config.isActionVisible(actionType)
    }

    /**
     * Refresh all context menus to reflect current configuration
     */
    private fun refreshContextMenus() {
        try {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                try {
                    val actionManager = com.intellij.openapi.actionSystem.ActionManager.getInstance()
                    val dynamicGroup = actionManager.getAction("Codex.DynamicExtensionContextMenu")
                    dynamicGroup?.let { group ->
                        logger.debug("Triggering UI refresh for dynamic context menu group")
                    }
                    logger.debug("Context menus refresh scheduled for extension: $currentExtensionId")
                } catch (e: Exception) {
                    logger.warn("Failed to schedule context menu refresh", e)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to refresh context menus", e)
        }
    }

    /**
     * Dispose the dynamic context menu manager
     */
    fun dispose() {
        logger.info("Disposing dynamic context menu manager")
        currentExtensionId = null
    }
}

/**
 * Default context menu configuration - shows minimal actions
 */
class DefaultContextMenuConfiguration : ContextMenuConfiguration {
    override fun isActionVisible(actionType: ContextMenuActionType): Boolean {
        return false // Codex 目前不显示上下文菜单
    }

    override fun getVisibleActions(): List<ContextMenuActionType> {
        return emptyList()
    }
}
