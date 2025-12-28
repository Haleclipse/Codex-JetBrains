// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.ipc.proxy.interfaces

import org.haleclipse.openai.codex.editor.EditorTabGroupDto
import org.haleclipse.openai.codex.editor.TabOperation

interface ExtHostEditorTabsProxy {
    fun acceptEditorTabModel(tabGroups: List<EditorTabGroupDto>)
    fun acceptTabGroupUpdate(groupDto: EditorTabGroupDto)
    fun acceptTabOperation(operation: TabOperation)
}
