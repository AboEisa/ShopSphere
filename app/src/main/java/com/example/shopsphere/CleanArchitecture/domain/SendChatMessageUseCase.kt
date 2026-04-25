package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.BuildConfig
import com.example.shopsphere.CleanArchitecture.data.FaqMatcher
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.network.Content
import com.example.shopsphere.CleanArchitecture.data.network.GeminiApiService
import com.example.shopsphere.CleanArchitecture.data.network.GeminiRequest
import com.example.shopsphere.CleanArchitecture.data.network.GenerationConfig
import com.example.shopsphere.CleanArchitecture.data.network.Part
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.utils.Constant
import javax.inject.Inject

/**
 * Hybrid chatbot use-case:
 *   1. Try rule-based FAQ first (instant, offline, no quota cost).
 *   2. Fall back to Gemini 2.0 Flash for anything else, passing a rich
 *      system prompt that includes the user's profile + recent orders +
 *      cart summary so replies feel personalised.
 */
class SendChatMessageUseCase @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val faqMatcher: FaqMatcher,
    private val sharedPreference: SharedPreference,
    private val repository: IRepository
) {

    data class ChatContext(
        val recentOrders: List<OrderHistoryItem> = emptyList(),
        val cartItemCount: Int = 0,
        val cartTotal: String = "EGP 0.00"
    )

    /**
     * @param history prior [role -> text] pairs, oldest first. role is either
     *                "user" or "model".
     */
    suspend fun send(
        message: String,
        history: List<Pair<String, String>>,
        context: ChatContext
    ): Result<String> {
        val userName = sharedPreference.getProfileName().trim().takeIf { it.isNotBlank() }

        // 1) Fast path — FAQ rule matched.
        faqMatcher.match(message, userName)?.let { match ->
            return Result.success(match.answer)
        }

        // 2) Gemini fallback.
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key not configured"))
        }

        return runCatching {
            val systemInstruction = Content(
                role = null,
                parts = listOf(Part(buildSystemPrompt(userName, context)))
            )

            val conversation = history.map { (role, text) ->
                Content(role = role, parts = listOf(Part(text)))
            } + Content(role = "user", parts = listOf(Part(message)))

            val request = GeminiRequest(
                contents = conversation,
                systemInstruction = systemInstruction,
                generationConfig = GenerationConfig(
                    temperature = 0.7f,
                    topP = 0.95f,
                    maxOutputTokens = 512
                )
            )

            val response = geminiApiService.generateContent(
                model = Constant.GEMINI_MODEL,
                apiKey = apiKey,
                request = request
            )

            val firstCandidate = response.candidates?.firstOrNull()
            val reply = firstCandidate?.content?.parts?.joinToString("") { it.text }?.trim()

            when {
                !reply.isNullOrBlank() -> reply
                response.promptFeedback?.blockReason != null ->
                    "I can't answer that one — want to try asking a different way?"
                else -> "Hmm, I didn't catch that. Mind rephrasing?"
            }
        }
    }

    private fun buildSystemPrompt(userName: String?, context: ChatContext): String = buildString {
        appendLine("You are YallaShop AI, the friendly shopping assistant for YallaShop.")
        appendLine("Be concise, helpful, and warm. Answer in the user's language (including Arabic).")
        appendLine("Keep replies under 4 sentences unless the user asks for detail.")
        appendLine()
        appendLine("=== SHOPPING POLICIES ===")
        appendLine("- Returns: 14 days from delivery. Users go to My Orders → select order → Return.")
        appendLine("- Shipping: 3–5 business days. Free on orders over \$50.")
        appendLine("- Support: 24/7 via Account → Contact Support.")
        appendLine("- Payments: Visa, Mastercard, Apple Pay, Cash on Delivery.")
        appendLine("- Tracking: live courier location inside My Orders.")
        appendLine()

        if (!userName.isNullOrBlank()) {
            appendLine("=== USER PROFILE ===")
            appendLine("Name: $userName")
            appendLine()
        }

        if (context.recentOrders.isNotEmpty()) {
            appendLine("=== RECENT ORDERS (last ${context.recentOrders.size}) ===")
            context.recentOrders.take(5).forEach { o ->
                appendLine("- #${o.orderId} • ${o.status} • ${o.total} • ${o.date}")
            }
            appendLine()
        }

        appendLine("=== CART ===")
        appendLine("Items in cart: ${context.cartItemCount}")
        if (context.cartTotal.isNotBlank()) {
            appendLine("Cart total: ${context.cartTotal}")
        }
        appendLine()
        appendLine("Never invent order numbers, prices, or policies that aren't listed above.")
    }
}
