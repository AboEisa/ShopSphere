package com.example.shopsphere.CleanArchitecture.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule-based FAQ matcher. If the user's message hits one of the canned keywords
 * we short-circuit the Gemini call and reply instantly — saving latency & quota.
 *
 * Matching is whole-word, case-insensitive, and supports both English and a
 * handful of Arabic keywords for RTL users.
 */
@Singleton
class FaqMatcher @Inject constructor() {

    data class Match(val answer: String, val topic: String)

    /**
     * @param userName optional — used to personalize the greeting response.
     * @param isFirstMessage true when this is the very first user message in the
     *        session. Greetings only fire then so saying "hi" mid-conversation
     *        falls through to Gemini for a real reply.
     * @return a canned answer when a rule matches, otherwise null (caller falls
     *         back to Gemini).
     */
    fun match(
        message: String,
        userName: String? = null,
        isFirstMessage: Boolean = false
    ): Match? {
        val normalized = message.trim().lowercase()
        if (normalized.isEmpty()) return null

        // Tokenize on whitespace + punctuation so "hi" matches "hi" but not "this".
        // Arabic words don't contain ASCII punctuation so they survive intact.
        val tokens = normalized.split(WORD_SPLIT).filter { it.isNotBlank() }
        val tokenSet = tokens.toHashSet()

        rules.forEach { rule ->
            if (rule.topic == "greeting" && !isFirstMessage) return@forEach

            val matched = rule.keywords.any { kw ->
                if (kw.contains(' ')) normalized.contains(kw)        // multi-word phrases
                else tokenSet.contains(kw)                            // single word — exact match
            }
            if (matched) {
                val answer = rule.buildAnswer(userName)
                return Match(answer = answer, topic = rule.topic)
            }
        }
        return null
    }

    private data class Rule(
        val topic: String,
        val keywords: List<String>,
        val buildAnswer: (String?) -> String
    )

    private val rules: List<Rule> = listOf(
        Rule(
            topic = "greeting",
            keywords = listOf(
                "hi", "hello", "hey", "yo", "hola",
                "مرحبا", "السلام", "اهلا", "أهلا", "هاي", "هلا"
            ),
            buildAnswer = { name ->
                val who = name?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
                "Hey$who! I'm Chatbot, your shopping assistant. I can help with orders, returns, deals, and anything else — what do you need today?"
            }
        ),
        Rule(
            topic = "thanks",
            keywords = listOf(
                "thanks", "thank", "thx", "ty", "appreciate",
                "شكرا", "شكراً", "تسلم", "ميرسي"
            ),
            buildAnswer = {
                "Anytime! Anything else I can help with — orders, deals, account stuff?"
            }
        ),
        Rule(
            topic = "goodbye",
            keywords = listOf(
                "bye", "goodbye", "cya", "see ya",
                "مع السلامة", "سلام", "وداعا", "وداعاً"
            ),
            buildAnswer = {
                "Take care! Tap the chat anytime you need me — happy shopping. 🛍️"
            }
        ),
        Rule(
            topic = "who_are_you",
            keywords = listOf(
                "who are you", "what are you", "your name",
                "مين انت", "من انت", "اسمك ايه", "ما اسمك"
            ),
            buildAnswer = { name ->
                val who = name?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
                "I'm Chatbot$who — the ShopSphere shopping assistant. I can track orders, explain returns, find deals, and walk you through checkout."
            }
        ),
        Rule(
            topic = "capabilities",
            keywords = listOf(
                "what can you do", "help me with", "how can you help",
                "تقدر تعمل", "تقدر تساعد", "ايه اللي تعرف تعمله"
            ),
            buildAnswer = {
                "I can help with: tracking orders, returns & refunds, deals & promo codes, payment methods, and account questions. What's on your mind?"
            }
        ),
        Rule(
            topic = "returns",
            keywords = listOf(
                "return", "returns", "refund", "exchange",
                "ارجاع", "إرجاع", "استرجاع", "استرداد"
            ),
            buildAnswer = {
                "Returns are open for 14 days from delivery. Open My Orders → pick the order → tap Return. Refunds land on the original payment method within 5–7 days."
            }
        ),
        Rule(
            topic = "shipping",
            keywords = listOf(
                "shipping", "delivery time", "how long", "when will it arrive", "dispatch",
                "شحن", "توصيل", "متى يوصل", "هتوصل امتى"
            ),
            buildAnswer = {
                "Standard shipping is 3–5 business days. Free on orders over \$50. You'll get a live courier map inside My Orders the moment it ships."
            }
        ),
        Rule(
            topic = "contact",
            keywords = listOf(
                "contact", "support", "help center", "human", "agent",
                "خدمة العملاء", "تواصل", "دعم"
            ),
            buildAnswer = {
                "I'm here 24/7. For a real human, tap Account → Contact Support, or say 'talk to human' and I'll point you there."
            }
        ),
        Rule(
            topic = "track",
            keywords = listOf(
                "track", "tracking", "where is my order", "where's my order", "my order status",
                "تتبع", "فين اوردري", "حالة الطلب"
            ),
            buildAnswer = {
                "Open My Orders and tap any order — you'll see the live courier location, the driver's name, and the exact delivery step. Want me to summarize your latest one?"
            }
        ),
        Rule(
            topic = "payment",
            keywords = listOf(
                "payment", "pay", "credit card", "debit card", "cash on delivery", "cod",
                "دفع", "بطاقة", "كاش"
            ),
            buildAnswer = {
                "We accept Visa, Mastercard, Apple Pay, and Cash on Delivery. Save a card under Account → Payment Methods so checkout is one tap."
            }
        ),
        Rule(
            topic = "discount",
            keywords = listOf(
                "discount", "coupon", "promo", "deal", "deals", "sale", "offer", "promo code",
                "خصم", "كوبون", "عرض", "عروض"
            ),
            buildAnswer = {
                "Check the Deals section on Home for live offers. Try the code SAVE10 at checkout — and turn on notifications so you don't miss flash sales."
            }
        ),
        Rule(
            topic = "language",
            keywords = listOf("change language", "arabic", "english", "غير اللغة", "اللغة"),
            buildAnswer = {
                "You can switch language anytime: Account → Language. I'll keep replying in whichever you pick — English or العربية."
            }
        )
    )

    private companion object {
        // Split on whitespace + common punctuation. Keeps Arabic letters intact.
        private val WORD_SPLIT = Regex("[\\s.,!?;:'\"()\\[\\]{}\\-—/]+")
    }
}
