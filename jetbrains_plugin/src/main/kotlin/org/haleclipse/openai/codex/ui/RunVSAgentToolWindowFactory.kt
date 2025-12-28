// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.ui.jcef.JBCefApp
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.ide.BrowserUtil
import org.haleclipse.openai.codex.actions.OpenDevToolsAction
import org.haleclipse.openai.codex.plugin.WecoderPlugin
import org.haleclipse.openai.codex.plugin.WecoderPluginService
import org.haleclipse.openai.codex.plugin.DEBUG_MODE
import org.haleclipse.openai.codex.webview.DragDropHandler
import org.haleclipse.openai.codex.webview.WebViewCreationCallback
import org.haleclipse.openai.codex.webview.WebViewInstance
import org.haleclipse.openai.codex.webview.WebViewManager
import org.haleclipse.openai.codex.util.PluginConstants
import org.haleclipse.openai.codex.extensions.core.ExtensionConfigurationManager
import org.haleclipse.openai.codex.extensions.core.ExtensionManager
import org.haleclipse.openai.codex.plugin.SystemObjectProvider
import org.haleclipse.openai.codex.extensions.ui.VsixUploadDialog
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.awt.Dimension
import java.awt.Font
import java.awt.Component
import java.awt.Cursor
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.BorderFactory
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import org.haleclipse.openai.codex.util.ConfigFileUtils

class RunVSAgentToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Initialize plugin service
        val pluginService = WecoderPlugin.getInstance(project)
