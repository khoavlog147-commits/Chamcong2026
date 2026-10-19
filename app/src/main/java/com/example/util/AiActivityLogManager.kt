package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AiActivityLog(
    val timestamp: Long,
    val actionType: String, // "ATTENDANCE", "SALARY_CONFIG", "TIMESHEET", "ACTION"
    val description: String,
    val userPrompt: String
)

object AiActivityLogManager {
    private const val PREF_NAME = "ai_activity_logs_pref"
    private const val KEY_LOGS = "ai_activity_logs_list"
    private const val MAX_LOGS = 100 // Keep the last 100 entries

    fun addLog(context: Context, actionType: String, description: String, userPrompt: String) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val currentLogs = getLogs(context).toMutableList()
            
            val newLog = AiActivityLog(
                timestamp = System.currentTimeMillis(),
                actionType = actionType,
                description = description,
                userPrompt = userPrompt
            )
            
            // Add to the front of the list (newest first)
            currentLogs.add(0, newLog)
            
            // Trim if exceeds limit
            val trimmedLogs = if (currentLogs.size > MAX_LOGS) {
                currentLogs.take(MAX_LOGS)
            } else {
                currentLogs
            }
            
            // Serialize to JSON
            val jsonArray = JSONArray()
            for (log in trimmedLogs) {
                val jsonObject = JSONObject().apply {
                    put("timestamp", log.timestamp)
                    put("actionType", log.actionType)
                    put("description", log.description)
                    put("userPrompt", log.userPrompt)
                }
                jsonArray.put(jsonObject)
            }
            
            sharedPrefs.edit().putString(KEY_LOGS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("AiActivityLogManager", "Error adding log to preferences", e)
        }
    }

    fun getLogs(context: Context): List<AiActivityLog> {
        val logs = mutableListOf<AiActivityLog>()
        try {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val jsonStr = sharedPrefs.getString(KEY_LOGS, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                logs.add(
                    AiActivityLog(
                        timestamp = jsonObject.getLong("timestamp"),
                        actionType = jsonObject.getString("actionType"),
                        description = jsonObject.getString("description"),
                        userPrompt = jsonObject.optString("userPrompt", "")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AiActivityLogManager", "Error parsing logs from preferences", e)
        }
        return logs
    }

    fun clearLogs(context: Context) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit().remove(KEY_LOGS).apply()
        } catch (e: Exception) {
            android.util.Log.e("AiActivityLogManager", "Error clearing activity logs", e)
        }
    }
}
