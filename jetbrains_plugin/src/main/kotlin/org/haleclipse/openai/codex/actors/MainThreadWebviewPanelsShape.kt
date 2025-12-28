// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.actors

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger

/**
 * MainThreadWebviewPanels interface
 * Corresponds to the MainThreadWebviewPanelsShape interface in VSCode.
 *
 * WebviewPanels 用于在编辑器区域创建独立的 WebView 面板（标签页）
 * CustomEditors 内部使用 WebviewPanels 来显示自定义编辑器内容
 */
interface MainThreadWebviewPanelsShape : Disposable {
    fun createWebviewPanel(
        extension: Map<String, Any?>,
        handle: String,
        viewType: String,
        initData: Map<String, Any?>,
        showOptions: Map<String, Any?>
    )

    fun disposeWebview(handle: String)

    fun reveal(handle: String, showOptions: Map<String, Any?>)

    fun setTitle(handle: String, value: String)

    fun setIconPath(handle: String, value: Map<String, Any?>?)

    fun registerSerializer(viewType: String, options: Map<String, Any?>)

    fun unregisterSerializer(viewType: String)
}

/**
 * MainThreadWebviewPanels stub implementation
 *
 * 与 MainThreadWebviewViews（侧边栏）不同，WebviewPanels 是在编辑器标签页中显示的独立面板
 *
 * TODO: 完整实现需要：
 * 1. 创建 JetBrains FileEditor 包装 JCEF WebView
 * 2. 管理面板生命周期（创建、显示、隐藏、销毁）
 * 3. 实现状态序列化/反序列化（用于恢复会话）
 */
class MainThreadWebviewPanels : MainThreadWebviewPanelsShape {
    private val logger = Logger.getInstance(MainThreadWebviewPanels::class.java)

    // 已创建的面板
    private val panels = mutableMapOf<String, PanelInfo>()
    // 已注册的序列化器
    private val serializers = mutableMapOf<String, Map<String, Any?>>()

    data class PanelInfo(
        val handle: String,
        val viewType: String,
        val title: String,
        val extension: Map<String, Any?>
    )

    override fun createWebviewPanel(
        extension: Map<String, Any?>,
        handle: String,
        viewType: String,
        initData: Map<String, Any?>,
        showOptions: Map<String, Any?>
    ) {
        val title = initData["title"] as? String ?: viewType
        logger.info("Creating webview panel: handle=$handle, viewType=$viewType, title=$title (stub)")
        panels[handle] = PanelInfo(handle, viewType, title, extension)
        // TODO: 实际创建 JetBrains 编辑器标签页
    }

    override fun disposeWebview(handle: String) {
        logger.info("Disposing webview panel: handle=$handle")
        panels.remove(handle)
    }

    override fun reveal(handle: String, showOptions: Map<String, Any?>) {
        logger.info("Revealing webview panel: handle=$handle (stub)")
        // TODO: 激活/显示对应的编辑器标签页
    }

    override fun setTitle(handle: String, value: String) {
        logger.info("Setting webview panel title: handle=$handle, title=$value")
        panels[handle]?.let {
            panels[handle] = it.copy(title = value)
        }
    }

    override fun setIconPath(handle: String, value: Map<String, Any?>?) {
        logger.debug("Setting webview panel icon: handle=$handle (stub)")
    }

    override fun registerSerializer(viewType: String, options: Map<String, Any?>) {
        logger.info("Registering webview serializer: viewType=$viewType")
        serializers[viewType] = options
    }

    override fun unregisterSerializer(viewType: String) {
        logger.info("Unregistering webview serializer: viewType=$viewType")
        serializers.remove(viewType)
    }

    override fun dispose() {
        logger.info("Disposing MainThreadWebviewPanels")
        panels.clear()
        serializers.clear()
    }
}
