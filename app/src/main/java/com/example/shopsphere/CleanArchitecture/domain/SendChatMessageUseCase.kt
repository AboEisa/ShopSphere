package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.BuildConfig
import com.example.shopsphere.CleanArchitecture.data.FaqMatcher
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.network.Content
import com.example.shopsphere.CleanArchitecture.data.network.GeminiApiService
import com.example.shopsphere.CleanArchitecture.data.network.GeminiRequest
import com.example.shopsphere.CleanArchitecture.data.network.GenerationConfig
import com.example.shopsphere.CleanArchitecture.data.network.Part
import com.example.shopsphere.CleanArchitecture.ui.models.ChatAction
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.utils.Constant
import javax.inject.Inject

/**
 * Hybrid chatbot use-case:
 *   1. Try rule-based FAQ first (instant, offline, no quota cost).
 *   2. Fall back to Gemini 2.0 Flash for anything else, passing a rich
 *      system prompt that includes the user's profile + recent orders +
 *      cart summary + on-demand product matches so replies feel personalised
 *      and can answer "is X available?" / "where is order #1234?" specifically.
 */
class SendChatMessageUseCase @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val faqMatcher: FaqMatcher,
    private val sharedPreference: SharedPreference,
    private val repository: IRepository
) {

    data class CartLine(
        val name: String,
        val quantity: Int,
        val price: String
    )

    data class ProductMatch(
        val productId: Int,
        val title: String,
        val price: String,
        val stock: Int,
        val category: String
    )

    /**
     * Reply payload returned to the ViewModel — the visible text plus any
     * deep-link shortcuts the use case generated for this turn.
     */
    data class ChatReply(
        val text: String,
        val actions: List<ChatAction> = emptyList()
    )

    /**
     * Result of the recommendation step. [source] tells the prompt how to
     * phrase the suggestion — "paired with your cart" vs "popular right now".
     */
    data class RecommendationSet(
        val items: List<ProductMatch>,
        val source: Source
    ) {
        enum class Source { CART_PAIRED, POPULAR, NONE }

        companion object {
            val empty = RecommendationSet(emptyList(), Source.NONE)
        }
    }

    data class ChatContext(
        val recentOrders: List<OrderHistoryItem> = emptyList(),
        val cartItemCount: Int = 0,
        val cartTotal: String = "EGP 0.00",
        val cartLines: List<CartLine> = emptyList()
    )

    /**
     * @param history prior [role -> text] pairs, oldest first. role is either
     *                "user" or "model".
     */
    suspend fun send(
        message: String,
        history: List<Pair<String, String>>,
        context: ChatContext
    ): Result<ChatReply> {
        val userName = sharedPreference.getProfileName().trim().takeIf { it.isNotBlank() }
        android.util.Log.d("ChatBot", "send() called | message='$message' | historySize=${history.size}")

        // 1) Fast path — FAQ rule matched.
        val isFirstUserMessage = history.none { it.first == "user" }
        val faqMatch = faqMatcher.match(
            message = message,
            userName = userName,
            isFirstMessage = isFirstUserMessage,
            hasOrderContext = context.recentOrders.isNotEmpty()
        )
        if (faqMatch != null) {
            android.util.Log.d("ChatBot", "FAQ matched → '${faqMatch.answer.take(80)}'")
            return Result.success(ChatReply(text = faqMatch.answer))
        }
        android.util.Log.d("ChatBot", "No FAQ match → going to Gemini")

        // 2) Gemini fallback.
        val apiKey = BuildConfig.GEMINI_API_KEY
        android.util.Log.d("ChatBot", "API key blank=${apiKey.isBlank()} length=${apiKey.length}")
        if (apiKey.isBlank()) {
            android.util.Log.e("ChatBot", "GEMINI_API_KEY is not configured!")
            return Result.failure(IllegalStateException("Gemini API key not configured"))
        }

        val productMatches = runCatching { lookupProductsFor(message) }.getOrDefault(emptyList())
        android.util.Log.d("ChatBot", "productMatches=${productMatches.size}")

        val orderFocus = pickFocusOrder(message, context.recentOrders)
        val recommendations = if (looksLikeRecommendationRequest(message)) {
            runCatching { buildRecommendations(context.cartLines) }.getOrDefault(RecommendationSet.empty)
        } else RecommendationSet.empty
        val cancelIntent = looksLikeCancelRequest(message)

        android.util.Log.d("ChatBot", "Sending to Gemini | model=${Constant.GEMINI_MODEL} | historyTurns=${history.size}")

        return runCatching {
            val systemInstruction = Content(
                role = null,
                parts = listOf(Part(buildSystemPrompt(
                    userName = userName,
                    context = context,
                    productMatches = productMatches,
                    focusOrder = orderFocus,
                    recommendations = recommendations,
                    cancelIntent = cancelIntent
                )))
            )

            val conversation = history.map { (role, text) ->
                Content(role = role, parts = listOf(Part(text)))
            } + Content(role = "user", parts = listOf(Part(message)))

            val request = GeminiRequest(
                contents = conversation,
                systemInstruction = systemInstruction,
                generationConfig = GenerationConfig(
                    temperature = 0.55f,
                    topP = 0.9f,
                    maxOutputTokens = 360
                )
            )

            android.util.Log.d("ChatBot", "Calling generateContent...")
            val response = geminiApiService.generateContent(
                model = Constant.GEMINI_MODEL,
                apiKey = apiKey,
                request = request
            )
            android.util.Log.d("ChatBot", "Gemini response received | candidates=${response.candidates?.size} | blockReason=${response.promptFeedback?.blockReason}")

            val firstCandidate = response.candidates?.firstOrNull()
            val raw = firstCandidate?.content?.parts?.joinToString("") { it.text }?.trim()
            android.util.Log.d("ChatBot", "Raw reply (first 120): ${raw?.take(120)}")

            val text = when {
                !raw.isNullOrBlank() -> sanitizeReply(raw)
                response.promptFeedback?.blockReason != null ->
                    "I can't help with that one — want to ask about your orders, returns, or a deal instead?"
                else -> "I didn't quite get that. Could you rephrase, or ask about orders, returns, payments, or deals?"
            }

            val actions = buildActionsFor(
                replyText = text,
                orders = context.recentOrders,
                productPool = productMatches + recommendations.items
            )
            android.util.Log.d("ChatBot", "Reply ready | actions=${actions.size}")
            ChatReply(text = text, actions = actions)
        }
    }

    /**
     * Inspect the bot reply for references the user can tap into:
     *   - Order numbers — match against [orders] so we never produce a chip
     *     for a number that's not actually an order ID.
     *   - Product titles — match against [productPool] (search hits +
     *     recommendation items) so the chip carries a real productId.
     * Capped at [MAX_ACTIONS] to avoid a forest of buttons under one reply.
     */
    private fun buildActionsFor(
        replyText: String,
        orders: List<OrderHistoryItem>,
        productPool: List<ProductMatch>
    ): List<ChatAction> {
        if (replyText.isBlank()) return emptyList()
        val lowerReply = replyText.lowercase()
        val actions = mutableListOf<ChatAction>()
        val seenOrderIds = HashSet<String>()
        val seenProductIds = HashSet<Int>()

        // Pull all digit groups that look like order references and match them
        // against the actual order list.
        Regex("#?(\\d{2,})").findAll(replyText).forEach { m ->
            val digits = m.groupValues[1]
            val match = orders.firstOrNull { it.orderId == digits || it.orderId.endsWith(digits) }
                ?: return@forEach
            if (!seenOrderIds.add(match.orderId)) return@forEach
            actions += ChatAction.ViewOrder(
                orderId = match.orderId,
                label = "View order #${match.orderId}"
            )
        }

        // Match product titles by checking whether the reply mentions either
        // the full title or its longest meaningful word. The longest word is a
        // good proxy when the model paraphrases ("the leather backpack").
        for (p in productPool) {
            if (actions.size >= MAX_ACTIONS) break
            if (p.productId in seenProductIds) continue
            val titleLower = p.title.lowercase()
            val mentioned = lowerReply.contains(titleLower) ||
                    run {
                        val keyword = p.title
                            .split(' ', '-', '/', ',')
                            .map { it.trim() }
                            .filter { it.length >= 5 }
                            .maxByOrNull { it.length }
                            ?.lowercase()
                        keyword != null && lowerReply.contains(keyword)
                    }
            if (!mentioned) continue
            seenProductIds += p.productId
            actions += ChatAction.OpenProduct(
                productId = p.productId,
                label = "Open ${p.title.take(28).trim()}"
            )
        }

        return actions.take(MAX_ACTIONS)
    }

    /**
     * Detect product / availability intent in the message and search the
     * catalog so Gemini can answer with real titles, prices and stock counts.
     * Returns at most [MAX_PRODUCT_MATCHES] results, or empty when no intent
     * was detected or the search returned nothing.
     */
    private suspend fun lookupProductsFor(message: String): List<ProductMatch> {
        val normalized = message.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()

        val hasIntent = AVAILABILITY_KEYWORDS.any { kw ->
            if (kw.contains(' ')) normalized.contains(kw) else
                Regex("(?<![\\p{L}])${Regex.escape(kw)}(?![\\p{L}])").containsMatchIn(normalized)
        }
        if (!hasIntent) return emptyList()

        // Strip the trigger words so the query is the actual subject ("is the
        // red shirt in stock?" -> "red shirt").
        var query = normalized
        AVAILABILITY_KEYWORDS.forEach { kw -> query = query.replace(kw, " ") }
        STOPWORDS.forEach { kw ->
            query = query.replace(Regex("(?<![\\p{L}])${Regex.escape(kw)}(?![\\p{L}])"), " ")
        }
        query = query.replace(Regex("[?.!,]+"), " ").trim()
        if (query.length < 2) return emptyList()

        return runCatching {
            repository.searchProducts(query).getOrNull().orEmpty()
                .take(MAX_PRODUCT_MATCHES)
                .map { p ->
                    ProductMatch(
                        productId = p.id,
                        title = p.title,
                        price = "EGP %.2f".format(p.price),
                        stock = p.stock,
                        category = p.category
                    )
                }
        }.getOrDefault(emptyList())
    }

    /**
     * True when the message is asking for product ideas / suggestions.
     * Uses the same keyword set as the FAQ rule — kept in sync intentionally.
     */
    private fun looksLikeRecommendationRequest(message: String): Boolean {
        val n = message.trim().lowercase()
        return RECOMMEND_KEYWORDS.any { kw ->
            if (kw.contains(' ')) n.contains(kw) else
                Regex("(?<![\\p{L}])${Regex.escape(kw)}(?![\\p{L}])").containsMatchIn(n)
        }
    }

    /** True when the message is asking to cancel an order. */
    private fun looksLikeCancelRequest(message: String): Boolean {
        val n = message.trim().lowercase()
        return CANCEL_KEYWORDS.any { kw ->
            if (kw.contains(' ')) n.contains(kw) else
                Regex("(?<![\\p{L}])${Regex.escape(kw)}(?![\\p{L}])").containsMatchIn(n)
        }
    }

    /**
     * Resolve recommendations for the user's request:
     *   1. Try cart-paired suggestions (search by the cart's product titles).
     *   2. If that returns nothing — empty cart, all matches were already in
     *      the cart, or search misses — fall back to popular in-stock items
     *      from the full catalog so we always have something to suggest.
     */
    private suspend fun buildRecommendations(cartLines: List<CartLine>): RecommendationSet {
        val paired = recommendFromCart(cartLines)
        if (paired.isNotEmpty()) {
            return RecommendationSet(paired, RecommendationSet.Source.CART_PAIRED)
        }
        val popular = recommendPopular(cartLines)
        return if (popular.isNotEmpty()) {
            RecommendationSet(popular, RecommendationSet.Source.POPULAR)
        } else {
            RecommendationSet.empty
        }
    }

    /**
     * Pick a few catalog items that pair with what's already in the cart.
     * Strategy: for each cart line, search the catalog using the most
     * meaningful word from its title, then de-dupe against cart items and
     * cap to [MAX_RECOMMENDATIONS]. Empty cart → empty result (caller falls
     * back to a popularity pivot).
     */
    private suspend fun recommendFromCart(cartLines: List<CartLine>): List<ProductMatch> {
        if (cartLines.isEmpty()) return emptyList()

        val cartTitles = cartLines.map { it.name.lowercase() }.toHashSet()
        val pool = mutableListOf<ProductMatch>()
        val seen = HashSet<String>(cartTitles)

        for (line in cartLines.take(3)) {
            val seed = line.name
                .split(' ', '-', '/', ',')
                .map { it.trim() }
                .filter { it.length >= 4 }
                .maxByOrNull { it.length }
                ?: continue

            val results = runCatching { repository.searchProducts(seed).getOrNull().orEmpty() }
                .getOrDefault(emptyList())

            for (p in results) {
                val key = p.title.lowercase()
                if (key in seen) continue
                seen += key
                if (p.stock <= 0) continue
                pool += ProductMatch(
                    productId = p.id,
                    title = p.title,
                    price = "EGP %.2f".format(p.price),
                    stock = p.stock,
                    category = p.category
                )
                if (pool.size >= MAX_RECOMMENDATIONS) break
            }
            if (pool.size >= MAX_RECOMMENDATIONS) break
        }
        return pool
    }

    /**
     * Popularity pivot — when the cart pairing strategy returns nothing.
     * Pulls the full catalog, drops anything already in the cart and
     * out-of-stock items, then picks the highest-stock survivors as a
     * proxy for "popular / well-stocked" without a real signal from the
     * backend. Caps to [MAX_RECOMMENDATIONS].
     */
    private suspend fun recommendPopular(cartLines: List<CartLine>): List<ProductMatch> {
        val skip = cartLines.map { it.name.lowercase() }.toHashSet()
        val all = runCatching { repository.getProducts().getOrNull().orEmpty() }
            .getOrDefault(emptyList())
        if (all.isEmpty()) return emptyList()

        return all
            .asSequence()
            .filter { it.stock > 0 }
            .filter { it.title.lowercase() !in skip }
            .sortedByDescending { it.stock }
            .take(MAX_RECOMMENDATIONS)
            .map { p ->
                ProductMatch(
                    productId = p.id,
                    title = p.title,
                    price = "EGP %.2f".format(p.price),
                    stock = p.stock,
                    category = p.category
                )
            }
            .toList()
    }

    /**
     * If the message references a specific order number, return that order so
     * the system prompt can spotlight it. Falls back to null when nothing
     * matched — Gemini will then use the recent-orders list as-is.
     */
    private fun pickFocusOrder(
        message: String,
        orders: List<OrderHistoryItem>
    ): OrderHistoryItem? {
        if (orders.isEmpty()) return null
        val digits = Regex("#?(\\d{2,})").findAll(message)
            .map { it.groupValues[1] }
            .toList()
        if (digits.isEmpty()) return null
        return orders.firstOrNull { o -> digits.any { d -> o.orderId.contains(d) } }
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

        // Words/phrases that suggest the user is asking about a product,
        // availability or stock. Both English and a few common Arabic forms.
        private val AVAILABILITY_KEYWORDS = listOf(
            "available", "availability", "in stock", "out of stock", "stock",
            "do you have", "have any", "have you got", "do you sell",
            "is there", "any ", "looking for", "find me", "search for",
            "متوفر", "متوفرة", "موجود", "موجودة", "عندكم", "عندكو", "ابحث عن", "ابحث"
        )

        // Common filler/stopwords stripped when building the search query.
        private val STOPWORDS = listOf(
            "is", "the", "a", "an", "are", "any", "for", "me", "this", "that",
            "you", "got", "still", "left", "now", "today", "please",
            "هل", "في", "على", "عن", "من", "ال"
        )

        private const val MAX_PRODUCT_MATCHES = 4
        private const val MAX_RECOMMENDATIONS = 4
        // Don't drown the bubble in chips. 3 deep-links is plenty.
        private const val MAX_ACTIONS = 3

        // Product / suggestion intent — kept in sync with the FaqMatcher rule.
        private val RECOMMEND_KEYWORDS = listOf(
            "recommend", "recommendation", "recommendations",
            "suggest", "suggestion", "suggestions",
            "what should i buy", "what to buy", "ideas", "ideas for",
            "اقترح", "اقتراح", "اقتراحات", "نصحني", "ايه اللي اشتريه"
        )

        // Cancel-order intent.
        private val CANCEL_KEYWORDS = listOf(
            "cancel", "cancellation", "stop the order", "stop my order",
            "الغاء", "إلغاء", "ألغي", "الغي", "اوقف الاوردر"
        )
    }

    private fun buildSystemPrompt(
        userName: String?,
        context: ChatContext,
        productMatches: List<ProductMatch>,
        focusOrder: OrderHistoryItem?,
        recommendations: RecommendationSet,
        cancelIntent: Boolean
    ): String = buildString {
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
        appendLine("=== HOW TO HANDLE COMMON INTENTS ===")
        appendLine("- Order status / tracking: cite the exact order number, current status and (if known) the driver. ")
        appendLine("  If multiple orders exist, lead with the most recent one and offer to look up another by number.")
        appendLine("- Availability / \"do you have X?\": use the PRODUCT MATCHES block below. Quote the title, price ")
        appendLine("  and whether it's in stock (stock > 0). If MATCHES is empty, say you couldn't find that item and ")
        appendLine("  invite the user to try a different keyword or browse the home feed.")
        appendLine("- Cart questions (\"what's in my cart?\"): use the CART block — list items with quantity and price.")
        appendLine("- Cancel an order: the app has NO direct cancel button. If they have a relevant order, name it and tell ")
        appendLine("  them to tap Account → Contact Support to stop it before it ships; once shipped it has to go through ")
        appendLine("  Returns. Never claim you cancelled it. If they have no orders, say there's nothing to cancel.")
        appendLine("- Recommendations / \"what should I buy?\": use the RECOMMENDATIONS block when present. The header tells")
        appendLine("  you whether they're cart-paired or popular picks — phrase the suggestion accordingly. If the block is")
        appendLine("  missing entirely (rare — only if the catalog couldn't load), ask what category they're into ")
        appendLine("  (fashion, electronics, jewelry, etc.) instead of guessing.")
        appendLine("- Returns / refunds: explain the 14-day window and point to My Orders → select order → Return.")
        appendLine("- Anything you don't have data for: say so plainly and tell the user which screen to check.")
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

        if (focusOrder != null) {
            appendLine("=== FOCUS ORDER (user referenced this one explicitly) ===")
            val driver = focusOrder.driverName?.takeIf { it.isNotBlank() }?.let { " • driver: $it" }.orEmpty()
            val pay = focusOrder.paymentStatus?.takeIf { it.isNotBlank() }?.let { " • payment: $it" }.orEmpty()
            appendLine("- #${focusOrder.orderId} • status: ${focusOrder.status} • total: ${focusOrder.total} • placed: ${focusOrder.date}$driver$pay")
            if (focusOrder.products.isNotEmpty()) {
                focusOrder.products.take(4).forEach { p ->
                    appendLine("    · ${p.productName} ×${p.quantity}")
                }
            }
            appendLine()
        }

        if (context.recentOrders.isNotEmpty()) {
            appendLine("=== RECENT ORDERS (last ${context.recentOrders.size}) ===")
            context.recentOrders.take(5).forEach { o ->
                val driver = o.driverName?.takeIf { it.isNotBlank() }?.let { " • driver: $it" }.orEmpty()
                val pay = o.paymentStatus?.takeIf { it.isNotBlank() }?.let { " • payment: $it" }.orEmpty()
                appendLine("- #${o.orderId} • status: ${o.status} • total: ${o.total} • placed: ${o.date}$driver$pay")
            }
            appendLine()
        } else {
            appendLine("=== RECENT ORDERS ===")
            appendLine("None yet — the user has no past or current orders on record.")
            appendLine("If they ask to track / status / where their order is: tell them they haven't placed any orders yet")
            appendLine("(don't invent one, don't tell them to open My Orders), then offer to help them find something to buy")
            appendLine("or browse deals on Home. Same goes if they ask to cancel or return — there's nothing to act on yet.")
            appendLine()
        }

        appendLine("=== CART ===")
        appendLine("Items in cart: ${context.cartItemCount}")
        if (context.cartTotal.isNotBlank()) {
            appendLine("Cart total: ${context.cartTotal}")
        }
        if (context.cartLines.isNotEmpty()) {
            context.cartLines.take(5).forEach { line ->
                appendLine("- ${line.name} ×${line.quantity} @ ${line.price}")
            }
        }
        appendLine()

        if (productMatches.isNotEmpty()) {
            appendLine("=== PRODUCT MATCHES (catalog hits for the current query) ===")
            productMatches.forEach { p ->
                val avail = if (p.stock > 0) "in stock (${p.stock} left)" else "out of stock"
                appendLine("- ${p.title} • ${p.price} • ${p.category} • $avail")
            }
            appendLine()
        }

        if (recommendations.items.isNotEmpty()) {
            val header = when (recommendations.source) {
                RecommendationSet.Source.CART_PAIRED ->
                    "=== RECOMMENDATIONS (paired with current cart contents) ==="
                RecommendationSet.Source.POPULAR ->
                    "=== RECOMMENDATIONS (popular / well-stocked picks — cart was empty or had no related items) ==="
                RecommendationSet.Source.NONE ->
                    "=== RECOMMENDATIONS ==="
            }
            appendLine(header)
            recommendations.items.forEach { p ->
                appendLine("- ${p.title} • ${p.price} • ${p.category}")
            }
            val phrasing = when (recommendations.source) {
                RecommendationSet.Source.CART_PAIRED ->
                    "(Pick 2–3 of these to suggest, framing them as pairs for what's in the cart. Mention price; don't list every one.)"
                RecommendationSet.Source.POPULAR ->
                    "(Pick 2–3 of these and frame them as popular picks or fresh ideas — NOT as paired with the cart, since the cart pairing didn't yield anything. Mention price.)"
                RecommendationSet.Source.NONE -> ""
            }
            if (phrasing.isNotEmpty()) appendLine(phrasing)
            appendLine()
        }

        if (cancelIntent) {
            appendLine("=== CANCEL INTENT DETECTED ===")
            appendLine("The user wants to cancel an order. The app has no in-app cancel action.")
            if (focusOrder != null) {
                appendLine("They referenced #${focusOrder.orderId} (status: ${focusOrder.status}).")
            } else if (context.recentOrders.isNotEmpty()) {
                appendLine("They didn't name an order — ask which one (list 1–2 recent ones with status).")
            } else {
                appendLine("They have no orders on record — tell them there's nothing to cancel.")
            }
            appendLine("Route them to Account → Contact Support if cancellation is still possible.")
            appendLine()
        }

        appendLine("=== HARD RULES ===")
        appendLine("1. Never invent order numbers, prices, statuses, products or policies that aren't listed above.")
        appendLine("2. If you don't have data the user is asking for, say so plainly and tell them which screen to check.")
        appendLine("3. Don't promise actions you can't perform (you can't actually cancel orders or issue refunds — point to the right screen).")
        appendLine("4. If the user asks for something off-topic (jokes, world news, code), redirect to shopping help in one short line.")
        appendLine("5. Quote products and orders verbatim from the data blocks — don't paraphrase the title or change the order #.")
    }
}