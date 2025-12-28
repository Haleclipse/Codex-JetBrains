// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.extensions.ui.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.DumbAware
import org.haleclipse.openai.codex.extensions.core.ExtensionManager
import org.haleclipse.openai.codex.extensions.config.ExtensionProvider
import org.haleclipse.openai.codex.extensions.common.ExtensionChangeListener
import org.haleclipse.openai.codex.extensions.plugin.codex.CodexButtonProvider
import org.haleclipse.openai.codex.extensions.ui.buttons.ExtensionButtonProvider

/**
 * Dynamic extension actions group that shows different buttons based on the current extension type.
 * This class dynamically generates buttons according to the current extension provider.
 */
class DynamicExtensionActionsGroup : DefaultActionGroup(), DumbAware, ActionUpdateThreadAware, ExtensionChangeListener {

    private val logger = Logger.getInstance(DynamicExtensionActionsGroup::class.java)

    private var cachedButtonProvider: ExtensionButtonProvider? = null
    private var cachedExtensionId: String? = null
    private var cachedActions: List<AnAction>? = null

    /**
     * Updates the action group based on the current context and extension type.
     * This method is called each time the menu/toolbar needs to be displayed.
     *
     * @param e The action event containing context information
     */
    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        if (project == null) {
            e.presentation.isVisible = false
            return
        }

        try {
            val extensionId = "codex"

            // 检查是否需要更新缓存
            if (cachedExtensionId != extensionId || cachedActions == null) {
                updateCachedActions(project)
            }

            // 使用缓存的actions
            if (cachedActions != null) {
                removeAll()
                cachedActions!!.forEach { action ->
                    add(action)
                }
                e.presentation.isVisible = true
                logger.debug("Using cached actions for extension: $extensionId")
            }
        } catch (exception: Exception) {
            logger.warn("Failed to load dynamic actions", exception)
            e.presentation.isVisible = false
        }
    }

    private fun updateCachedActions(project: Project) {
        val extensionId = "codex"
        val buttonProvider = CodexButtonProvider()

        // 创建并缓存actions
        val actions = buttonProvider.getButtons(project)

        // 更新缓存
        cachedButtonProvider = buttonProvider
        cachedExtensionId = extensionId
        cachedActions = actions

        logger.debug("Updated cached actions for extension: $extensionId, count: ${actions.size}")
    }

    /**
     * Specifies which thread should be used for updating this action.
     * Returns BGT to ensure updates happen on the background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    /**
     * Called when the current extension changes.
     * This method is part of the ExtensionChangeListener interface.
     *
     * @param newExtensionId The ID of the new extension
     */
    override fun onExtensionChanged(newExtensionId: String) {
        logger.info("Extension changed notification received, but extension is fixed to codex")
    }
}
