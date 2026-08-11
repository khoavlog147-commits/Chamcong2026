package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.FirestoreService
import com.example.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            var uid = inputData.getString("uid")
            if (uid.isNullOrBlank()) {
                uid = sharedPrefs.getString("current_user_uid", "") ?: ""
            }

            if (uid.isBlank()) {
                val db = AppDatabase.getInstance(context)
                val active = db.timeEntryDao().getActiveEntry("user")
                uid = active?.userId ?: ""
            }

            if (uid.isBlank()) {
                Log.w("AdminNotificationWorker", "Không tìm thấy UID người dùng để kiểm tra thông báo.")
                return@withContext Result.success()
            }

            val lastCheckTime = sharedPrefs.getLong("admin_notif_last_check_${uid}", 0L)
            
            val notifications = FirestoreService.getUnreadAdminNotifications(uid, lastCheckTime)

            var newestTimestamp = lastCheckTime
            for (notif in notifications) {
                if (notif.createdAt > lastCheckTime) {
                    val notifId = (notif.createdAt % 1000000).toInt() + 20000
                    NotificationHelper.showNotification(
                        context = context,
                        title = if (notif.title.isNotBlank()) notif.title else "📢 Thông báo từ Admin",
                        message = notif.message,
                        notificationId = notifId
                    )
                    if (notif.createdAt > newestTimestamp) {
                        newestTimestamp = notif.createdAt
                    }
                }
            }

            if (newestTimestamp > lastCheckTime) {
                sharedPrefs.edit().putLong("admin_notif_last_check_${uid}", newestTimestamp).apply()
            }

            Log.d("AdminNotificationWorker", "Đã quét ${notifications.size} thông báo Admin mới cho UID $uid.")
            Result.success()
        } catch (e: Exception) {
            Log.e("AdminNotificationWorker", "Lỗi kiểm tra thông báo Admin: ${e.message}", e)
            Result.retry()
        }
    }
}
