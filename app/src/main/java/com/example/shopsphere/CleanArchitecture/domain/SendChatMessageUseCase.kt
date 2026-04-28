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

        // 1) Fast path — FAQ rule matched. Greetings only fire on the very first
        //    user turn so saying "hi" mid-conversation routes to Gemini for a
        //    real, contextual reply instead of a canned re-introduction.
        val isFirstUserMessage = history.none { it.first == "user" }
        faqMatcher.match(message, userName, isFirstMessage = isFirstUserMessage)?.let { match ->
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
                    // Slightly lower temperature → more focused, less rambling.
                    temperature = 0.55f,
                    topP = 0.9f,
                    // Cap output so the bot stays conversational instead of
                    // dumping a wall of text.
                    maxOutputTokens = 320
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
                !reply.isNullOrBlank() -> sanitizeReply(reply)
                response.promptFeedback?.blockReason != null ->
                    "I can't help with that one — want to ask about your orders, returns, or a deal instead?"
                else -> "I didn't quite get that. Could you rephrase, or ask about orders, returns, payments, or deals?"
            }
        }
    }

    /**
     * Strip leading "Hi", "Hello!", "Sure!" filler so the bot doesn't restart
     * the introduction on every turn, and trim any markdown headings the model
     * sometimes emits despite the system prompt.
     */
    private fun sanitizeReply(reply: String): String {
        var out = reply.trim()
        // Remove a leading markdown heading or bold line, common Gemini artifact.
        out = out.removePrefix("**").trimStart('#', ' ', '*').trim()
        // Drop opening filler that breaks the conversational flow.
        FILLER_PREFIXES.forEach { prefix ->
            if (out.startsWith(prefix, ignoreCase = true)) {
                out = out.substring(prefix.length).trimStart(' ', ',', '.', '!', '—', '-').trim()
            }
        }
        return out.ifBlank { reply.trim() }
    }

    private companion object {
        private val FILLER_PREFIXES = listOf(
            "Sure thing", "Sure!", "Sure,",
            "Of course!", "Of course,", "Of course",
            "Absolutely!", "Absolutely,",
            "Great question!", "Great question",
            "I'd be happy to help",
        )
    }

    private fun buildSystemPrompt(userName: String?, context: ChatContext): String = buildString {
        appendLine("You are Chatbot, the friendly shopping assistant for ShopSphere.")
        appendLine()
        appendLine("=== CONVERSATION STYLE ===")
        appendLine("- Sound like a sharp human teammate, not a corporate FAQ. Warm, concise, genuinely helpful.")
        appendLine("- Use the user's language. If they write Arabic, reply in Egyptian/MSA Arabic.")
        appendLine("- Default to 1–3 short sentences. Expand only when the user explicitly asks for detail.")
        appendLine("- When the request is vague or could mean two things, ask ONE clarifying question instead of guessing.")
        appendLine("- Stay on the previous topic if the user follows up — don't reset to a generic intro.")
        appendLine("- End with a soft next-step offer when it helps (\"Want me to pull up your latest order?\").")
        appendLine("- Never re-introduce yourself after the first reply. No \"Hi! I'm Chatbot…\" unless the user asks who you are.")
        appendLine("- Don't apologize unnecessarily. Skip filler like \"Great question!\".")
        appendLine("- Avoid markdown headings and code blocks; use plain prose with at most a short bulleted list.")
        appendLine()
        appendLine("=== APP NAVIGATION HINTS ===")
        appendLine("- Returns: 14 days from delivery → My Orders → select order → Return.")
        appendLine("- Tracking: My Orders → tap an order for live courier location + driver name.")
        appendLine("- Shipping: 3–5 business days. Free on orders over \$50.")
        appendLine("- Support: 24/7 via Account → Contact Support.")
        appendLine("- Payments: Visa, Mastercard, Apple Pay, Cash on Delivery (manage in Account → Payment Methods).")
        appendLine("- Language: Account → Language (English / العربية).")
        appendLine()

        if (!userName.isNullOrBlank()) {
            appendLine("=== USER PROFILE ===")
            appendLine("Name: $userName")
            appendLine("(Use the first name occasionally — never every reply, that gets creepy.)")
            appendLine()
        }

        if (context.recentOrders.isNotEmpty()) {
            appendLine("=== RECENT ORDERS (last ${context.recentOrders.size}) ===")
            context.recentOrders.take(5).forEach { o ->
                val driver = o.driverName?.takeIf { it.isNotBlank() }?.let { " • driver: $it" }.orEmpty()
                appendLine("- #${o.orderId} • status: ${o.status} • total: ${o.total} • placed: ${o.date}$driver")
            }
            appendLine()
        } else {
            appendLine("=== RECENT ORDERS ===")
            appendLine("None yet — if asked about an order, suggest browsing the home feed first.")
            appendLine()
        }

        appendLine("=== CART ===")
        appendLine("Items in cart: ${context.cartItemCount}")
        if (context.cartTotal.isNotBlank()) {
            appendLine("Cart total: ${context.cartTotal}")
        }
        appendLine()
        appendLine("=== HARD RULES ===")
        appendLine("1. Never invent order numbers, prices, statuses, or policies that aren't listed above.")
        appendLine("2. If you don't have data the user is asking for, say so plainly and tell them which screen to check.")
        appendLine("3. Don't promise actions you can't perform (you can't actually cancel orders or issue refunds — point to the right screen).")
        appendLine("4. If the user asks for something off-topic (jokes, world news, code), redirect to shopping help in one short line.")
    }
}
