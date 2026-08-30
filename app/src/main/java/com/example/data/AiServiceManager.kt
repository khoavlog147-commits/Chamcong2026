package com.example.data

import android.content.Context
import com.example.util.AiApiKeyManager

object AiServiceManager {

    suspend fun generateContent(
        context: Context,
        userPrompt: String,
        contextData: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> {
        val provider = AiApiKeyManager.getProvider(context)

        return if (provider == "openrouter") {
            val openRouterKey = AiApiKeyManager.getOpenRouterKey(context)
            val model = AiApiKeyManager.getOpenRouterModel(context)
            if (openRouterKey.isBlank()) {
                return Result.failure(IllegalArgumentException("Chưa cài đặt OpenRouter API Key."))
            }
            OpenRouterAiService.generateContent(
                apiKey = openRouterKey,
                model = model,
                userPrompt = userPrompt,
                contextData = contextData,
                chatHistory = chatHistory
            )
        } else {
            // Default: Gemini with Fallback
            val primaryKey = AiApiKeyManager.getApiKey(context)
            val backupKey = AiApiKeyManager.getBackupApiKey(context)

            val activeKey = if (primaryKey.isNotBlank()) primaryKey else backupKey
            if (activeKey.isBlank()) {
                return Result.failure(IllegalArgumentException("Chưa cài đặt Gemini API Key."))
            }

            GeminiAiService.generateContentWithFallback(
                primaryKey = primaryKey,
                backupKey = backupKey,
                userPrompt = userPrompt,
                contextData = contextData,
                chatHistory = chatHistory
            )
        }
    }
}
