// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.ipc.proxy.interfaces

import org.haleclipse.openai.codex.editor.EditorPropertiesChangeData
import org.haleclipse.openai.codex.editor.TextEditorDiffInformation


interface ExtHostEditorsProxy {
    fun acceptEditorPropertiesChanged(id: String, props: EditorPropertiesChangeData)
    fun acceptEditorPositionData(data: Map<String , Int>)
    fun acceptEditorDiffInformation(id: String, diffInformation: List<TextEditorDiffInformation>?)
}