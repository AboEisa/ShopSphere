package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.ChatHistoryStore
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.example.shopsphere.CleanArchitecture.domain.SendChatMessageUseCase
import com.example.shopsphere.CleanArchitecture.ui.models.ChatMessage
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.utils.formatEgpPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [ChatBotFragment]. Holds the in-memory conversation, pushes user
 * messages through [SendChatMessageUseCase] (FAQ → Gemini), and exposes a
 * single [UiState] the Fragment renders.
 */
@HiltViewModel
class ChatBotViewModel @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val repository: IRepository,
    private val sharedPreference: SharedPreference,
    private val chatHistoryStore: ChatHistoryStore
) : ViewModel() {

    data class UiState(
        val messages: List<ChatMessage> = emptyList(),
        val isSending: Boolean = false,
        val lastFailedUserMessage: String? = null
    )

    // Restore the previous session's conversation so the user can keep the
    // thread going across app restarts. Typing indicators and error bubbles
    // are dropped at save time, so what we get back here is clean.
    private val _state = MutableStateFlow(UiState(messages = chatHistoryStore.load()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Rolling context the use case passes into Gemini. Refreshed before each send.
    private var recentOrders: List<OrderHistoryItem> = emptyList()

    /** Wipe persisted history and reset the in-memory thread. */
    fun clearHistory() {
        chatHistoryStore.clear()
        _state.value = UiState()
    }

    /**
     * Called by the Fragment once the shared order LiveData emits — keeps the
     * Gemini system prompt's order list up to date.
     */
    fun updateOrderContext(orders: List<OrderHistoryItem>) {
        recentOrders = orders.take(5)
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isSending) return

        val userMsg = ChatMessage.UserMessage(text = trimmed)
        val typing = ChatMessage.TypingIndicator()

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg + typing,
            isSending = true,
            lastFailedUserMessage = null
        )

        viewModelScope.launch {
            val ctx = buildChatContext()
            // Keep the last MAX_HISTORY_TURNS turns of real conversation. Older
            // turns are dropped to keep token usage bounded and the model
            // focused on the current thread.
            val history = _state.value.messages
                .filter { it !is ChatMessage.TypingIndicator && it.id != userMsg.id }
                .mapNotNull { msg ->
                    when (msg) {
                        is ChatMessage.UserMessage -> "user" to msg.text
                        is ChatMessage.BotMessage -> if (!msg.isError) "model" to msg.text else null
                        is ChatMessage.TypingIndicator -> null
                    }
                }
                .takeLast(MAX_HISTORY_TURNS * 2)

            val result = sendChatMessageUseCase.send(trimmed, history, ctx)

            // Remove the typing indicator, then append either the reply or an
            // error bubble the user can retry.
            val withoutTyping = _state.value.messages.filterNot { it is ChatMessage.TypingIndicator }

            result
                .onSuccess { reply ->
                    _state.value = _state.value.copy(
                        messages = withoutTyping + ChatMessage.BotMessage(
                            text = reply.text,
                            actions = reply.actions
                        ),
                        isSending = false,
                        lastFailedUserMessage = null
                    )
                    chatHistoryStore.save(_state.value.messages)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        messages = withoutTyping + ChatMessage.BotMessage(
                            text = "I couldn't reach the server just now — check your connection and tap Retry.",
                            isError = true
                        ),
                        isSending = false,
                        lastFailedUserMessage = trimmed
                    )
                    // Error bubbles are filtered out at save time, but persist
                    // the user message so it isn't lost if the user backgrounds
                    // the app before retrying.
                    chatHistoryStore.save(_state.value.messages)
                }
        }
    }

    private companion object {
        // 12 user/model exchanges = enough to keep the thread coherent without
        // ballooning prompt size.
        private const val MAX_HISTORY_TURNS = 12
    }

    fun retryLast() {
        val failed = _state.value.lastFailedUserMessage ?: return
        // Drop the trailing error bubble before retrying so the conversation
        // doesn't accumulate dead "try again" messages.
        val pruned = _state.value.messages.toMutableList()
        val lastIndex = pruned.indexOfLast { it is ChatMessage.BotMessage && (it as ChatMessage.BotMessage).isError }
        if (lastIndex >= 0) pruned.removeAt(lastIndex)
        _state.value = _state.value.copy(
            messages = pruned,
            lastFailedUserMessage = null
        )
        sendMessage(failed)
    }

    private suspend fun buildChatContext(): SendChatMessageUseCase.ChatContext {
        // Cart summary — safe fallbacks on any failure (never block the chat).
        // We pull the full item list when possible so the prompt can answer
        // "what's in my cart?" with actual product names.
        val itemsResult = runCatching { repository.getCartItems().getOrNull().orEmpty() }
        val items = itemsResult.getOrDefault(emptyList())

        val cartCount = if (items.isNotEmpty()) {
            items.sumOf { it.quantity }
        } else {
            runCatching { repository.getCartItemCount() }.getOrDefault(0)
        }
        val cartTotal = formatEgpPrice(items.sumOf { it.price * it.quantity })
        val cartLines = items.take(5).map {
            SendChatMessageUseCase.CartLine(
                name = it.productName,
                quantity = it.quantity,
                price = formatEgpPrice(it.price)
            )
        }

        return SendChatMessageUseCase.ChatContext(
            recentOrders = recentOrders,
            cartItemCount = cartCount,
            cartTotal = cartTotal,
            cartLines = cartLines
        )
    }
}
