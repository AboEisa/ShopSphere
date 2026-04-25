package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val sharedPreference: SharedPreference
) : ViewModel() {

    data class UiState(
        val messages: List<ChatMessage> = emptyList(),
        val isSending: Boolean = false,
        val lastFailedUserMessage: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Rolling context the use case passes into Gemini. Refreshed before each send.
    private var recentOrders: List<OrderHistoryItem> = emptyList()

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
            val history = _state.value.messages
                .filter { it !is ChatMessage.TypingIndicator && it.id != userMsg.id }
                .mapNotNull { msg ->
                    when (msg) {
                        is ChatMessage.UserMessage -> "user" to msg.text
                        is ChatMessage.BotMessage -> if (!msg.isError) "model" to msg.text else null
                        is ChatMessage.TypingIndicator -> null
                    }
                }

            val result = sendChatMessageUseCase.send(trimmed, history, ctx)

            // Remove the typing indicator, then append either the reply or an
            // error bubble the user can retry.
            val withoutTyping = _state.value.messages.filterNot { it is ChatMessage.TypingIndicator }

            result
                .onSuccess { reply ->
                    _state.value = _state.value.copy(
                        messages = withoutTyping + ChatMessage.BotMessage(text = reply),
                        isSending = false,
                        lastFailedUserMessage = null
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        messages = withoutTyping + ChatMessage.BotMessage(
                            text = "Hmm, I'm having trouble connecting. Try again?",
                            isError = true
                        ),
                        isSending = false,
                        lastFailedUserMessage = trimmed
                    )
                }
        }
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
        val (cartCount, cartTotal) = runCatching {
            val items = repository.getCartItems().getOrNull().orEmpty()
            val count = items.sumOf { it.quantity }
            val total = items.sumOf { it.price * it.quantity }
            val formatted = formatEgpPrice(total)
            count to formatted
        }.getOrDefault(
            runCatching { repository.getCartItemCount() }.getOrDefault(0) to formatEgpPrice(0.0)
        )

        return SendChatMessageUseCase.ChatContext(
            recentOrders = recentOrders,
            cartItemCount = cartCount,
            cartTotal = cartTotal
        )
    }
}
