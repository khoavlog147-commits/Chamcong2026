package com.example.util

import android.content.Context
import android.content.SharedPreferences

object AiApiKeyManager {
    private const val PREF_NAME = "ai_studio_user_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_user_api_key"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_GEMINI_API_KEY, "")?.trim() ?: ""
    }

    fun saveApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun clearApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }
}
