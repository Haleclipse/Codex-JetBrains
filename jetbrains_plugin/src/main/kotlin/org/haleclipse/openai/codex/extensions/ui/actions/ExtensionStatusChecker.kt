// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.extensions.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.StringSelection
import javax.swing.*
import org.haleclipse.openai.codex.extensions.core.ExtensionManager
import org.haleclipse.openai.codex.core.PluginContext
import org.haleclipse.openai.codex.core.ServiceProxyRegistry
import org.haleclipse.openai.codex.util.ProxyConfigUtil
import org.haleclipse.openai.codex.webview.WebViewManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.ui.jcef.JBCefApp
import com.intellij.openapi.application.ApplicationInfo
import org.haleclipse.openai.codex.util.PluginConstants

/**
 * Action to check extension status and diagnose issues
 */
class ExtensionStatusChecker : AnAction("Check Extension Status") {
    
    private val logger = Logger.getInstance(ExtensionStatusChecker::class.java)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        
        val status = checkExtensionStatus(project)
        showStatusDialog(status)
    }
    
    private fun checkExtensionStatus(project: Project): String {
        val sb = StringBuilder()
        sb.appendLine("🔍 Extension Status Check")
        sb.appendLine("=".repeat(50))
        
        // Add System Information
        addSystemInformation(sb)
        
        // Check Extension Manager
        try {
            val extensionManager = ExtensionManager.getInstance(project)
            val currentProvider = extensionManager.getCurrentProvider()
            sb.appendLine("📋 Current Extension Provider: ${currentProvider?.getExtensionId() ?: "None"}")
            sb.appendLine("📋 Current Extension Name: ${currentProvider?.getDisplayName() ?: "None"}")
        } catch (e: Exception) {
            sb.appendLine("❌ Extension Manager Error: ${e.message}")
        }
        
        // Check Plugin Context
        try {
            val pluginContext = project.getService(PluginContext::class.java)
            if (pluginContext != null) {
                sb.appendLine("✅ PluginContext: Available")
                
                val rpcProtocol = pluginContext.getRPCProtocol()
                if (rpcProtocol != null) {
                    sb.appendLine("✅ RPC Protocol: Available")
                    
                    val commandsProxy = rpcProtocol.getProxy(ServiceProxyRegistry.ExtHostContext.ExtHostCommands)
                    if (commandsProxy != null) {
                        sb.appendLine("✅ ExtHostCommands Proxy: Available")
                    } else {
                        sb.appendLine("❌ ExtHostCommands Proxy: Not Available")
                    }
                } else {
                    sb.appendLine("❌ RPC Protocol: Not Available")
                }
            } else {
                sb.appendLine("❌ PluginContext: Not Available")
            }
        } catch (e: Exception) {
            sb.appendLine("❌ Plugin Context Error: ${e.message}")
        }
        
        // Check available extensions
        try {
            val extensionManager = ExtensionManager.getInstance(project)
            val availableProviders = extensionManager.getAvailableProviders()
            sb.appendLine("\n📋 Available Extensions:")
            availableProviders.forEach { provider ->
                sb.appendLine("  - ${provider.getExtensionId()}: ${provider.getDisplayName()}")
            }
        } catch (e: Exception) {
            sb.appendLine("❌ Error getting available extensions: ${e.message}")
        }
        
        // Check WebView status
        try {
            val webViewManager = project.getService(WebViewManager::class.java)
            if (webViewManager != null) {
                sb.appendLine("\n🌐 WebView Status:")
                val latestWebView = webViewManager.getLatestWebView()
                if (latestWebView != null) {
                    sb.appendLine("✅ Latest WebView: Available")
                } else {
                    sb.appendLine("❌ Latest WebView: Not Available")
                }
            } else {
                sb.appendLine("\n❌ WebView Manager: Not Available")
            }
        } catch (e: Exception) {
            sb.appendLine("\n❌ WebView Status Error: ${e.message}")
        }
        
        // Check Proxy status
        try {
            val proxyConfig = ProxyConfigUtil.getProxyConfig()
            sb.appendLine("\n🌐 Proxy Status:")
            
            val sourceDescription = when (proxyConfig.source) {
                "ide-pac" -> "IDE Settings (PAC)"
                "ide-http" -> "IDE Settings (HTTP Proxy)"
                "ide-none" -> "IDE Settings (No Proxy)"
                "env" -> "Environment Variables"
                "none" -> "No Proxy Configuration"
                "ide-error" -> "IDE Settings (Error)"
                "env-error" -> "Environment Variables (Error)"
                else -> proxyConfig.source
            }
            sb.appendLine("  Source: $sourceDescription")
            
            if (proxyConfig.hasProxy) {
                if (!proxyConfig.pacUrl.isNullOrEmpty()) {
                    sb.appendLine("  PAC URL: ${proxyConfig.pacUrl}")
                } else if (!proxyConfig.proxyUrl.isNullOrEmpty()) {
                    sb.appendLine("  Proxy URL: ${proxyConfig.proxyUrl}")
                }
                
                if (!proxyConfig.proxyExceptions.isNullOrEmpty()) {
                    sb.appendLine(" No Proxy For: ${proxyConfig.proxyExceptions}")
                }
            } else {
                sb.appendLine("  No proxy configuration found")
            }
        } catch (e: Exception) {
            sb.appendLine("\n❌ Proxy Status Error: ${e.message}")
        }
        
        return sb.toString()
    }
    
    /**
     * Add system information to the status report
     */
    private fun addSystemInformation(sb: StringBuilder) {
        try {
            val appInfo = ApplicationInfo.getInstance()
            val plugin = PluginManagerCore.getPlugin(PluginId.getId(PluginConstants.PLUGIN_ID))
            val pluginVersion = plugin?.version ?: "unknown"
            val osName = System.getProperty("os.name")
            val osVersion = System.getProperty("os.version")
            val osArch = System.getProperty("os.arch")
            val jcefSupported = JBCefApp.isSupported()
            
            // Check for Linux ARM system
            val isLinuxArm = osName.lowercase().contains("linux") && (osArch.lowercase().contains("aarch64") || osArch.lowercase().contains("arm"))
            
            sb.appendLine("\n📊 System Information:")
            sb.appendLine("  💻 CPU Architecture: $osArch")
            sb.appendLine("  🖥️ Operating System: $osName $osVersion")
            sb.appendLine("  🔧 IDE Version: ${appInfo.fullApplicationName} (build ${appInfo.build})")
            sb.appendLine("  📦 Plugin Version: $pluginVersion")
            sb.appendLine("  🌐 JCEF Support: ${if (jcefSupported) "✅ Yes" else "❌ No"}")
            
            // Add warnings for unsupported configurations
            if (isLinuxArm) {
                sb.appendLine("  ⚠️ Warning: Linux ARM systems are currently not supported")
            }
            
            if (!jcefSupported) {
                sb.appendLine("  ❌ Warning: JCEF not supported - WebView functionality may not work")
            }
            
        } catch (e: Exception) {
            sb.appendLine("\n❌ System Information Error: ${e.message}")
        }
    }
    
    private fun showStatusDialog(status: String) {
        val dialog = ExtensionStatusDialog(status)
        dialog.show()
    }
    
    private class ExtensionStatusDialog(private val statusText: String) : DialogWrapper(true) {
        
        init {
            title = "Extension Status"
            init()
        }
        
        override fun createCenterPanel(): JComponent {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            
            val textArea = JTextArea(statusText)
            textArea.isEditable = false
            textArea.font = JLabel().font
            textArea.background = JLabel().background
            
            val scrollPane = JScrollPane(textArea)
            scrollPane.preferredSize = java.awt.Dimension(600, 400)
            scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            
            panel.add(scrollPane)
            return panel
        }
        
        override fun createActions(): Array<Action> {
            val copyAction = object : AbstractAction("Copy to Clipboard") {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                    val selection = StringSelection(statusText)
                    CopyPasteManager.getInstance().setContents(selection)
                    Messages.showInfoMessage("Status information copied to clipboard!", "Copied")
                }
            }
            
            return arrayOf(copyAction, okAction)
        }
    }
}
