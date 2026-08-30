package com.example.util

import android.content.Context
import android.content.SharedPreferences

object AiApiKeyManager {
    private const val PREF_NAME = "ai_studio_user_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_user_api_key"
    private const val KEY_GEMINI_BACKUP_API_KEY = "gemini_user_backup_api_key"

    private const val KEY_AI_PROVIDER = "ai_provider" // "gemini" or "openrouter"
    private const val KEY_OPENROUTER_API_KEY = "openrouter_user_api_key"
    private const val KEY_OPENROUTER_MODEL = "openrouter_model"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getProvider(context: Context): String {
        return getPrefs(context).getString(KEY_AI_PROVIDER, "gemini") ?: "gemini"
    }

    fun saveProvider(context: Context, provider: String) {
        getPrefs(context).edit().putString(KEY_AI_PROVIDER, provider).apply()
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
        val provider = getProvider(context)
        return if (provider == "openrouter") {
            getOpenRouterKey(context).isNotBlank()
        } else {
            getApiKey(context).isNotBlank() || getBackupApiKey(context).isNotBlank()
        }
    }

    fun getBackupApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_GEMINI_BACKUP_API_KEY, "")?.trim() ?: ""
    }

    fun saveBackupApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_BACKUP_API_KEY, apiKey.trim()).apply()
    }

    fun clearBackupApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_GEMINI_BACKUP_API_KEY).apply()
    }

    fun getOpenRouterKey(context: Context): String {
        return getPrefs(context).getString(KEY_OPENROUTER_API_KEY, "")?.trim() ?: ""
    }

    fun saveOpenRouterKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_OPENROUTER_API_KEY, apiKey.trim()).apply()
    }

    fun clearOpenRouterKey(context: Context) {
        getPrefs(context).edit().remove(KEY_OPENROUTER_API_KEY).apply()
    }

    fun getOpenRouterModel(context: Context): String {
        return getPrefs(context).getString(KEY_OPENROUTER_MODEL, "meta-llama/llama-3.3-70b-instruct:free") ?: "meta-llama/llama-3.3-70b-instruct:free"
    }

    fun saveOpenRouterModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_OPENROUTER_MODEL, model).apply()
    }
}
