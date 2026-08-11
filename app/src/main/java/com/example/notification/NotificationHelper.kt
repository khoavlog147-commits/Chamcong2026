package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MainActivity
import com.example.R
import com.example.data.model.TimeEntry
import com.example.data.SalaryCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

object NotificationHelper {
    const val CHANNEL_ID = "timesnap_reminder_channel"
    private const val CHANNEL_NAME = "Nhắc nhở chấm công"
    private const val CHANNEL_DESC = "Kênh thông báo nhắc nhở nhân viên thực hiện check-in/check-out đúng giờ"

    // Khởi tạo Notification Channel với mức độ ưu tiên cao (Android 10+ / API 26+)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Hiển thị thông báo ngay lập tức
    fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    // Kiểm tra quyền thông báo (Android 13+ / API 33+)
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Lên lịch nhắc Check-in
    fun scheduleCheckInReminder(context: Context, uid: String, targetTimeMs: Long) {
        val now = System.currentTimeMillis()
        var delayMs = targetTimeMs - now
        
        if (delayMs < 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = targetTimeMs
                add(Calendar.DAY_OF_YEAR, 1)
            }
            delayMs = cal.timeInMillis - now
        }

        val data = Data.Builder()
            .putString("uid", uid)
            .putString("reminderType", "CHECK_IN")
            .putLong("scheduledTimeMs", targetTimeMs)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("checkin_reminder_$uid")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "checkin_reminder_$uid",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Hủy nhắc nhở Check-in
    fun cancelCheckInReminder(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("checkin_reminder_$uid")
    }

    // Lên lịch nhắc Check-out nối đuôi động
    fun scheduleCheckOutReminder(context: Context, uid: String, delayMs: Long, shiftId: String) {
        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) return

        val data = Data.Builder()
            .putString("uid", uid)
            .putString("reminderType", "CHECK_OUT")
            .putString("shiftId", shiftId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("checkout_reminder_$uid")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "checkout_reminder_$uid",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Hủy nhắc nhở Check-out
    fun cancelCheckOutReminder(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("checkout_reminder_$uid")
    }

    // Lên lịch lắng nghe / đồng bộ thông báo từ Admin khi có kết nối mạng (NetworkType.CONNECTED)
    fun scheduleAdminNotificationSync(context: Context, uid: String) {
        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        if (uid.isNotBlank()) {
            sharedPrefs.edit().putString("current_user_uid", uid).apply()
        }

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val periodicRequest = androidx.work.PeriodicWorkRequestBuilder<AdminNotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(androidx.work.workDataOf("uid" to uid))
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            "AdminNotificationWorker_$uid",
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            periodicRequest
        )

        val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<AdminNotificationWorker>()
            .setConstraints(constraints)
            .setInputData(androidx.work.workDataOf("uid" to uid))
            .build()

        WorkManager.getInstance(context.applicationContext).enqueue(oneTimeRequest)
    }

    fun scheduleCheckOutReminderForActiveEntry(context: Context, uid: String, activeEntry: TimeEntry) {
        if (!activeEntry.isWorking) return

        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) return

        val autoCheckoutEnabled = sharedPrefs.getBoolean("auto_checkout_enabled", false)
        if (autoCheckoutEnabled) return

        val checkInTime = activeEntry.checkInTime ?: System.currentTimeMillis()
        val shift = SalaryCalculator.getShiftForEntry(activeEntry)

        val targetCal = Calendar.getInstance().apply {
            timeInMillis = checkInTime
            add(Calendar.HOUR_OF_DAY, 11)
            add(Calendar.MINUTE, 45)
        }

        val now = System.currentTimeMillis()
        var delayMs = targetCal.timeInMillis - now
        if (delayMs <= 0) delayMs = 2000L

