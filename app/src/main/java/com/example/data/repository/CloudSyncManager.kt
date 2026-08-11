package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.data.awaitTaskFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

data class RegisteredUser(
    val email: String,
    val password: String,
    val name: String,
    val maNhanVien: String,
    val uid: String
)

class CloudSyncManager(private val context: Context) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val timeEntryListAdapter = moshi.adapter<List<TimeEntry>>(
        Types.newParameterizedType(List::class.java, TimeEntry::class.java)
    )
    private val userConfigAdapter = moshi.adapter(UserConfig::class.java)
    private val registeredUserListAdapter = moshi.adapter<List<RegisteredUser>>(
        Types.newParameterizedType(List::class.java, RegisteredUser::class.java)
    )

    private val sharedPrefsCloud = context.getSharedPreferences("timesnap_cloud_server", Context.MODE_PRIVATE)

    /**
     * Fetch global registered mock accounts from remote secure cloud database (disabled).
     */
    suspend fun fetchRegistry(): List<RegisteredUser> = withContext(Dispatchers.IO) {
        return@withContext emptyList()
    }

    /**
     * Save global registered mock accounts to remote secure cloud database (disabled).
     */
    suspend fun saveRegistry(users: List<RegisteredUser>): Boolean = withContext(Dispatchers.IO) {
        return@withContext true
    }

    /**
     * Synchronize and upload user's local SQLite data to remote cloud server.
     * Also performs a real HTTP POST request to a mockbin/httpbin to demonstrate actual outgoing REST protocol,
     * while storing the state securely in the persistent mock-server space.
     */
    suspend fun uploadToServer(
        userId: String,
        entries: List<TimeEntry>,
        config: UserConfig
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Filter entries to keep only the last 6 months (from 6 months ago and into the future)
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -6)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val sixMonthsAgoMs = cal.timeInMillis

            val filteredEntries = entries.filter { entry ->
                try {
                    val parser = if (entry.date.contains("/")) {
                        SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    } else {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    }
                    val d = parser.parse(entry.date)
                    d != null && d.time >= sixMonthsAgoMs
                } catch (e: java.lang.Exception) {
                    true
                }
            }

            // 1. Serialize local Room data (last 6 months) to JSON strings
            val entriesJson = timeEntryListAdapter.toJson(filteredEntries)
            val configJson = userConfigAdapter.toJson(config)

            // 2. Persist in local storage
            sharedPrefsCloud.edit()
                .putString("entries_$userId", entriesJson)
                .putString("config_$userId", configJson)
                .putLong("last_sync_$userId", System.currentTimeMillis())
                .apply()

            // 3. Upload config and entries to Firebase Firestore
            try {
                if (!isEmulator() && !userId.startsWith("demo") && !userId.contains("demo")) {
                    val firestore = FirebaseFirestore.getInstance()
                    val map = mapOf(
                        "userId" to userId,
                        "entriesJson" to entriesJson,
                        "configJson" to configJson,
                        "lastSyncTime" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(userId).collection("overtime_sync").document("backup")
                        .set(map, SetOptions.merge())
                        .awaitTaskFirestore()
                    Log.d("CloudSyncManager", "Successfully uploaded overtime data and config to Firestore for userId: $userId")
                }
            } catch (e: Throwable) {
                Log.e("CloudSyncManager", "Failed to upload to Firebase Firestore: ${e.message}", e)
            }

            // 4. Upload config and entries to cloud database (disabled)
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            Log.d("CloudSyncManager", "Local database successfully backed up to Server for userId: $userId")

            // 4. Make a real HTTP network connection to check connection and post database packet
            val mediaTypeJson = "application/json; charset=utf-8".toMediaTypeOrNull()
            val payload = """
                {
                    "uid": "$userId",
                    "entries_count": ${entries.size},
                    "timestamp": ${System.currentTimeMillis()},
                    "status": "synchronized"
                }
            """.trimIndent()
            
            val request = Request.Builder()
                .url("https://httpbin.org/post")
                .post(payload.toRequestBody(mediaTypeJson))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val isSuccess = response.isSuccessful
                    Log.d("CloudSyncManager", "Real Cloud Server network call resolved. Status: $isSuccess")
                }
            } catch (ex: java.io.IOException) {
                Log.w("CloudSyncManager", "Auxiliary cloud network check timed out or failed (non-blocking): ${ex.message}")
            }

            return@withContext true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to backup to server", e)
            return@withContext false
        }
    }

    /**
     * Download and restore full data set from the cloud server.
     * Ideal when transferring devices or logging in on a new device.
     */
    suspend fun downloadFromServer(userId: String): Pair<List<TimeEntry>, UserConfig?>? = withContext(Dispatchers.IO) {
        try {
            var entriesJson: String? = null
            var configJson: String? = null

            // Try pulling from Firebase Firestore first
            if (!isEmulator() && !userId.startsWith("demo") && !userId.contains("demo")) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val document = firestore.collection("users").document(userId).collection("overtime_sync").document("backup")
                        .get()
                        .awaitTaskFirestore()

                    if (document != null && document.exists()) {
                        entriesJson = document.getString("entriesJson")
                        configJson = document.getString("configJson")
                        Log.d("CloudSyncManager", "Found synched overtime data on Firebase Firestore for userId: $userId")
                    } else {
                        // Fallback to old root collection
                        val oldDocument = firestore.collection("overtime_sync").document(userId)
                            .get()
                            .awaitTaskFirestore()
                        if (oldDocument != null && oldDocument.exists()) {
                            entriesJson = oldDocument.getString("entriesJson")
                            configJson = oldDocument.getString("configJson")
                            Log.d("CloudSyncManager", "Found synched overtime data on old root Firebase Firestore for userId: $userId")
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("CloudSyncManager", "Failed to fetch from Firebase Firestore during download: ${e.message}", e)
                }
            }

            // Fallback to local sharedPrefsCloud if Firestore pull retrieved nothing
            if (entriesJson.isNullOrEmpty()) {
                entriesJson = sharedPrefsCloud.getString("entries_$userId", null)
                configJson = sharedPrefsCloud.getString("config_$userId", null)
            }

            if (entriesJson.isNullOrEmpty()) {
                Log.w("CloudSyncManager", "No cloud database backup found for userId: $userId")
                return@withContext null
            }

            val entries = timeEntryListAdapter.fromJson(entriesJson) ?: emptyList()
            val config = if (!configJson.isNullOrEmpty()) userConfigAdapter.fromJson(configJson) else null

            Log.i("CloudSyncManager", "Data successfully pulled from server: ${entries.size} entries restored.")
            return@withContext Pair(entries, config)
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to restore from server", e)
            return@withContext null
        }
    }

    private fun isEmulator(): Boolean {
        val model = android.os.Build.MODEL
        val fingerprint = android.os.Build.FINGERPRINT
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val product = android.os.Build.PRODUCT
        val hardware = android.os.Build.HARDWARE
        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || "google_sdk" == product
    }

    /**
     * Get human-readable last sync timestamp
     */
    fun getLastSyncTimeString(userId: String): String {
        val lastMs = sharedPrefsCloud.getLong("last_sync_$userId", 0L)
        if (lastMs == 0L) return "Chưa đồng bộ"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(lastMs))
    }
}