//        pluginService.initialize(project)

        // toolbar
        val titleActions = mutableListOf<AnAction>()
        val action = ActionManager.getInstance().getAction("WecoderToolbarGroup")
        if (action != null) {
            titleActions.add(action)
        }
        // Add developer tools button only in debug mode
        if ( WecoderPluginService.getDebugMode() != DEBUG_MODE.NONE) {
            titleActions.add(OpenDevToolsAction { project.getService(WebViewManager::class.java).getLatestWebView() })
        }

        toolWindow.setTitleActions(titleActions)

        // webview panel
        val toolWindowContent = RunVSAgentToolWindowContent(project, toolWindow)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            toolWindowContent.content,
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    private class RunVSAgentToolWindowContent(
        private val project: Project,
        private val toolWindow: ToolWindow
    ) : WebViewCreationCallback {
        private val logger = Logger.getInstance(RunVSAgentToolWindowContent::class.java)

        // Get WebViewManager instance
        private val webViewManager = project.getService(WebViewManager::class.java)

        // Get ExtensionConfigurationManager instance
        private val configManager = ExtensionConfigurationManager.getInstance(project)
        
        // Get ExtensionManager instance
        private val extensionManager = ExtensionManager.getInstance(project)

        // Content panel
        private val contentPanel = JPanel(BorderLayout())

        // Placeholder label
        private val placeholderLabel = JLabel(createSystemInfoText())

        // System info text for copying
        private val systemInfoText = createSystemInfoPlainText()

        // Plugin selection panel (shown when configuration is invalid)
        private val pluginSelectionPanel = createPluginSelectionPanel()

        // Configuration status panel
        private val configStatusPanel = createConfigStatusPanel()

        // State lock to prevent UI changes during plugin startup
        @Volatile
        private var isPluginStarting = false

        // Plugin running state
        @Volatile
        private var isPluginRunning = false

        /**
         * Check if plugin is actually running
         */
        private fun isPluginActuallyRunning(): Boolean {
            return try {
                val extensionManager = ExtensionManager.getInstance(project)
                extensionManager.isProperlyInitialized()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Create system information text in HTML format
         */
        private fun createSystemInfoText(): String {
            val appInfo = ApplicationInfo.getInstance()
            val plugin = PluginManagerCore.getPlugin(PluginId.getId(PluginConstants.PLUGIN_ID))
            val pluginVersion = plugin?.version ?: "unknown"
            val osName = System.getProperty("os.name")
            val osVersion = System.getProperty("os.version")
            val osArch = System.getProperty("os.arch")
            val jcefSupported = JBCefApp.isSupported()

            // Check for Linux ARM system
            val isLinuxArm = osName.lowercase().contains("linux") && (osArch.lowercase().contains("aarch64") || osArch.lowercase().contains("arm"))

            // Detect current IDEA theme
            val isDarkTheme = detectCurrentTheme()
            
            // Generate theme-adaptive CSS styles
            val themeStyles = generateThemeStyles(isDarkTheme)

            return buildString {
                append("<html><head><style>$themeStyles</style></head>")
                append("<body class='${if (isDarkTheme) "dark-theme" else "light-theme"}'>")

                // Header section
                append("<div class='header'>")
                append("<div class='title'>Codex</div>")
                append("<div class='subtitle'>Initializing...</div>")
                append("</div>")

                // System info card
                append("<div class='info-card'>")
                append("<div class='card-title'>📊 System Information</div>")
                append("<div class='info-grid'>")

                // Info rows with modern styling
                append("<div class='info-row'>")
                append("<span class='info-label'>💻 CPU Architecture</span>")
                append("<span class='info-value'>$osArch</span>")
                append("</div>")

                append("<div class='info-row'>")
                append("<span class='info-label'>🖥️ Operating System</span>")
                append("<span class='info-value'>$osName $osVersion</span>")
                append("</div>")

                append("<div class='info-row'>")
                append("<span class='info-label'>🔧 IDE Version</span>")
                append("<span class='info-value version-text'>${appInfo.fullApplicationName}</span>")
                append("</div>")

                append("<div class='info-row'>")
                append("<span class='info-label'>📦 Plugin Version</span>")
                append("<span class='info-value'>$pluginVersion</span>")
                append("</div>")

                append("<div class='info-row'>")
                append("<span class='info-label'>🌐 JCEF Support</span>")
                append("<span class='info-value ${if (jcefSupported) "success" else "error"}'>${if (jcefSupported) "✓ Yes" else "✗ No"}</span>")
                append("</div>")

                append("</div>")
                append("</div>")

                // Warning messages with modern styling
                if (isLinuxArm) {
                    append("<div class='warning-card warning'>")
                    append("<div class='warning-header'>")
                    append("<span class='warning-icon'>⚠️</span>")
                    append("<span class='warning-title'>System Not Supported</span>")
                    append("</div>")
                    append("<div class='warning-text'>Linux ARM systems are currently not supported by this plugin.</div>")
                    append("</div>")
                }

                if (!jcefSupported) {
                    append("<div class='warning-card error'>")
                    append("<div class='warning-header'>")
                    append("<span class='warning-icon'>❌</span>")
                    append("<span class='warning-title'>JCEF Not Supported</span>")
                    append("</div>")
                    append("<div class='warning-text'>Your IDE runtime does not support JCEF. Please use a runtime that supports JCEF.</div>")
                    append("</div>")
                }

                // Help text
                append("<div class='help-card'>")
                append("<div class='help-text'>")
                append("If this interface continues to display for a long time, you can refer to the known issues documentation to check if there are any known problems.")
                append("</div>")
                append("</div>")

                append("</body></html>")
            }
        }

        /**
         * Detect current IDEA theme
         */
        private fun detectCurrentTheme(): Boolean {
            return try {
                val background = javax.swing.UIManager.getColor("Panel.background")
                if (background != null) {
                    val brightness = (0.299 * background.red + 0.587 * background.green + 0.114 * background.blue) / 255.0
                    brightness < 0.5
                } else {
                    // Default to dark theme if cannot detect
                    true
                }
            } catch (e: Exception) {
                // Default to dark theme on error
                true
            }
        }

        /**
         * Generate theme-adaptive CSS styles
         */
        private fun generateThemeStyles(isDarkTheme: Boolean): String {
            return if (isDarkTheme) {
                """
                body.dark-theme {
                    width: 400px;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                    color: #e2e8f0;
                    border-radius: 12px;
                }
                
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                
                .title {
                    font-size: 24px;
                    font-weight: 600;
                    margin-bottom: 8px;
                    color: #f8fafc;
                }
                
                .subtitle {
                    font-size: 14px;
                    opacity: 0.9;
                    color: #cbd5e1;
                }
                
                .info-card {
                    background: rgba(30, 41, 59, 0.8);
                    backdrop-filter: blur(10px);
                    border-radius: 8px;
                    padding: 20px;
                    margin-bottom: 20px;
                    border: 1px solid rgba(148, 163, 184, 0.2);
                }
                
                .card-title {
                    font-size: 16px;
                    font-weight: 600;
                    margin-bottom: 15px;
                    text-align: center;
                    color: #f1f5f9;
                }
                
                .info-grid {
                    display: grid;
                    gap: 12px;
                }
                
                .info-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 8px 0;
                    border-bottom: 1px solid rgba(148, 163, 184, 0.1);
                }
                
                .info-row:last-child {
                    border-bottom: none;
                }
                
                .info-label {
                    font-weight: 500;
                    opacity: 0.9;
                    color: #cbd5e1;
                }
                
                .info-value {
                    font-weight: 600;
                    color: #f8fafc;
                }
                
                .version-text {
                    font-size: 12px;
                }
                
                .success {
                    color: #10b981;
                }
                
                .error {
                    color: #ef4444;
                }
                
                .warning-card {
                    border-radius: 8px;
                    padding: 16px;
                    margin-bottom: 16px;
                    backdrop-filter: blur(10px);
                }
                
                .warning-card.warning {
                    background: rgba(245, 158, 11, 0.2);
                    border: 1px solid rgba(245, 158, 11, 0.4);
                }
                
                .warning-card.error {
                    background: rgba(239, 68, 68, 0.2);
                    border: 1px solid rgba(239, 68, 68, 0.4);
                }
                
                .warning-header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 8px;
                }
                
                .warning-icon {
                    font-size: 18px;
                    margin-right: 8px;
                }
                
                .warning-title {
                    font-weight: 600;
                    color: #fbbf24;
                }
                
                .warning-card.error .warning-title {
                    color: #f87171;
                }
                
                .warning-text {
                    font-size: 13px;
                    opacity: 0.9;
                    line-height: 1.4;
                    color: #cbd5e1;
                }
                
                .help-card {
                    text-align: center;
                    margin-top: 20px;
                    padding: 16px;
                    background: rgba(30, 41, 59, 0.6);
                    border-radius: 8px;
                    backdrop-filter: blur(10px);
                    border: 1px solid rgba(148, 163, 184, 0.1);
                }
                
                .help-text {
                    font-size: 13px;
                    opacity: 0.9;
                    line-height: 1.5;
                    color: #cbd5e1;
                }
                """.trimIndent()
            } else {
                """
                body.light-theme {
                    width: 400px;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
                    color: #334155;
                    border-radius: 12px;
                }
                
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                
                .title {
                    font-size: 24px;
                    font-weight: 600;
                    margin-bottom: 8px;
                    color: #1e293b;
                }
                
                .subtitle {
                    font-size: 14px;
                    opacity: 0.9;
                    color: #64748b;
                }
                
                .info-card {
                    background: rgba(255, 255, 255, 0.8);
                    backdrop-filter: blur(10px);
                    border-radius: 8px;
                    padding: 20px;
                    margin-bottom: 20px;
                    border: 1px solid rgba(148, 163, 184, 0.2);
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                }
                
                .card-title {
                    font-size: 16px;
                    font-weight: 600;
                    margin-bottom: 15px;
                    text-align: center;
                    color: #1e293b;
                }
                
                .info-grid {
                    display: grid;
                    gap: 12px;
                }
                
                .info-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 8px 0;
                    border-bottom: 1px solid rgba(148, 163, 184, 0.2);
                }
                
                .info-row:last-child {
                    border-bottom: none;
                }
                
                .info-label {
                    font-weight: 500;
                    opacity: 0.9;
                    color: #64748b;
                }
                
                .info-value {
                    font-weight: 600;
                    color: #1e293b;
                }
                
                .version-text {
                    font-size: 12px;
                }
                
                .success {
                    color: #059669;
                }
                
                .error {
                    color: #dc2626;
                }
                
                .warning-card {
                    border-radius: 8px;
                    padding: 16px;
                    margin-bottom: 16px;
                    backdrop-filter: blur(10px);
                    box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1);
                }
                
                .warning-card.warning {
                    background: rgba(245, 158, 11, 0.1);
                    border: 1px solid rgba(245, 158, 11, 0.3);
                }
                
                .warning-card.error {
                    background: rgba(239, 68, 68, 0.1);
                    border: 1px solid rgba(239, 68, 68, 0.3);
                }
                
                .warning-header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 8px;
                }
                
                .warning-icon {
                    font-size: 18px;
                    margin-right: 8px;
                }
                
                .warning-title {
                    font-weight: 600;
                    color: #d97706;
                }
                
                .warning-card.error .warning-title {
                    color: #dc2626;
                }
                
                .warning-text {
                    font-size: 13px;
                    opacity: 0.9;
                    line-height: 1.4;
                    color: #475569;
                }
                
                .help-card {
                    text-align: center;
                    margin-top: 20px;
                    padding: 16px;
                    background: rgba(255, 255, 255, 0.6);
                    border-radius: 8px;
                    backdrop-filter: blur(10px);
                    border: 1px solid rgba(148, 163, 184, 0.1);
                    box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1);
                }
                
                .help-text {
                    font-size: 13px;
                    opacity: 0.9;
                    line-height: 1.5;
                    color: #475569;
                }
                """.trimIndent()
            }
        }

        /**
         * Create system information text in plain text format for copying
         */
        private fun createSystemInfoPlainText(): String {
            val appInfo = ApplicationInfo.getInstance()
            val plugin = PluginManagerCore.getPlugin(PluginId.getId(PluginConstants.PLUGIN_ID))
            val pluginVersion = plugin?.version ?: "unknown"
            val osName = System.getProperty("os.name")
            val osVersion = System.getProperty("os.version")
            val osArch = System.getProperty("os.arch")
            val jcefSupported = JBCefApp.isSupported()

            // Check for Linux ARM system
            val isLinuxArm = osName.lowercase().contains("linux") && (osArch.lowercase().contains("aarch64") || osArch.lowercase().contains("arm"))

            return buildString {
                append("Codex System Information\n")
                append("=============================\n\n")
                append("🚀 Plugin Status: Initializing...\n\n")
                append("📊 System Information:\n")
                append("  💻 CPU Architecture: $osArch\n")
                append("  🖥️ Operating System: $osName $osVersion\n")
                append("  🔧 IDE Version: ${appInfo.fullApplicationName} (build ${appInfo.build})\n")
                append("  📦 Plugin Version: $pluginVersion\n")
                append("  🌐 JCEF Support: ${if (jcefSupported) "✓ Yes" else "✗ No"}\n\n")

                // Add warning messages
                if (isLinuxArm) {
                    append("⚠️ Warning: System Not Supported\n")
                    append("   Linux ARM systems are currently not supported by this plugin.\n\n")
                }

                if (!jcefSupported) {
                    append("❌ Warning: JCEF Not Supported\n")
                    append("   Your IDE runtime does not support JCEF. Please use a runtime that supports JCEF.\n")
                    append("   Please refer to the known issues documentation for more information.\n\n")
                }

                append("💡 Tip: If this interface continues to display for a long time, you can refer to the known issues documentation to check if there are any known problems.\n")
            }
        }

        /**
         * Copy system information to clipboard
         */
        private fun copySystemInfo() {
            val stringSelection = StringSelection(systemInfoText)
            val clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            clipboard.setContents(stringSelection, null)
        }

        // Known Issues button
        private val knownIssuesButton = JButton("📚 Known Issues").apply {
            preferredSize = Dimension(160, 36)
            font = font.deriveFont(14f)
            isOpaque = false
            isFocusPainted = false
            border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
            addActionListener {
                BrowserUtil.browse("https://github.com/Haleclipse/Codex-JetBrains/blob/main/docs/KNOWN_ISSUES.md")
            }
        }

        // Copy button
        private val copyButton = JButton("📋 Copy System Info").apply {
            preferredSize = Dimension(160, 36)
            font = font.deriveFont(14f)
            isOpaque = false
            isFocusPainted = false
            border = javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16)
            addActionListener { copySystemInfo() }
        }

        // Button panel to hold both buttons side by side with modern spacing
        private val buttonPanel = JPanel().apply {
            layout = BorderLayout()
            border = javax.swing.BorderFactory.createEmptyBorder(20, 0, 0, 0)
            add(knownIssuesButton, BorderLayout.WEST)
            add(copyButton, BorderLayout.EAST)
        }

        private var dragDropHandler: DragDropHandler? = null

        // Main panel
        val content: JPanel = JPanel(BorderLayout()).apply {
            // Set content panel with both label and button
            contentPanel.layout = BorderLayout()

            // Check extension installation status and show appropriate content
            if (configManager.isExtensionInstalled()) {
                // Extension is installed, show system info (will auto-start in init)
                contentPanel.add(placeholderLabel, BorderLayout.CENTER)
                contentPanel.add(buttonPanel, BorderLayout.SOUTH)
            } else {
                // Extension not installed, show install panel
                contentPanel.add(pluginSelectionPanel, BorderLayout.CENTER)
            }

            add(contentPanel, BorderLayout.CENTER)
        }

        init {
            // Initialize UI content based on current configuration status
            updateUIContent()

            // Start configuration monitoring
            startConfigurationMonitoring()

            // Add theme change listener
            addThemeChangeListener()

            // Try to get existing WebView
            webViewManager.getLatestWebView()?.let { webView ->
                // Add WebView component immediately when created
                ApplicationManager.getApplication().invokeLater {
                    addWebViewComponent(webView)
                }
                // Set page load callback to hide system info only after page is loaded
                webView.setPageLoadCallback {
                    ApplicationManager.getApplication().invokeLater {
                        hideSystemInfo()
                    }
                }
                // If page is already loaded, hide system info immediately
                if (webView.isPageLoaded()) {
                    ApplicationManager.getApplication().invokeLater {
                        hideSystemInfo()
                    }
                }
            }?:webViewManager.addCreationCallback(this, toolWindow.disposable)
        }

        /**
         * Add theme change listener to automatically update UI when theme changes
         */
        private fun addThemeChangeListener() {
            try {
                val messageBus = ApplicationManager.getApplication().messageBus
                val connection = messageBus.connect(toolWindow.disposable)
                connection.subscribe(com.intellij.ide.ui.LafManagerListener.TOPIC, com.intellij.ide.ui.LafManagerListener {
                    logger.info("Theme changed, updating UI styles")
                    // Update UI content with new theme
                    ApplicationManager.getApplication().invokeLater {
                        updateUIContent()
                        // Update status panel if it exists
                        if (configStatusPanel.componentCount > 0) {
                            updateConfigStatus(configStatusPanel.getComponent(0) as JLabel)
                        }
                    }
                })
                logger.info("Theme change listener added successfully")
            } catch (e: Exception) {
                logger.error("Failed to add theme change listener", e)
            }
        }

        /**
         * Start configuration monitoring to detect extension installation changes
         */
        private fun startConfigurationMonitoring() {
            // Start background monitoring thread
            Thread {
                try {
                    var lastInstallState = configManager.isExtensionInstalled()

                    while (!project.isDisposed) {
                        Thread.sleep(2000) // Check every 2 seconds

                        if (!project.isDisposed && !isPluginStarting) {
                            val currentInstallState = configManager.isExtensionInstalled()

                            // Only trigger update if install state changed (installed -> uninstalled or vice versa)
                            if (currentInstallState != lastInstallState) {
                                logger.info("Extension installation state changed: $lastInstallState -> $currentInstallState")
                                lastInstallState = currentInstallState

                                ApplicationManager.getApplication().invokeLater {
                                    updateUIContent()
                                }
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    logger.info("Configuration monitoring interrupted")
                } catch (e: Exception) {
                    logger.error("Error in configuration monitoring", e)
                }
            }.apply {
                isDaemon = true
                name = "Codex-ConfigMonitor-UI"
                start()
            }
        }

        /**
         * WebView creation callback implementation
         */
        override fun onWebViewCreated(instance: WebViewInstance) {
            // Add WebView component immediately when created
            ApplicationManager.getApplication().invokeLater {
                addWebViewComponent(instance)
            }
            // Set page load callback to hide system info only after page is loaded
            instance.setPageLoadCallback {
                // Ensure UI update in EDT thread
                ApplicationManager.getApplication().invokeLater {
                    hideSystemInfo()
                }
            }
        }

        /**
         * Add WebView component to UI
         */
        private fun addWebViewComponent(webView: WebViewInstance) {
            logger.info("Adding WebView component to UI: ${webView.viewType}/${webView.viewId}")

            // Check if WebView component is already added
            val components = contentPanel.components
            for (component in components) {
                if (component === webView.browser.component) {
                    logger.info("WebView component already exists in UI")
                    return
                }
            }

            // Add WebView component without removing existing components
            contentPanel.add(webView.browser.component, BorderLayout.CENTER)

            setupDragAndDropSupport(webView)

            // Relayout
            contentPanel.revalidate()
            contentPanel.repaint()

            logger.info("WebView component added to tool window")
        }

        /**
         * Hide system info placeholder
         */
        private fun hideSystemInfo() {
            logger.info("Hiding system info placeholder")

            // Remove all components from content panel except WebView component
            val components = contentPanel.components
            for (component in components) {
                if (component !== webViewManager.getLatestWebView()?.browser?.component) {
                    contentPanel.remove(component)
                }
            }

            // Relayout
            contentPanel.revalidate()
            contentPanel.repaint()

            logger.info("System info placeholder hidden")
        }

        /**
         * Setup drag and drop support
         */
        private fun setupDragAndDropSupport(webView: WebViewInstance) {
            try {
                logger.info("Setting up drag and drop support for WebView")

                dragDropHandler = DragDropHandler(webView, contentPanel)

                dragDropHandler?.setupDragAndDrop()

                logger.info("Drag and drop support enabled")
            } catch (e: Exception) {
                logger.error("Failed to setup drag and drop support", e)
            }
        }

        /**
         * Create extension install panel (simplified - Codex only)
         * 简化为单纯的安装提示界面
         */
        private fun createPluginSelectionPanel(): JPanel {
            val isDarkTheme = detectCurrentTheme()

            val panel = JPanel()
            panel.layout = BorderLayout()
            panel.border = javax.swing.BorderFactory.createEmptyBorder(40, 40, 40, 40)

            // 中央内容面板
            val centerPanel = JPanel()
            centerPanel.layout = javax.swing.BoxLayout(centerPanel, javax.swing.BoxLayout.Y_AXIS)
            centerPanel.isOpaque = false

            // 图标/标题
            val titleLabel = JLabel("📦 安装 Codex 扩展").apply {
                font = font.deriveFont(20f).deriveFont(java.awt.Font.BOLD)
                foreground = if (isDarkTheme) java.awt.Color(0xF8, 0xFA, 0xFC) else java.awt.Color(0x1E, 0x29, 0x3B)
                alignmentX = java.awt.Component.CENTER_ALIGNMENT
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 20, 0)
            }

            // 说明文字
            val descLabel = JLabel("<html><center>需要安装 OpenAI Codex 扩展才能使用此插件。<br><br>请点击下方按钮选择 VSIX 文件进行安装。</center></html>").apply {
                font = font.deriveFont(14f)
                foreground = if (isDarkTheme) java.awt.Color(0xCB, 0xD5, 0xE1) else java.awt.Color(0x47, 0x56, 0x69)
                alignmentX = java.awt.Component.CENTER_ALIGNMENT
                horizontalAlignment = javax.swing.SwingConstants.CENTER
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 30, 0)
            }

            // 安装按钮
            val installButton = JButton("📦 选择 VSIX 文件安装").apply {
                preferredSize = JBUI.size(220, 44)
                maximumSize = JBUI.size(220, 44)
                font = JBFont.label().deriveFont(15f).deriveFont(java.awt.Font.BOLD)
                isFocusPainted = false
                alignmentX = java.awt.Component.CENTER_ALIGNMENT
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                addActionListener {
                    uploadVsixForPlugin("codex", "OpenAI Codex")
                }
            }

            // 提示文字
            val hintLabel = JLabel("<html><center>下载地址: <font color='#3B82F6'>vsixhub.com/vsix/163404</font><br>安装路径: ~/.cometix/plugins/codex/</center></html>").apply {
                font = font.deriveFont(12f)
                foreground = if (isDarkTheme) java.awt.Color(0x94, 0xA3, 0xB8) else java.awt.Color(0x64, 0x74, 0x8B)
                alignmentX = java.awt.Component.CENTER_ALIGNMENT
                horizontalAlignment = javax.swing.SwingConstants.CENTER
                border = javax.swing.BorderFactory.createEmptyBorder(20, 0, 0, 0)
            }

            // 组装中央面板
            centerPanel.add(javax.swing.Box.createVerticalGlue())
            centerPanel.add(titleLabel)
            centerPanel.add(descLabel)
            centerPanel.add(installButton)
            centerPanel.add(hintLabel)
            centerPanel.add(javax.swing.Box.createVerticalGlue())

            panel.add(centerPanel, BorderLayout.CENTER)

            return panel
        }

        /**
         * Upload VSIX file for Codex extension
         * Uses VsixUploadDialog for file selection
         */
        private fun uploadVsixForPlugin(pluginId: String, pluginName: String) {
            try {
                // Use VsixUploadDialog directly
                val success = VsixUploadDialog.show(project, pluginId, pluginName)

                if (success) {
                    logger.info("VSIX installed successfully for $pluginId")
                    javax.swing.JOptionPane.showMessageDialog(
                        contentPanel,
                        "✅ $pluginName 扩展安装成功！\n\n安装路径: ~/.cometix/plugins/$pluginId/\n\n插件将自动启动。",
                        "安装成功",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                    )
                    // Refresh UI and start plugin
                    updateUIContent()
                }
            } catch (e: Exception) {
                logger.error("Failed to upload VSIX for plugin: $pluginId", e)
                javax.swing.JOptionPane.showMessageDialog(
                    contentPanel,
                    "❌ 安装过程出错: ${e.message}",
                    "错误",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                )
            }
        }
        
        /**
         * Create configuration status panel
         */
        private fun createConfigStatusPanel(): JPanel {
            val panel = JPanel()
            panel.layout = BorderLayout()
            panel.border = javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)
            
            // Status label
            val statusLabel = JLabel().apply {
                font = font.deriveFont(14f)
                horizontalAlignment = javax.swing.SwingConstants.CENTER
            }
            
            // Update status
            updateConfigStatus(statusLabel)
            
            panel.add(statusLabel, BorderLayout.CENTER)
            return panel
        }
        
        /**
         * Update configuration status
         */
        private fun updateConfigStatus(statusLabel: JLabel) {
            // Detect current theme for status colors
            val isDarkTheme = detectCurrentTheme()
            
            if (configManager.isConfigurationLoaded()) {
                if (configManager.isConfigurationValid()) {
                    val extensionId = configManager.getCurrentExtensionId()
                    // Check if plugin is actually running
                    val isPluginRunning = isPluginActuallyRunning()
                    
                    if (isPluginRunning) {
                        statusLabel.text = "✅ Plugin Running - Current Plugin: $extensionId"
                        statusLabel.foreground = getThemeAdaptiveColor(isDarkTheme, "success")
                    } else {
                        statusLabel.text = "⚠️ Configuration Valid but Plugin Not Running - Current Plugin: $extensionId"
                        statusLabel.foreground = getThemeAdaptiveColor(isDarkTheme, "warning")
                    }
                } else {
                    statusLabel.text = "❌ Configuration Invalid - ${configManager.getConfigurationError()}"
                    statusLabel.foreground = getThemeAdaptiveColor(isDarkTheme, "error")
                }
            } else {
                statusLabel.text = "⏳ Loading Configuration..."
                statusLabel.foreground = getThemeAdaptiveColor(isDarkTheme, "info")
            }
        }

        /**
         * Get theme-adaptive color for status indicators
         */
        private fun getThemeAdaptiveColor(isDarkTheme: Boolean, colorType: String): java.awt.Color {
            return if (isDarkTheme) {
                when (colorType) {
                    "success" -> java.awt.Color(16, 185, 129) // Green for dark theme
                    "warning" -> java.awt.Color(251, 191, 36) // Yellow for dark theme
                    "error" -> java.awt.Color(239, 68, 68)   // Red for dark theme
                    "info" -> java.awt.Color(59, 130, 246)   // Blue for dark theme
                    else -> java.awt.Color(148, 163, 184)    // Default gray for dark theme
                }
            } else {
                when (colorType) {
                    "success" -> java.awt.Color(5, 150, 105)  // Green for light theme
                    "warning" -> java.awt.Color(217, 119, 6)  // Yellow for light theme
                    "error" -> java.awt.Color(220, 38, 38)    // Red for light theme
                    "info" -> java.awt.Color(37, 99, 235)     // Blue for light theme
                    else -> java.awt.Color(100, 116, 139)     // Default gray for light theme
                }
            }
        }
        
        /**
         * Apply plugin selection and create configuration
         */
        private fun applyPluginSelection(pluginId: String) {
            try {
                logger.info("Applying plugin selection: $pluginId")
                
                // Create configuration with selected plugin
                configManager.setCurrentExtensionId(pluginId)
                
                // Verify configuration was saved successfully
                if (configManager.isConfigurationValid()) {
                    // Start the plugin directly instead of just saving configuration
                    startPluginAfterSelection(pluginId)
                    
                    logger.info("Plugin selection applied successfully: $pluginId")
                } else {
                    // Configuration is still invalid after setting
                    val errorMsg = configManager.getConfigurationError() ?: "Unknown error"
                    val message = "❌ Configuration Update Failed\nError: $errorMsg\n\nPlease check the configuration file or try manual configuration."
                    javax.swing.JOptionPane.showMessageDialog(
                        contentPanel,
                        message,
                        "Configuration Update Failed",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    )
                    
                    logger.error("Configuration is still invalid after setting extension ID: $pluginId, error: $errorMsg")
                }
            } catch (e: Exception) {
                logger.error("Failed to apply plugin selection", e)
                val message = "❌ Configuration Update Failed\nError: ${e.message}\n\nPlease check file permissions or try manual configuration."
                javax.swing.JOptionPane.showMessageDialog(
                    contentPanel,
                    message,
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                )
            }
        }
        
        /**
         * Start plugin after plugin selection
         */
        private fun startPluginAfterSelection(pluginId: String) {
            try {
                logger.info("Starting plugin after selection: $pluginId")
                
                // Set plugin starting state
                isPluginStarting = true
                
                // Update status to show plugin is starting
                updateConfigStatus(configStatusPanel.getComponent(0) as JLabel)
                
                // Get extension manager and set the selected provider
                val extensionManager = ExtensionManager.getInstance(project)
                extensionManager.initialize(pluginId)
                
                // Initialize the current provider
                extensionManager.initializeCurrentProvider()
                
                // Get plugin service and restart extension host
                val pluginService = WecoderPlugin.getInstance(project)
                if (pluginService.isInitialized()) {
                    // Service already initialized, just restart extension host
                    logger.info("Plugin service already initialized, restarting extension host")
                    pluginService.restartExtensionHost()
                } else {
                    // First time initialization
                    logger.info("First time plugin service initialization")
                    pluginService.initialize(project)
                }

                // Initialize WebViewManager
                val webViewManager = project.getService(WebViewManager::class.java)
                if (webViewManager != null) {
                    // Register to project Disposer
                    com.intellij.openapi.util.Disposer.register(project, webViewManager)
                    
                    // Start configuration monitoring
                    startConfigurationMonitoring()
                    
                    // Register project-level resource disposal
                    com.intellij.openapi.util.Disposer.register(project, com.intellij.openapi.Disposable {
                        logger.info("Disposing Codex plugin for project: ${project.name}")
                        pluginService.dispose()
                        extensionManager.dispose()
                        SystemObjectProvider.dispose()
                        // Reset state when disposing
                        isPluginRunning = false
                        isPluginStarting = false
                    })
                    
                    logger.info("Plugin started successfully after selection: $pluginId")
                    
                    // Set plugin running state
                    isPluginRunning = true
                    isPluginStarting = false
                    
                    // Update UI to show plugin is running
                    updateUIContent()
                } else {
                    logger.error("WebViewManager not available")
                    throw IllegalStateException("WebViewManager not available")
                }
                
            } catch (e: Exception) {
                logger.error("Failed to start plugin after selection", e)
                // Reset state on failure
                isPluginStarting = false
                isPluginRunning = false
                
                val message = "❌ Plugin Startup Failed\nError: ${e.message}\n\nPlease check plugin configuration or try restarting the IDE."
                javax.swing.JOptionPane.showMessageDialog(
                    contentPanel,
                    message,
                    "Plugin Startup Failed",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                )
            }
        }
        
        /**
         * Update UI content based on extension installation status
         */
        private fun updateUIContent() {
            // Don't update UI if plugin is starting
            if (isPluginStarting) {
                logger.info("Plugin is starting, skipping UI update")
                return
            }

            // Check if Codex extension is installed
            val isExtensionInstalled = configManager.isExtensionInstalled()
            logger.info("Extension installed: $isExtensionInstalled, plugin running: $isPluginRunning")

            // If plugin is already running and WebView is showing, don't change UI
            if (isPluginRunning && webViewManager.getLatestWebView() != null) {
                logger.info("Plugin is running with WebView, keeping current UI content")
                return
            }

            contentPanel.removeAll()

            if (isExtensionInstalled) {
                // Extension is installed, show system info and auto-start
                logger.info("Extension is installed, showing system info and auto-starting plugin")
                contentPanel.add(placeholderLabel, BorderLayout.CENTER)
                contentPanel.add(buttonPanel, BorderLayout.SOUTH)
                contentPanel.revalidate()
                contentPanel.repaint()

                // Auto-start plugin after UI is ready
                javax.swing.SwingUtilities.invokeLater {
                    if (!this.isPluginRunning && !isPluginStarting) {
                        startPluginAfterSelection("codex")
                    }
                }
            } else {
                // Extension not installed, show install panel
                logger.info("Extension not installed, showing install panel")
                // Recreate plugin selection panel to refresh theme
                val freshPluginSelectionPanel = createPluginSelectionPanel()
                contentPanel.add(freshPluginSelectionPanel, BorderLayout.CENTER)
                contentPanel.revalidate()
                contentPanel.repaint()
            }
        }
        
        /**
         * Show manual configuration instructions
         */
        private fun showManualConfigInstructions() {
            val instructions = """
                📝 Manual Configuration Instructions
                
                1. Create configuration file in user home directory: ${PluginConstants.ConfigFiles.getMainConfigPath()}
                2. Add the following content:
                   ${PluginConstants.ConfigFiles.EXTENSION_TYPE_KEY}=roo-code
                   
                3. Supported plugin types:
                   - roo-code: Roo Code AI Assistant
                   - cline: Cline AI Assistant
                   - custom: Custom Plugin
                   
                4. Save the file and restart IDE
                
                Configuration file path: ${configManager.getConfigurationFilePath()}
            """.trimIndent()
            
            javax.swing.JOptionPane.showMessageDialog(
                contentPanel,
                instructions,
                "Manual Configuration Instructions",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
        }

        /**
         * Show debug information
         */
        private fun showDebugInfo() {
            val debugText = """
                Codex Debug Information
                =======================
                
                🚀 Plugin Status: ${if (configManager.isConfigurationLoaded() && configManager.isConfigurationValid()) "Loaded and Valid" else "Not Loaded or Invalid"}
                
                📝 Current Configuration: ${configManager.getCurrentExtensionId() ?: "Not Set"}
                
                ⚙️ Configuration File Path: ${configManager.getConfigurationFilePath()}
                
                🔄 Configuration Load Time: ${configManager.getConfigurationLoadTime()?.let { it.toString() } ?: "Unknown"}
                
                💡 Tip: If configuration is invalid, please check the configuration file content or try manual configuration.
            """.trimIndent()
            
            javax.swing.JOptionPane.showMessageDialog(
                contentPanel,
                debugText,
                "Debug Information",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
        }
    }
}