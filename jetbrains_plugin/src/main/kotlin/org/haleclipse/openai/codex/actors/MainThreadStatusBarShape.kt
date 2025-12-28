// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package org.haleclipse.openai.codex.actors

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import java.net.URI

/**
 * Main thread search service interface.
 * Corresponds to the MainThreadSearchShape interface in VSCode.
 */
interface MainThreadStatusBarShape : Disposable {
    fun setEntry(
        entryId: String,
        id: String,
        extensionId: String?,
        name: String,
        text: String,
        tooltip: Any?,
        hasTooltipProvider: Boolean,
        command: Map<Any, Any>?,
        color: Any?,
        backgroundColor: Any?,
        alignLeft: Boolean,
        priority: Number?,
        accessibilityInformation: Any?
    )

}

/**
 * Implementation of the main thread search service.
 * Provides search-related functionality for the IDEA platform.
 */
class MainThreadStatusBar : MainThreadStatusBarShape {
    private val logger = Logger.getInstance(MainThreadStatusBar::class.java)
    override fun setEntry(
        entryId: String,
        id: String,
        extensionId: String?,
        name: String,
        text: String,
        tooltip: Any?,
        hasTooltipProvider: Boolean,
        command: Map<Any, Any>?,
        color: Any?,
        backgroundColor: Any?,
        alignLeft: Boolean,
        priority: Number?,
        accessibilityInformation: Any?
    ) {
        logger.info("Set entry $entryId")
    }

    override fun dispose() {
        logger.info("Disposing main thread")
    }

} 