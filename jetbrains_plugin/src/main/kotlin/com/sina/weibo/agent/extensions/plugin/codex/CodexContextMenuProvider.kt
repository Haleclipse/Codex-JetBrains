// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.codex

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.extensions.ui.contextmenu.ExtensionContextMenuProvider
import com.sina.weibo.agent.extensions.ui.contextmenu.ContextMenuConfiguration
import com.sina.weibo.agent.extensions.ui.contextmenu.ContextMenuActionType

/**
 * OpenAI Codex extension context menu provider.
 * Provides context menu configuration specific to Codex extension.
 */
class CodexContextMenuProvider : ExtensionContextMenuProvider {

    override fun getExtensionId(): String = "codex"

    override fun getDisplayName(): String = "OpenAI Codex"

    override fun getDescription(): String = "OpenAI Codex AI-powered code assistant"

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun getContextMenuActions(project: Project): List<AnAction> {
        // Codex 目前不提供特殊的上下文菜单操作
        return emptyList()
    }

    override fun getContextMenuConfiguration(): ContextMenuConfiguration {
        return CodexContextMenuConfiguration()
    }

    /**
     * Codex context menu configuration.
     */
    private class CodexContextMenuConfiguration : ContextMenuConfiguration {
        override fun isActionVisible(actionType: ContextMenuActionType): Boolean {
            // Codex 目前不显示上下文菜单项
            return false
        }

        override fun getVisibleActions(): List<ContextMenuActionType> {
            return emptyList()
        }
    }
}
