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
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ChatBotViewModel @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val repository: IRepository,
    private val sharedPreference: SharedPreference,
) : ViewModel() {

    data class UiState(
        val messages: List<ChatMessage> = emptyList(),
        val isSending: Boolean = false,
        val lastFailedUserMessage: String? = null
    )

    // Fresh conversation every session — no persistence.
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var recentOrders: List<OrderHistoryItem> = emptyList()

    fun clearHistory() {
        _state.value = UiState()
    }

    fun updateOrderContext(orders: List<OrderHistoryItem>) {
        recentOrders = orders.take(5)
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isSending) return

        val userMsg = ChatMessage.UserMessage(text = trimmed)
        val typing = ChatMessage.TypingIndicator()

        // Capture history from the CURRENT messages BEFORE appending the new user message,
        // so retries don't keep duplicating the user turn in the conversation history.
        val history = _state.value.messages
            .filter { it !is ChatMessage.TypingIndicator }
            .mapNotNull { msg ->
                when (msg) {
                    is ChatMessage.UserMessage -> "user" to msg.text
                    is ChatMessage.BotMessage -> if (!msg.isError) "model" to msg.text else null
                    is ChatMessage.TypingIndicator -> null
                }
            }
            .takeLast(MAX_HISTORY_TURNS * 2)

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg + typing,
            isSending = true,
            lastFailedUserMessage = null
        )

        viewModelScope.launch {
            val ctx = buildChatContext()

            // Cap the entire send at 15 s so the typing indicator never spins forever.
            val result = withTimeoutOrNull(15_000L) {
                runCatching { sendChatMessageUseCase.send(trimmed, history, ctx) }
                    .getOrElse { e ->
                        android.util.Log.e("ChatBot", "send() threw exception", e)
                        Result.failure(e)
                    }
            } ?: run {
                android.util.Log.e("ChatBot", "send() timed out after 15s")
                Result.failure(Exception("Request timed out"))
            }

            val withoutTyping = _state.value.messages.filterNot { it is ChatMessage.TypingIndicator }

            result
                .onSuccess { reply ->
                    android.util.Log.d("ChatBot", "ViewModel got success reply")
                    _state.value = _state.value.copy(
                        messages = withoutTyping + ChatMessage.BotMessage(
                            text = reply.text,
                            actions = reply.actions
                        ),
                        isSending = false,
                        lastFailedUserMessage = null
                    )
                }
                .onFailure { e ->
                    android.util.Log.e("ChatBot", "ViewModel got failure: ${e.message}", e)
                    val errorText = when {
                        e is retrofit2.HttpException && e.code() == 429 ->
                            "I'm handling a lot of requests right now — please wait a moment and tap Retry."
                        e.message?.contains("timed out", ignoreCase = true) == true ->
                            "That took too long to respond. Check your connection and tap Retry."
                        else ->
                            "I couldn't reach the server just now — check your connection and tap Retry."
                    }
                    _state.value = _state.value.copy(
                        messages = withoutTyping + ChatMessage.BotMessage(
                            text = errorText,
                            isError = true
                        ),
                        isSending = false,
                        lastFailedUserMessage = trimmed
                    )
                }
        }
    }

    private companion object {
        private const val MAX_HISTORY_TURNS = 12
    }

    fun retryLast() {
        val failed = _state.value.lastFailedUserMessage ?: return
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
        val items = runCatching {
            withTimeoutOrNull(5_000L) {
                repository.getCartItems().getOrNull().orEmpty()
            } ?: emptyList()
        }.getOrDefault(emptyList())

        val cartCount = items.sumOf { it.quantity }
            .takeIf { it > 0 }
            ?: runCatching { repository.getCartItemCount() }.getOrDefault(0)

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