package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.SalaryCalculator
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AutoCheckInWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        val scheduledTimeMs = inputData.getLong("scheduledTimeMs", 0L)

        android.util.Log.d("AutoCheckInWorker", "Thực hiện tự động vào ca cho user: $uid, scheduledTime: $scheduledTimeMs")

        try {
            val database = AppDatabase.getInstance(context)
            val targetDate = Date(if (scheduledTimeMs > 0) scheduledTimeMs else System.currentTimeMillis())
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(targetDate)
            val altTodayStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(targetDate)
            
            // 1. Kiểm tra ngày nghỉ cuối tuần hoặc ngày lễ
            val isSunday = SalaryCalculator.isSunday(todayStr)
            val isHoliday = SalaryCalculator.isHoliday(todayStr)
            if (isSunday || isHoliday) {
                android.util.Log.d("AutoCheckInWorker", "Bỏ qua chấm công tự động vì hôm nay là ngày nghỉ hoặc ngày lễ ($todayStr).")
                val nextCheckInMs = NotificationHelper.estimateHistoricalCheckInTime(context, uid)
                NotificationHelper.scheduleAutoCheckIn(context, uid, nextCheckInMs)
                return Result.success()
            }

            // 2. KIỂM TRA NGHỈ PHÉP: Không được tự động vào ca nếu người dùng nghỉ phép
            val existingToday = database.timeEntryDao().getEntryByDate(uid, todayStr)
                ?: database.timeEntryDao().getEntryByDate(uid, altTodayStr)

            var isUserOnLeave = existingToday != null && SalaryCalculator.isLeaveType(existingToday.dayType)

            if (!isUserOnLeave) {
                try {
                    val remoteRecord = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                        com.example.data.FirestoreService.getAttendanceRecordForDate(uid, todayStr)
                    }
                    if (remoteRecord != null && SalaryCalculator.isLeaveType(remoteRecord.status)) {
                        isUserOnLeave = true
                    }
                } catch (e: Exception) {
                    android.util.Log.d("AutoCheckInWorker", "Lỗi kiểm tra trạng thái nghỉ phép từ Firestore: ${e.message}")
                }
            }

            if (isUserOnLeave) {
                android.util.Log.d("AutoCheckInWorker", "Bỏ qua tự động vào ca vì người dùng đang nghỉ phép ngày $todayStr (${existingToday?.dayType}).")
                val nextCheckInMs = NotificationHelper.estimateHistoricalCheckInTime(context, uid)
                NotificationHelper.scheduleAutoCheckIn(context, uid, nextCheckInMs)
                return Result.success()
            }
            
            // 3. Xung đột: Kiểm tra xem đang có ca nào active hay không
            val activeEntry = database.timeEntryDao().getActiveEntry(uid)

            if (activeEntry != null && activeEntry.isWorking) {
                // Người dùng đã vào ca (thủ công hoặc ca trước chưa ra ca) -> Tránh ghi đè ca đang làm
                android.util.Log.d("AutoCheckInWorker", "Xung đột: Người dùng đang trong ca làm việc (${activeEntry.date}). Bỏ qua tự động vào ca.")
                val sharedPrefs = NotificationHelper.getPrefs(context, uid)
                val autoCheckoutEnabled = sharedPrefs.getBoolean("auto_checkout_enabled", false)
                if (autoCheckoutEnabled) {
                    val checkoutMs = NotificationHelper.estimateHistoricalCheckoutTime(context, uid, activeEntry)
                    NotificationHelper.scheduleAutoCheckOut(context, uid, checkoutMs)
                }
            } else if (existingToday != null && existingToday.checkInTime != null) {
                // Người dùng đã chủ động vào ca hoặc đã có dữ liệu chấm công cho ngày hôm nay
                android.util.Log.d("AutoCheckInWorker", "Xung đột: Đã có bản ghi chấm công ngày $todayStr. Bỏ qua tự động vào ca.")
                val sharedPrefs = NotificationHelper.getPrefs(context, uid)
                val autoCheckoutEnabled = sharedPrefs.getBoolean("auto_checkout_enabled", false)
                if (autoCheckoutEnabled && existingToday.isWorking) {
                    val checkoutMs = NotificationHelper.estimateHistoricalCheckoutTime(context, uid, existingToday)
                    NotificationHelper.scheduleAutoCheckOut(context, uid, checkoutMs)
                }
            } else {
                val checkInMs = if (scheduledTimeMs > 0) scheduledTimeMs else System.currentTimeMillis()
                val cal = Calendar.getInstance().apply { timeInMillis = checkInMs }
                val hour = cal.get(Calendar.HOUR_OF_DAY)

                val sId = if (hour >= 15 || hour < 6) "ca_dem" else "ca1"
                val sType = if (sId == "ca_dem") "NIGHT" else "DAY"

                val dayType = when {
                    sType == "NIGHT" -> "NIGHT"
                    SalaryCalculator.isHoliday(todayStr) -> "HOLIDAY"
                    SalaryCalculator.isSunday(todayStr) -> "SUNDAY"
                    else -> "NORMAL"
                }

                val newEntry = TimeEntry(
                    userId = uid,
                    date = todayStr,
                    checkInTime = checkInMs,
                    checkOutTime = null,
                    isWorking = true,
                    dayType = dayType,
                    shiftId = sId,
                    shiftType = sType,
                    note = "Tự động vào ca"
                )

                val userConfig = database.userConfigDao().getConfigForUser(uid) ?: UserConfig(userId = uid)
                val calculated = SalaryCalculator.calculateSingleEntry(newEntry, userConfig)

                database.timeEntryDao().insertOrUpdate(calculated)

                // Đồng bộ Firestore
                try {
                    val record = com.example.data.AttendanceRecord(
                        id = calculated.id.toLong(),
                        uid = calculated.userId,
                        dateString = calculated.date,
                        clockInTime = calculated.checkInTime ?: 0L,
                        clockOutTime = calculated.checkOutTime,
                        status = calculated.dayType,
                        notes = calculated.note ?: ""
                    )
                    com.example.data.FirestoreService.saveAttendanceRecord(record)
                } catch (e: Exception) {
                    android.util.Log.e("AutoCheckInWorker", "Lỗi đồng bộ Firestore sau khi tự động vào ca: ${e.message}")
                }

                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(checkInMs))

                // Hiển thị thông báo theo yêu cầu: "hh:mm Tôi đã tự động chấm công cho bạn"
                NotificationHelper.showNotification(
                    context = context,
                    title = "Bạn đã vào ca",
                    message = "Lúc $timeStr",
                    notificationId = 1003,
                    uid = uid
                )

                // Nếu bật tự động ra ca, lên lịch auto checkout cho ca này
                val sharedPrefs = NotificationHelper.getPrefs(context, uid)
                val autoCheckoutEnabled = sharedPrefs.getBoolean("auto_checkout_enabled", false)
                if (autoCheckoutEnabled) {
                    val checkoutMs = NotificationHelper.estimateHistoricalCheckoutTime(context, uid, calculated)
                    NotificationHelper.scheduleAutoCheckOut(context, uid, checkoutMs)
                }
            }

            // Lên lịch tự động vào ca cho ngày mai
            val nextCheckInMs = NotificationHelper.estimateHistoricalCheckInTime(context, uid)
            NotificationHelper.scheduleAutoCheckIn(context, uid, nextCheckInMs)

            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("AutoCheckInWorker", "Lỗi trong quá trình tự động vào ca: ${e.message}")
            return Result.failure()
        }
    }
}
