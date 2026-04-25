package com.example.shopsphere.CleanArchitecture.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule-based FAQ matcher. If the user's message hits one of the canned keywords
 * we short-circuit the Gemini call and reply instantly — saving latency & quota.
 *
 * Matching is case-insensitive and supports both English and a handful of Arabic
 * keywords for RTL users.
 */
@Singleton
class FaqMatcher @Inject constructor() {

    data class Match(val answer: String, val topic: String)

    /**
     * @param userName optional — used to personalize the greeting response.
     * @return a canned answer when a rule matches, otherwise null (caller falls
     *         back to Gemini).
     */
    fun match(message: String, userName: String? = null): Match? {
        val normalized = message.trim().lowercase()
        if (normalized.isEmpty()) return null

        rules.forEach { rule ->
            if (rule.keywords.any { kw -> normalized.contains(kw) }) {
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
            keywords = listOf("hello", "hi ", "hey", "hii", "helloo", "مرحبا", "السلام", "اهلا", "أهلا"),
            buildAnswer = { name ->
                val who = name?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
                "Hey$who! I'm YallaShop AI, your shopping assistant. I can help with orders, returns, deals, and anything else — what's up?"
            }
        ),
        Rule(
            topic = "returns",
            keywords = listOf("return", "refund", "exchange"),
            buildAnswer = {
                "You can return any product within 14 days of delivery. Go to My Orders → select the order → tap 'Return'."
            }
        ),
        Rule(
            topic = "shipping",
            keywords = listOf("shipping", "delivery time", "how long", "when will it arrive", "dispatch"),
            buildAnswer = {
                "Standard shipping is 3–5 business days. Free on orders over \$50. You'll get live tracking once your order ships."
            }
        ),
        Rule(
            topic = "contact",
            keywords = listOf("contact", "support", "help", "human", "agent"),
            buildAnswer = {
                "I'm here 24/7! For a human, tap Account → Contact Support, or say 'talk to human' and I'll point you there."
            }
        ),
        Rule(
            topic = "track",
            keywords = listOf("track", "where is my order", "where's my order", "live tracking", "my order status"),
            buildAnswer = {
                "Open My Orders and tap any order to see live tracking with real-time courier location."
            }
        ),
        Rule(
            topic = "payment",
            keywords = listOf("payment", " pay ", "pay.", "how do i pay", "credit card", "debit card", "cash on delivery"),
            buildAnswer = {
                "We accept Visa, Mastercard, Apple Pay, and Cash on Delivery — add your card from Account → Payment Methods."
            }
        ),
        Rule(
            topic = "discount",
            keywords = listOf("discount", "coupon", "promo", "deal", "sale", "offer"),
            buildAnswer = {
                "Check the 'Deals' section on Home, or subscribe to notifications for exclusive codes. Try SAVE10 at checkout!"
            }
        )
    )
}
