// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.ipc.proxy.interfaces

import org.haleclipse.openai.codex.util.URI


interface ExtHostTerminalShellIntegrationProxy {
    fun shellIntegrationChange(instanceId: Int)
    fun shellExecutionStart(instanceId: Int, commandLineValue: String, commandLineConfidence: Int, isTrusted: Boolean, cwd: URI? )
    fun shellExecutionEnd(instanceId: Int, commandLineValue: String, commandLineConfidence: Int, isTrusted: Boolean, exitCode: Int?)
    fun shellExecutionData(instanceId: Int, data: String)
    fun shellEnvChange(instanceId: Int, shellEnvKeys: Array<String>, shellEnvValues: Array<String>, isTrusted: Boolean)
    fun cwdChange(instanceId: Int, cwd: URI?)
    fun closeTerminal(instanceId: Int)
}