        scheduleCheckOutReminder(context, uid, delayMs, shift.shiftId)
    }

    fun getContinuousWeekIndex(timeMs: Long): Long {
        val tz = java.util.TimeZone.getDefault()
        val localMs = timeMs + tz.getOffset(timeMs)
        val offsetMs = 3L * 24 * 3600 * 1000L // +3 days to start on Monday (Epoch 01/01/1970 was Thursday)
        return (localMs + offsetMs) / (7L * 24 * 3600 * 1000L)
    }

    fun getEffectiveCheckInTime(context: Context, targetMs: Long = System.currentTimeMillis()): String {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val rawIn = prefs.getString("custom_check_in_time", "") ?: ""
        val rawOut = prefs.getString("custom_checkout_time", "") ?: ""
        
        if (rawIn.isBlank() && rawOut.isBlank()) return ""
        
        val rotationWeeks = prefs.getInt("shift_rotation_weeks", 2).coerceIn(0, 5)
        if (rotationWeeks == 0) {
            return rawIn
        }
        
        var anchorTime = prefs.getLong("shift_anchor_time", 0L)
        if (anchorTime <= 0L) {
            anchorTime = System.currentTimeMillis()
            prefs.edit().putLong("shift_anchor_time", anchorTime).apply()
        }
        
        val weekAnchor = getContinuousWeekIndex(anchorTime)
        val weekTarget = getContinuousWeekIndex(targetMs)
        val weeksPassed = (weekTarget - weekAnchor).coerceAtLeast(0)
        
        val anchorType = prefs.getString("shift_anchor_type", "DAY") ?: "DAY"
        val cyclePhase = ((weeksPassed / rotationWeeks) % 2).toInt()
        val effectivePhase = if (anchorType == "NIGHT") (cyclePhase + 1) % 2 else cyclePhase
        
        if (effectivePhase == 1) {
            if (rawOut.isNotBlank()) return rawOut
            val parts = rawIn.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 19
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
            val swappedH = if (h >= 12) (h - 12) else (h + 12)
            return String.format(Locale.getDefault(), "%02d:%02d", swappedH, m)
        }
        return rawIn
    }

    fun getEffectiveCheckOutTime(context: Context, targetMs: Long = System.currentTimeMillis()): String {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val rawIn = prefs.getString("custom_check_in_time", "") ?: ""
        val rawOut = prefs.getString("custom_checkout_time", "") ?: ""
        
        if (rawIn.isBlank() && rawOut.isBlank()) return ""
        
        val rotationWeeks = prefs.getInt("shift_rotation_weeks", 2).coerceIn(0, 5)
        if (rotationWeeks == 0) {
            return rawOut
        }
        
        var anchorTime = prefs.getLong("shift_anchor_time", 0L)
        if (anchorTime <= 0L) {
            anchorTime = System.currentTimeMillis()
            prefs.edit().putLong("shift_anchor_time", anchorTime).apply()
        }
        
        val weekAnchor = getContinuousWeekIndex(anchorTime)
        val weekTarget = getContinuousWeekIndex(targetMs)
        val weeksPassed = (weekTarget - weekAnchor).coerceAtLeast(0)
        
        val anchorType = prefs.getString("shift_anchor_type", "DAY") ?: "DAY"
        val cyclePhase = ((weeksPassed / rotationWeeks) % 2).toInt()
        val effectivePhase = if (anchorType == "NIGHT") (cyclePhase + 1) % 2 else cyclePhase
        
        if (effectivePhase == 1) {
            if (rawIn.isNotBlank()) return rawIn
            val parts = rawOut.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
            val swappedH = if (h >= 12) (h - 12) else (h + 12)
            return String.format(Locale.getDefault(), "%02d:%02d", swappedH, m)
        }
        return rawOut
    }

    // Ước tính giờ ra ca dựa trên Mode (Tần suất xuất hiện nhiều nhất) và chu kỳ đổi ca
    suspend fun estimateHistoricalCheckoutTime(context: Context, uid: String, activeEntry: TimeEntry): Long {
        var targetHour = -1
        var targetMin = -1

        try {
            val db = com.example.data.db.AppDatabase.getInstance(context)
            val entries = db.timeEntryDao().getLastCompletedEntries(uid, 100)
            
            if (entries.isNotEmpty()) {
                val activeCi = activeEntry.checkInTime ?: System.currentTimeMillis()
                val activeCal = java.util.Calendar.getInstance().apply { timeInMillis = activeCi }
                val isNightShift = activeCal.get(java.util.Calendar.HOUR_OF_DAY) >= 15
                
                val targetEntries = entries.filter { entry ->
                    val ci = entry.checkInTime ?: return@filter false
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ci }
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    if (isNightShift) hour >= 15 else hour < 15
                }.ifEmpty { entries }
                
                val frequencyMap = mutableMapOf<Int, Int>()
                for (entry in targetEntries) {
                    val co = entry.checkOutTime ?: continue
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = co }
                    val rawMins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                    val roundedMins = ((rawMins + 7) / 15) * 15 % 1440
                    frequencyMap[roundedMins] = (frequencyMap[roundedMins] ?: 0) + 1
                }

                val mostFrequentMins = frequencyMap.maxByOrNull { it.value }?.key
                if (mostFrequentMins != null) {
                    targetHour = mostFrequentMins / 60
                    targetMin = mostFrequentMins % 60
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (targetHour == -1) {
            targetHour = 17
            targetMin = 30
        }
        val fallbackTargetHour = targetHour
        val fallbackTargetMin = targetMin

        val ciMs = activeEntry.checkInTime ?: System.currentTimeMillis()
        var currentTargetMs = ciMs
        
        var cal = java.util.Calendar.getInstance()
        var found = false
        var loopCount = 0
        
        while (!found && loopCount < 14) {
            val customTime = getEffectiveCheckOutTime(context, currentTargetMs)
            
            if (customTime.isNotBlank() && customTime.contains(":") && customTime.length == 5) {
                val parts = customTime.split(":")
                targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 17
                targetMin = parts.getOrNull(1)?.toIntOrNull() ?: 30
            } else {
                targetHour = fallbackTargetHour
                targetMin = fallbackTargetMin
            }
            
            cal = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTargetMs
                set(java.util.Calendar.HOUR_OF_DAY, targetHour)
                set(java.util.Calendar.MINUTE, targetMin)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            // CheckOut có thể qua ngày hôm sau nếu là ca đêm. 
            // Ta đảm bảo Checkout > Checkin. Nếu cal < ciMs, cộng 1 ngày
            if (cal.timeInMillis < ciMs + 4 * 3600 * 1000L) { // Tối thiểu làm 4 tiếng
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            
            if (cal.timeInMillis > System.currentTimeMillis()) {
                found = true
                break
            }
            
            val nextDay = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTargetMs
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            currentTargetMs = nextDay.timeInMillis
            loopCount++
        }
        
        return cal.timeInMillis
    }

    // Đặt lịch Tự động Vào Ca
    fun scheduleAutoCheckIn(context: Context, uid: String, targetTimeMs: Long) {
        val now = System.currentTimeMillis()
        var delayMs = targetTimeMs - now
        if (delayMs < 0) {
            if (delayMs > -15 * 60 * 1000) delayMs = 0 else return
        }

        val data = Data.Builder()
            .putString("uid", uid)
            .putLong("scheduledTimeMs", targetTimeMs)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutoCheckInWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("auto_checkin_$uid")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "auto_checkin_$uid",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Hủy Tự động Vào Ca
    fun cancelAutoCheckIn(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("auto_checkin_$uid")
    }

    // Đặt lịch Tự động Ra Ca
    fun scheduleAutoCheckOut(context: Context, uid: String, targetTimeMs: Long) {
        val now = System.currentTimeMillis()
        var delayMs = targetTimeMs - now
        if (delayMs < 0) {
            if (delayMs > -15 * 60 * 1000) delayMs = 0 else return
        }

        cancelCheckOutReminder(context, uid)

        val data = Data.Builder()
            .putString("uid", uid)
            .putLong("scheduledTimeMs", targetTimeMs)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutoCheckOutWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("auto_checkout_$uid")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "auto_checkout_$uid",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Hủy Tự động Ra Ca
    fun cancelAutoCheckOut(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("auto_checkout_$uid")
    }

    // Lên lịch nhắc Check-in cho ca tiếp theo
    fun scheduleNextCheckInReminder(context: Context, uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val autoInEnabled = sharedPrefs.getBoolean("auto_check_in_enabled", false)
                val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
                
                if (!autoInEnabled && !notificationsEnabled) {
                    cancelCheckInReminder(context, uid)
                    cancelAutoCheckIn(context, uid)
                    return@launch
                }

                val targetMs = estimateHistoricalCheckInTime(context, uid)
                
                if (autoInEnabled) {
                    scheduleAutoCheckIn(context, uid, targetMs)
                    cancelCheckInReminder(context, uid)
                } else {
                    scheduleCheckInReminder(context, uid, targetMs)
                    cancelAutoCheckIn(context, uid)
                }
            } catch (e: Exception) {}
        }
    }

    // Ước tính giờ vào ca dựa trên Mode (Tần suất xuất hiện nhiều nhất) và chu kỳ đổi ca
    suspend fun estimateHistoricalCheckInTime(context: Context, uid: String): Long {
        var targetHour = -1
        var targetMin = -1

        try {
            val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val rotationWeeks = sharedPrefs.getInt("shift_rotation_weeks", 2).coerceIn(0, 5)

            val db = com.example.data.db.AppDatabase.getInstance(context)
            val entries = db.timeEntryDao().getLastCompletedEntries(uid, 100)
            
            if (entries.isNotEmpty()) {
                val nowMs = System.currentTimeMillis()
                
                fun getWeekIndex(timeMs: Long): Long {
                    val tz = java.util.TimeZone.getDefault()
                    val localMs = timeMs + tz.getOffset(timeMs)
                    val offsetMs = 3L * 24 * 3600 * 1000L
                    return (localMs + offsetMs) / (7L * 24 * 3600 * 1000L)
                }
                
                val currentWeek = getWeekIndex(nowMs)
                
                val weekShifts = mutableMapOf<Long, String>()
                val weekEntries = entries.groupBy { getWeekIndex(it.checkInTime ?: 0L) }
                
                for ((w, list) in weekEntries) {
                    if (w == 0L) continue
                    var nightCount = 0
                    var dayCount = 0
                    for (e in list) {
                        val ci = e.checkInTime ?: continue
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ci }
                        if (cal.get(java.util.Calendar.HOUR_OF_DAY) >= 15) nightCount++ else dayCount++
                    }
                    weekShifts[w] = if (nightCount > dayCount) "NIGHT" else "DAY"
                }
                
                var predictedShift = "DAY"
                
                if (rotationWeeks == 0) {
                    val lastWeek = weekShifts.keys.filter { it <= currentWeek }.maxOrNull()
                    if (lastWeek != null) predictedShift = weekShifts[lastWeek] ?: "DAY"
                } else {
                    val maxWeek = weekShifts.keys.maxOrNull() ?: currentWeek
                    
                    if (maxWeek >= currentWeek) {
                        predictedShift = weekShifts[maxWeek] ?: "DAY"
                    } else {
                        val lastShift = weekShifts[maxWeek] ?: "DAY"
                        var consecutive = 0
                        var currW = maxWeek
                        
                        while (weekShifts.containsKey(currW) && weekShifts[currW] == lastShift) {
                            consecutive++
                            currW--
                        }
                        
                        val weeksDiff = (currentWeek - maxWeek).toInt()
                        var simShift = lastShift
                        var simConsecutive = consecutive
                        
                        for (i in 1..weeksDiff) {
                            if (simConsecutive >= rotationWeeks) {
                                simShift = if (simShift == "NIGHT") "DAY" else "NIGHT"
                                simConsecutive = 1
                            } else {
                                simConsecutive++
                            }
                        }
                        predictedShift = simShift
                    }
                }
                
                val targetEntries = entries.filter { entry ->
                    val ci = entry.checkInTime ?: return@filter false
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ci }
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    if (predictedShift == "NIGHT") hour >= 15 else hour < 15
                }.ifEmpty { entries }
                
                val frequencyMap = mutableMapOf<Int, Int>()
                for (entry in targetEntries) {
                    val ci = entry.checkInTime ?: continue
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ci }
                    val rawMins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                    val roundedMins = ((rawMins + 7) / 15) * 15 % 1440
                    frequencyMap[roundedMins] = (frequencyMap[roundedMins] ?: 0) + 1
                }

                val mostFrequentMins = frequencyMap.maxByOrNull { it.value }?.key
                if (mostFrequentMins != null) {
                    targetHour = mostFrequentMins / 60
                    targetMin = mostFrequentMins % 60
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (targetHour == -1) {
            targetHour = 7
            targetMin = 30
        }
        val fallbackTargetHour = targetHour
        val fallbackTargetMin = targetMin

        var currentTargetMs = System.currentTimeMillis()
        var cal = java.util.Calendar.getInstance()
        var found = false
        var loopCount = 0
        
        while (!found && loopCount < 14) {
            val customTime = getEffectiveCheckInTime(context, currentTargetMs)
            
            if (customTime.isNotBlank() && customTime.contains(":") && customTime.length == 5) {
                val parts = customTime.split(":")
                targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
                targetMin = parts.getOrNull(1)?.toIntOrNull() ?: 30
            } else {
                targetHour = fallbackTargetHour
                targetMin = fallbackTargetMin
            }
            
            cal = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTargetMs
                set(java.util.Calendar.HOUR_OF_DAY, targetHour)
                set(java.util.Calendar.MINUTE, targetMin)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
            val isSunday = com.example.data.SalaryCalculator.isSunday(dateStr)
            val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
            
            if (!isSunday && !isHoliday && cal.timeInMillis > System.currentTimeMillis()) {
                found = true
                break
            }
            
            val nextDay = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTargetMs
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            currentTargetMs = nextDay.timeInMillis
            loopCount++
        }
        
        return cal.timeInMillis
    }
}
