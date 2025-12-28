// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.extensions.plugin.codex

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.icons.AllIcons
import org.haleclipse.openai.codex.actions.*
import org.haleclipse.openai.codex.extensions.ui.buttons.ExtensionButtonProvider
import org.haleclipse.openai.codex.extensions.ui.buttons.ButtonType
import org.haleclipse.openai.codex.extensions.ui.buttons.ButtonConfiguration

/**
 * OpenAI Codex extension button provider.
 * Provides button configuration specific to Codex extension.
 */
class CodexButtonProvider : ExtensionButtonProvider {

    override fun getExtensionId(): String = "codex"

    override fun getDisplayName(): String = "OpenAI Codex"

    override fun getDescription(): String = "OpenAI Codex AI-powered code assistant"

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun getButtons(project: Project): List<AnAction> {
        return listOf(
            NewChatAction(),
            NewPanelAction(),
            SettingsAction()
        )
    }

    override fun getButtonConfiguration(): ButtonConfiguration {
        return CodexButtonConfiguration()
    }

    /**
     * Codex button configuration.
     */
    private class CodexButtonConfiguration : ButtonConfiguration {
        private val visibleButtons = listOf(
            ButtonType.PLUS,
            ButtonType.SETTINGS
        )

        override fun isButtonVisible(buttonType: ButtonType): Boolean {
            return buttonType in visibleButtons
        }

        override fun getVisibleButtons(): List<ButtonType> {
            return visibleButtons
        }
    }

    /**
     * Action for creating a new chat/thread in Codex.
     */
    class NewChatAction : AnAction() {
        private val logger: Logger = Logger.getInstance(NewChatAction::class.java)
        private val commandId: String = "chatgpt.newChat"

        init {
            templatePresentation.icon = AllIcons.General.Add
            templatePresentation.text = "New Thread"
            templatePresentation.description = "Create a new thread in Codex"
        }

        override fun actionPerformed(e: AnActionEvent) {
            logger.info("New chat button clicked")
            executeCommand(commandId, e.project)
        }
    }

    /**
     * Action for creating a new Codex panel.
     */
    class NewPanelAction : AnAction() {
        private val logger: Logger = Logger.getInstance(NewPanelAction::class.java)
        private val commandId: String = "chatgpt.newCodexPanel"

        init {
            templatePresentation.icon = AllIcons.Actions.OpenNewTab
            templatePresentation.text = "New Codex Agent"
            templatePresentation.description = "Open a new Codex agent panel"
        }

        override fun actionPerformed(e: AnActionEvent) {
            logger.info("New panel button clicked")
            executeCommand(commandId, e.project)
        }
    }

    /**
     * Action for opening Codex settings.
     */
    class SettingsAction : AnAction() {
        private val logger: Logger = Logger.getInstance(SettingsAction::class.java)
        private val commandId: String = "workbench.action.openSettings"

        init {
            templatePresentation.icon = AllIcons.General.Settings
            templatePresentation.text = "Settings"
            templatePresentation.description = "Open Codex settings"
        }

        override fun actionPerformed(e: AnActionEvent) {
            logger.info("Settings button clicked")
            executeCommand(commandId, e.project, listOf("@ext:openai.chatgpt"))
        }
    }
}
