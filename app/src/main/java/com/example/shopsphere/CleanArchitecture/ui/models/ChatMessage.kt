package com.example.shopsphere.CleanArchitecture.ui.models

import java.util.UUID

/**
 * Sealed hierarchy for chat messages rendered in the Sphere AI screen.
 * Each subtype maps to a different RecyclerView view type.
 */
sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    data class UserMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String
    ) : ChatMessage()

    data class BotMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String,
        val isError: Boolean = false
    ) : ChatMessage()

    data class TypingIndicator(
        override val id: String = "typing_indicator",
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()
}
