package com.sina.weibo.agent.extensions.common

/**
 * Extension type enum for supported AI code assistants
 * Defines different types of extensions that can be supported
 */
enum class ExtensionType(val code: String, val displayName: String, val description: String) {
    CODEX("codex", "OpenAI Codex", "OpenAI Codex AI-powered code assistant"),
    ;

    companion object {
        /**
         * Get extension type by code
         * @param code Extension code
         * @return Extension type or null if not found
         */
        fun fromCode(code: String): ExtensionType? {
            return values().find { it.code == code }
        }

        /**
         * Get default extension type
         * @return Default extension type
         */
        fun getDefault(): ExtensionType {
            return CODEX
        }

        /**
         * Get all supported extension types
         * @return List of all extension types
         */
        fun getAllTypes(): List<ExtensionType> {
            return values().toList()
        }
    }
}
