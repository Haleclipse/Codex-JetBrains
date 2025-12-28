// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.ipc.proxy.interfaces

import org.haleclipse.openai.codex.actors.WebviewHandle
import org.haleclipse.openai.codex.ipc.proxy.SerializableObjectWithBuffers

/**
 * Extension host Webviews shape interface
 * Corresponds to ExtHostWebviewsShape interface in TypeScript
 */
interface ExtHostWebviewsProxy {
    /**
     * Handle message received from Webview
     * Corresponds to $onMessage method in TypeScript interface
     *
     * @param handle Webview handle
     * @param jsonSerializedMessage JSON serialized message
     * @param buffers Array of binary buffers
     */
    fun onMessage(
        handle: WebviewHandle,
        jsonSerializedMessage: String,
        buffers: SerializableObjectWithBuffers<List<ByteArray>>
    )

    /**
     * Handle missing Content Security Policy (CSP)
     * Corresponds to $onMissingCsp method in TypeScript interface
     *
     * @param handle Webview handle
     * @param extensionId Extension ID
     */
    fun onMissingCsp(
        handle: WebviewHandle,
        extensionId: String
    )
} 