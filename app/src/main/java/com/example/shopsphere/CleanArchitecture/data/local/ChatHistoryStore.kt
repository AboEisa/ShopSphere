package com.example.shopsphere.CleanArchitecture.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.shopsphere.CleanArchitecture.ui.models.ChatAction
import com.example.shopsphere.CleanArchitecture.ui.models.ChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the chatbot conversation across app restarts so the user can pick
 * up the same thread instead of starting from scratch every launch.
 *
 * Backed by a small Gson-encoded blob in SharedPreferences. Typing indicators
 * and error bubbles are dropped on save — they don't belong in history. We
 * also cap at [MAX_PERSISTED_MESSAGES] so the blob can't grow unbounded.
 */
@Singleton
class ChatHistoryStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    fun load(): List<ChatMessage> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<PersistedMessage>>() {}.type
            gson.fromJson<List<PersistedMessage>>(json, type).orEmpty().mapNotNull { it.toChatMessage() }
        }.getOrDefault(emptyList())
    }

    fun save(messages: List<ChatMessage>) {
        val persistable = messages
            .mapNotNull {
                when (it) {
                    is ChatMessage.UserMessage -> PersistedMessage(
                        role = ROLE_USER,
                        text = it.text,
                        timestamp = it.timestamp
                    )
                    is ChatMessage.BotMessage -> if (it.isError) null else PersistedMessage(
                        role = ROLE_BOT,
                        text = it.text,
                        timestamp = it.timestamp
                    )
                    is ChatMessage.TypingIndicator -> null
                }
            }
            .takeLast(MAX_PERSISTED_MESSAGES)

        if (persistable.isEmpty()) {
            prefs.edit().remove(KEY).apply()
        } else {
            prefs.edit().putString(KEY, gson.toJson(persistable)).apply()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private data class PersistedMessage(
        val role: String,
        val text: String,
        val timestamp: Long
    ) {
        fun toChatMessage(): ChatMessage? = when (role) {
            ROLE_USER -> ChatMessage.UserMessage(text = text, timestamp = timestamp)
            ROLE_BOT -> ChatMessage.BotMessage(text = text, timestamp = timestamp)
            else -> null
        }
    }

    private companion object {
        const val PREF_NAME = "CHAT_HISTORY_PREF"
        const val KEY = "chat_history_messages"
        // Roughly 25 user/bot exchanges. Old turns scroll off the top instead
        // of bloating the JSON blob on disk.
        const val MAX_PERSISTED_MESSAGES = 50
        const val ROLE_USER = "user"
        const val ROLE_BOT = "bot"
    }
}
