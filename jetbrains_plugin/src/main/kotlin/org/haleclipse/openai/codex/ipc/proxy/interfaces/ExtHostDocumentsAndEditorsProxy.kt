// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.ipc.proxy.interfaces

import org.haleclipse.openai.codex.editor.DocumentsAndEditorsDelta


interface ExtHostDocumentsAndEditorsProxy {
    fun acceptDocumentsAndEditorsDelta(d: DocumentsAndEditorsDelta)
}