package com.example.shopsphere.CleanArchitecture.data.network

/**
 * Request / response DTOs for Google Generative Language API (Gemini 2.0 Flash).
 * Endpoint: POST /v1beta/models/gemini-2.0-flash-exp:generateContent?key=<API_KEY>
 */
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val role: String? = null,   // "user" or "model"
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val maxOutputTokens: Int = 1024
)

data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val promptFeedback: PromptFeedback? = null
)

data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

data class PromptFeedback(
    val blockReason: String? = null
)
