// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.actors

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger

/**
 * MainThreadCustomEditors interface
 * Corresponds to the MainThreadCustomEditorsShape interface in VSCode.
 *
 * Custom Editors 用于在编辑器标签页中显示自定义内容（如 Codex 对话任务）
 */
interface MainThreadCustomEditorsShape : Disposable {
    fun registerTextEditorProvider(
        extension: Map<String, Any?>,
        viewType: String,
        options: Map<String, Any?>,
        capabilities: Map<String, Any?>,
        serializeBuffersForPostMessage: Boolean
    )

    fun registerCustomEditorProvider(
        extension: Map<String, Any?>,
        viewType: String,
        options: Map<String, Any?>,
        supportsMultipleEditorsPerDocument: Boolean,
        serializeBuffersForPostMessage: Boolean
    )

    fun unregisterEditorProvider(viewType: String)

    fun onDidEdit(resource: Map<String, Any?>, viewType: String, editId: Int, label: String?)

    fun onContentChange(resource: Map<String, Any?>, viewType: String)
}

/**
 * MainThreadCustomEditors stub implementation
 *
 * TODO: 完整实现需要以下工作：
 * 1. 创建 CustomEditorPanel 类 - 在 JetBrains 编辑器区域创建 WebView 标签页
 * 2. 实现 VirtualFileSystem - 处理 openai-codex:// URI scheme
 * 3. 实现 FileEditorProvider - 注册自定义文件编辑器
 * 4. 实现 ExtHostCustomEditors 代理 - 处理 $resolveCustomEditor 回调
 * 5. 实现文档模型 - 脏状态、保存、撤销/重做
 */
class MainThreadCustomEditors : MainThreadCustomEditorsShape {
    private val logger = Logger.getInstance(MainThreadCustomEditors::class.java)

    // 存储已注册的 editor providers
    private val registeredProviders = mutableMapOf<String, EditorProviderInfo>()

    data class EditorProviderInfo(
        val extension: Map<String, Any?>,
        val viewType: String,
        val options: Map<String, Any?>,
        val isTextEditor: Boolean
    )

    override fun registerTextEditorProvider(
        extension: Map<String, Any?>,
        viewType: String,
        options: Map<String, Any?>,
        capabilities: Map<String, Any?>,
        serializeBuffersForPostMessage: Boolean
    ) {
        logger.info("Registering text editor provider: viewType=$viewType (stub - not implemented)")
        registeredProviders[viewType] = EditorProviderInfo(extension, viewType, options, true)
    }

    override fun registerCustomEditorProvider(
        extension: Map<String, Any?>,
        viewType: String,
        options: Map<String, Any?>,
        supportsMultipleEditorsPerDocument: Boolean,
        serializeBuffersForPostMessage: Boolean
    ) {
        logger.info("Registering custom editor provider: viewType=$viewType (stub - not implemented)")
        registeredProviders[viewType] = EditorProviderInfo(extension, viewType, options, false)
    }

    override fun unregisterEditorProvider(viewType: String) {
        logger.info("Unregistering editor provider: viewType=$viewType")
        registeredProviders.remove(viewType)
    }

    override fun onDidEdit(resource: Map<String, Any?>, viewType: String, editId: Int, label: String?) {
        logger.debug("onDidEdit: viewType=$viewType, editId=$editId (stub)")
    }

    override fun onContentChange(resource: Map<String, Any?>, viewType: String) {
        logger.debug("onContentChange: viewType=$viewType (stub)")
    }

    override fun dispose() {
        logger.info("Disposing MainThreadCustomEditors")
        registeredProviders.clear()
    }
}
