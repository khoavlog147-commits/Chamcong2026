package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthController
import com.example.auth.UserSession
import com.example.data.db.AppDatabase
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.data.repository.TimeRepository
import com.example.data.repository.CloudSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class TimeSnapViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = TimeRepository(database.timeEntryDao(), database.userConfigDao())
    val authController = AuthController(application, repository)
    val cloudSyncManager = CloudSyncManager(application)
    private var syncJob: kotlinx.coroutines.Job? = null
    private var adminNotifListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    val hasRestoredForSession = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    // Current logged-in UI session state
    val currentUserSession: StateFlow<UserSession?> = authController.currentUserFlow

    // Live sync status text
    private val _cloudSyncStatus = MutableStateFlow("Đang cập nhật")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus

    // Date formatter
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    
    // UI Selected month (For Calendar View: "yyyy-MM")
    private val _currentSelectedMonth = MutableStateFlow(monthFormatter.format(Date()))
    val currentSelectedMonth: StateFlow<String> = _currentSelectedMonth

    // Custom UI Trigger to refresh calculations or triggers
    private val _triggerRefresh = MutableStateFlow(0)

    // Reactive flow of current user salary config
    val userConfig: StateFlow<UserConfig?> = currentUserSession
        .flatMapLatest { session ->
            if (session != null) {
                repository.getConfig(session.uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Reactive flow of user's earliest recorded entry date (used for auto-detecting new user start date)
    val userEarliestEntryDate: StateFlow<String?> = currentUserSession
        .flatMapLatest { session ->
            if (session != null) {
                repository.getEntries(session.uid).map { entries ->
                    entries.minByOrNull { it.date }?.date
                }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Reactive flow of user's actual and projected income comparison over the last 6 months
    val salaryHistoryState: StateFlow<List<MonthlySalaryPoint>> = currentUserSession
        .flatMapLatest { session ->
            if (session != null) {
                combine(
                    repository.getEntries(session.uid),
                    userConfig,
                    userEarliestEntryDate,
                    _triggerRefresh
                ) { allEntries, config, earliestDate, _ ->
                    if (config != null) {
                        val format = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        val points = mutableListOf<MonthlySalaryPoint>()
                        // Calculate for the last 6 months (chronological order)
                        for (i in 5 downTo 0) {
                            val tempCal = Calendar.getInstance()
                            tempCal.add(Calendar.MONTH, -i)
                            val monthStr = format.format(tempCal.time)
                            
                            val parts = monthStr.split("-")
                            val mPattern = "${parts[0]}-${parts[1]}-"
                            val mPatternSlash = "/${parts[1]}/${parts[0]}"
                            
                            val monthEntries = allEntries.filter { 
                                it.date.startsWith(mPattern) || it.date.endsWith(mPatternSlash)
                            }
                            
                            val summary = calculateSalarySummaryForMonth(monthStr, monthEntries, config, earliestDate)
                            points.add(
                                MonthlySalaryPoint(
                                    monthStr = monthStr,
                                    luongThucNhan = summary.luongThucNhan,
                                    workingDays = summary.workingDays,
                                    luongDuKien = summary.luongThucNhan
                                )
                            )
                        }
                        points
                    } else {
                        emptyList()
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive list of active month entries
    val monthTimeEntries: StateFlow<List<TimeEntry>> = combine(
        currentUserSession,
        currentSelectedMonth,
        _triggerRefresh
    ) { session, month, _ ->
        Pair(session, month)
    }.flatMapLatest { (session, month) ->
        if (session != null) {
            val parts = month.split("-")
            val monthPattern = if (parts.size == 2) "%/${parts[1]}/${parts[0]}" else "%"
            val altMonthPattern = if (parts.size == 2) "${parts[0]}-${parts[1]}-%" else "%"
            repository.getEntriesInMonth(session.uid, monthPattern, altMonthPattern)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active working entry of today (checked-in but not checked-out)
    private val _activeWorkingEntry = MutableStateFlow<TimeEntry?>(null)
    val activeWorkingEntry: StateFlow<TimeEntry?> = _activeWorkingEntry

    // Running duration tracker ticker ("Đã làm: X giờ Y phút")
    private val _runningDurationText = MutableStateFlow("")
    val runningDurationText: StateFlow<String> = _runningDurationText

    // All-time/Month summaries list
    private val _salarySummaryState = MutableStateFlow<SalarySummary?>(null)
    val salarySummaryState: StateFlow<SalarySummary?> = _salarySummaryState

    private fun getMonthDifference(startStr: String, endStr: String): Int {
        if (startStr.isEmpty() || endStr.isEmpty()) return 0
        try {
            val parts1 = startStr.split("-")
            val parts2 = endStr.split("-")
            val y1 = parts1[0].toInt()
            val m1 = parts1[1].toInt()
            val y2 = parts2[0].toInt()
            val m2 = parts2[1].toInt()
            return (y2 - y1) * 12 + (m2 - m1)
        } catch (e: Exception) {
            return 0
        }
    }

    init {
        // Automatic monthly phepNamConLai increment (+1 per month) - Runs once at startup
        viewModelScope.launch(Dispatchers.IO) {
            val config = userConfig.filterNotNull().first()
            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            if (config.lastAccumulatedMonth.isEmpty()) {
                val updated = config.copy(lastAccumulatedMonth = currentMonthStr)
                repository.saveConfig(updated)
            } else if (config.lastAccumulatedMonth != currentMonthStr) {
                val diff = getMonthDifference(config.lastAccumulatedMonth, currentMonthStr)
                if (diff > 0) {
                    val updated = config.copy(
                        phepNamConLai = config.phepNamConLai + diff,
                        lastAccumulatedMonth = currentMonthStr
                    )
                    repository.saveConfig(updated)
                }
            }
        }

        // Realtime UserConfig sync listener from Firestore
        viewModelScope.launch(Dispatchers.IO) {
            var realtimeConfigJob: kotlinx.coroutines.Job? = null
            currentUserSession.collect { session ->
                realtimeConfigJob?.cancel()
                if (session != null && !session.uid.startsWith("demo")) {
                    realtimeConfigJob = viewModelScope.launch(Dispatchers.IO) {
                        com.example.data.FirestoreService.getUserConfigFlow(session.uid).collect { remoteConfig ->
                            if (remoteConfig != null) {
                                android.util.Log.d("TimeSnapViewModel", "Realtime user config update received from server for ${session.uid}")
                                repository.saveConfig(remoteConfig)
                            }
                        }
                    }
                }
            }
        }

        // Realtime AttendanceLogs sync listener from Firestore
        viewModelScope.launch(Dispatchers.IO) {
            var realtimeLogsJob: kotlinx.coroutines.Job? = null
            currentUserSession.collect { session ->
                realtimeLogsJob?.cancel()
                if (session != null && !session.uid.startsWith("demo")) {
                    realtimeLogsJob = viewModelScope.launch(Dispatchers.IO) {
                        com.example.data.FirestoreService.getAttendanceLogsFlow(session.uid).collect { _ ->
                            android.util.Log.d("TimeSnapViewModel", "Realtime attendance logs update received from server for ${session.uid}")
                            syncAttendanceLogsFromFirestore(session.uid)
                        }
                    }
                }
            }
        }

        // Automatic iCloud/Server Data Restore check on Login (survives app re-install)
        viewModelScope.launch(Dispatchers.IO) {
            var lastUid: String? = null
            currentUserSession.collect { session ->
                if (session == null) {
                    // Hủy đăng ký listener cũ khi session null (logout)
                    adminNotifListenerRegistration?.remove()
                    adminNotifListenerRegistration = null
                    lastUid = null
                    _cloudSyncStatus.value = "Yêu cầu đăng nhập"
                } else if (session.uid != lastUid) {
                    // Hủy đăng ký cũ nếu có
                    adminNotifListenerRegistration?.remove()
                    adminNotifListenerRegistration = null

                    lastUid = session.uid
                    hasRestoredForSession[session.uid] = false
                    _cloudSyncStatus.value = "Đang kiểm tra..."
                    checkAndFetchSalaryConfigFromFirestore(session.uid)
                    
                    // Khởi tạo kênh thông báo và lên lịch nhắc nhở Check-in
                    try {
                        com.example.notification.NotificationHelper.createNotificationChannel(getApplication())
                        com.example.notification.NotificationHelper.scheduleNextCheckInReminder(getApplication(), session.uid)
                        loadLocalCachedNotifications(session.uid)
                        fetchAdminNotifications()

                        // Đăng ký realtime listener lắng nghe thông báo mới từ Firestore
                        try {
                            val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic")
                                    || android.os.Build.FINGERPRINT.startsWith("unknown")
                                    || android.os.Build.MODEL.contains("google_sdk")
                                    || android.os.Build.MODEL.contains("Emulator")
                                    || android.os.Build.MODEL.contains("Android SDK built for x86")
                                    || android.os.Build.HARDWARE.contains("goldfish")
                                    || android.os.Build.HARDWARE.contains("ranchu")
                            if (!isEmulator) {
                                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                adminNotifListenerRegistration = firestore.collection("users").document(session.uid)
                                    .collection("notifications")
                                    .addSnapshotListener { _, error ->
                                        if (error != null) {
                                            android.util.Log.e("TimeSnapViewModel", "Lỗi realtime notifications: ${error.message}")
                                            return@addSnapshotListener
                                        }
                                        fetchAdminNotifications()
                                    }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TimeSnapViewModel", "Failed to register realtime notif listener: ${e.message}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TimeSnapViewModel", "Failed to init notifications: ${e.message}")
                    }

                    try {
                        // 1. Migrate local guest records if any
                        migrateGuestDataToUser(session.uid)
                        
                        // 2. Normalize any existing local TimeEntry date formats (from dd/MM/yyyy to yyyy-MM-dd)
                        normalizeTimeEntryDates(session.uid)
                        
                        // 3. If local entries is empty, try to pull from new cloud backup (overtime_sync/backup)
                        var currentEntries = repository.getEntries(session.uid).first()
                        if (currentEntries.isEmpty()) {
                            restoreDataFromServerSuspended(session.uid)
                            currentEntries = repository.getEntries(session.uid).first()
                        }
                        
                        // 4. Sync latest attendance logs from Firestore (if Admin clocked out or edited time)
                        syncAttendanceLogsFromFirestore(session.uid)

                        // 5. Check and migrate legacy AttendanceRecord logs (from local and remote databases)
                        migrateLegacyData(session.uid)
                        
                        // 5.5. Deduplicate and normalize all records to guarantee 1 entry per day
                        normalizeTimeEntryDates(session.uid)
                        
                        // 6. Final sync: Upload current merged state to the server to ensure cloud is up to date
                        val finalEntries = repository.getEntries(session.uid).first()
                        val config = repository.getConfigDirect(session.uid) ?: UserConfig(userId = session.uid)
                        cloudSyncManager.uploadToServer(session.uid, finalEntries, config)
                        
                        // Update active working entry StateFlow immediately
                        val active = repository.getActiveEntry(session.uid)
                        _activeWorkingEntry.value = active
                        if (active != null && active.isWorking) {
                            com.example.notification.NotificationHelper.scheduleCheckOutReminderForActiveEntry(getApplication(), session.uid, active)
                        }
                        
                        hasRestoredForSession[session.uid] = true
                        _cloudSyncStatus.value = "Đã đồng bộ"
                    } catch (e: Exception) {
                        android.util.Log.e("TimeSnapViewModel", "Auto-restore initialization failed", e)
                        hasRestoredForSession[session.uid] = true
                        _cloudSyncStatus.value = "Lỗi xác thực dữ liệu"
                    }
                }
            }
        }

        // Run reactive worker to fetch active working shift
        viewModelScope.launch(Dispatchers.IO) {
            currentUserSession.flatMapLatest { session ->
                if (session != null) {
                    repository.getActiveEntryFlow(session.uid)
                } else {
                    kotlinx.coroutines.flow.flowOf(null)
                }
            }.collect { active ->
                _activeWorkingEntry.value = active
            }
        }

        // Ticker loop for the active working shift text
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val active = _activeWorkingEntry.value
                val config = userConfig.value
                if (active != null && active.checkInTime != null) {
                    val diffMs = System.currentTimeMillis() - active.checkInTime
                    val hours = diffMs / 3600000
                    val minutes = (diffMs % 3600000) / 60000
                    
                    val breakHours = if (config?.tinhKhauTruNghi == true) config.soGioNghiGiaiLao else 0.0
                    val actualWorkedMin = ((diffMs / 60000.0) - (breakHours * 60.0)).coerceAtLeast(0.0)
                    val actHours = (actualWorkedMin / 60).toInt()
                    val actMinutes = (actualWorkedMin % 60).toInt()

                    val targetMs = active.checkInTime + ((8.0 + breakHours) * 3600000).toLong()
                    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val expectedOut = sdfTime.format(Date(targetMs))

                    if (config?.tinhKhauTruNghi == true) {
                        _runningDurationText.value = "Thực làm: ${actHours}g ${actMinutes}p (Trừ ${config.soGioNghiGiaiLao}g nghỉ)\nĐủ 8 tiếng lúc: $expectedOut"
                    } else {
                        _runningDurationText.value = "Đã làm: ${hours}g ${minutes}p\nĐủ 8 tiếng lúc: $expectedOut"
                    }
                } else {
                    _runningDurationText.value = ""
                }
                delay(10000) // update every 10 seconds is perfect for resources and text accuracy
            }
        }

        // Calculate pay slip summaries automatically when month entries or config changes
        viewModelScope.launch(Dispatchers.Default) {
            combine(monthTimeEntries, userConfig, userEarliestEntryDate) { entries, config, earliestDate ->
                Triple(entries, config, earliestDate)
            }.collectLatest { (entries, config, earliestDate) ->
                if (config != null) {
                    val summary = calculateSalarySummary(entries, config, earliestDate)
                    _salarySummaryState.value = summary
                } else {
                    _salarySummaryState.value = null
                }
            }
        }

        // Automatic cloud remote restore on success login removed to prevent parallel sync state conflicts
        // Logic handled robustly by upper collection listener above

    }

    fun selectMonth(yearMonth: String) {
        _currentSelectedMonth.value = yearMonth
    }

    private fun syncTimeEntryToLegacyLog(entry: TimeEntry) {
        if (entry.userId.startsWith("demo")) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val log = com.example.data.AttendanceRecord(
                    id = entry.id.toLong(),
                    uid = entry.userId,
                    dateString = entry.date,
                    clockInTime = entry.checkInTime ?: 0L,
                    clockOutTime = entry.checkOutTime,
                    status = entry.dayType,
                    notes = entry.note ?: ""
                )
                com.example.data.FirestoreService.saveAttendanceRecord(log)
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Failed to sync legacy log: ${e.message}")
            }
        }
    }

    private suspend fun deleteTimeEntryFromLegacyLog(entry: TimeEntry) {
        if (entry.userId.startsWith("demo")) return
        try {
            com.example.data.FirestoreService.deleteAttendanceRecord(entry.userId, entry.date)
        } catch (e: Exception) {
            android.util.Log.e("TimeSnapViewModel", "Failed to delete legacy log: ${e.message}")
        }
    }

    // Toggle Check-In / Check-Out
    fun toggleCheckIn(
        note: String = "",
        enableAutoCheckout: Boolean? = null,
        customCheckoutTime: String? = null
    ) {
        val session = currentUserSession.value ?: return
        val todayStr = dateFormatter.format(Date())

        viewModelScope.launch(Dispatchers.IO) {
            val sharedPrefs = com.example.notification.NotificationHelper.getPrefs(getApplication<Application>(), session.uid)
            val isAutoCheckoutEnabled = enableAutoCheckout ?: sharedPrefs.getBoolean("auto_checkout_enabled", false)
            val storedCustomTime = customCheckoutTime ?: sharedPrefs.getString("custom_checkout_time", "") ?: ""
            val active = repository.getActiveEntry(session.uid)
            if (active == null) {
                // Check if an entry already exists for today. If so, overwrite it or create new
                val existing = repository.getEntryByDate(session.uid, todayStr)
                val cal = Calendar.getInstance()
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val isSunday = (dayOfWeek == Calendar.SUNDAY)
                val isHoliday = isHolidayDate(todayStr)
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                
                val dayType = when {
                    hour >= 18 -> "NIGHT"
                    isHoliday -> "HOLIDAY"
                    isSunday -> "SUNDAY"
                    else -> "NORMAL"
                }

                val sId = if (hour >= 18) "ca_dem" else "ca1"
                val sType = if (sId == "ca_dem") "NIGHT" else "DAY"

                val newEntry = TimeEntry(
                    id = existing?.id ?: 0,
                    userId = session.uid,
                    date = todayStr,
                    checkInTime = System.currentTimeMillis(),
                    checkOutTime = null,
                    isWorking = true,
                    dayType = dayType,
                    note = note.takeIf { it.isNotBlank() },
                    shiftId = sId,
                    shiftType = sType
                )
                val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(newEntry, userConfig.value)
                repository.insertOrUpdate(calculated)
                syncTimeEntryToLegacyLog(calculated)

                if (isAutoCheckoutEnabled) {
                    try {
                        var targetMs: Long = 0L
                        if (storedCustomTime.isNotBlank()) {
                            val parts = storedCustomTime.split(":")
                            val targetHour = parts.getOrNull(0)?.toIntOrNull()
                            val targetMin = parts.getOrNull(1)?.toIntOrNull()
                            if (targetHour != null && targetMin != null) {
                                val tCal = Calendar.getInstance().apply {
                                    timeInMillis = calculated.checkInTime ?: System.currentTimeMillis()
                                    set(Calendar.HOUR_OF_DAY, targetHour)
                                    set(Calendar.MINUTE, targetMin)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                if (tCal.timeInMillis <= (calculated.checkInTime ?: System.currentTimeMillis())) {
                                    tCal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                                targetMs = tCal.timeInMillis
                            }
                        }

                        if (targetMs <= 0L) {
                            // Tự nhìn lại các ngày cũ ra ca lúc mấy giờ
                            targetMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(
                                context = getApplication(),
                                uid = session.uid,
                                activeEntry = calculated
                            )
                        }

                        com.example.notification.NotificationHelper.scheduleAutoCheckOut(
                            context = getApplication(),
                            uid = session.uid,
                            targetTimeMs = targetMs
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("TimeSnapViewModel", "Failed to schedule auto checkout: ${e.message}")
                    }
                } else {
                    // Lên lịch nhắc Check-out nối đuôi động dựa trên ca làm việc thực tế
                    try {
                        com.example.notification.NotificationHelper.scheduleCheckOutReminderForActiveEntry(
                            context = getApplication(),
                            uid = session.uid,
                            activeEntry = calculated
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("TimeSnapViewModel", "Failed to schedule checkout reminder: ${e.message}")
                    }
                }
            } else {
                // Perform check-out
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                
                val sId = if (active.shiftId == "ca1" && hour >= 20) "ca2" else active.shiftId ?: "ca1"
                val sType = if (sId == "ca2") "DAY_REST" else active.shiftType ?: "DAY"

                val updated = active.copy(
                    checkOutTime = System.currentTimeMillis(),
                    isWorking = false,
                    note = if (note.isNotBlank()) note else active.note,
                    shiftId = sId,
                    shiftType = sType
                )
                val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(updated, userConfig.value)
                repository.insertOrUpdate(calculated)
                syncTimeEntryToLegacyLog(calculated)

                // Hủy nhắc nhở Check-out & Tự động ra ca vì đã check-out thủ công thành công trước hạn
                try {
                    com.example.notification.NotificationHelper.cancelCheckOutReminder(
                        context = getApplication(),
                        uid = session.uid
                    )
                    com.example.notification.NotificationHelper.cancelAutoCheckOut(
                        context = getApplication(),
                        uid = session.uid
                    )
                } catch (e: Exception) {
                    android.util.Log.e("TimeSnapViewModel", "Failed to cancel checkout reminder: ${e.message}")
                }
            }
            triggerSync()
        }
    }
    
    fun updateActiveEntryNote(note: String) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val active = repository.getActiveEntry(session.uid) ?: return@launch
            val updated = active.copy(note = note.takeIf { it.isNotBlank() })
            repository.insertOrUpdate(updated)
            syncTimeEntryToLegacyLog(updated)
            triggerSyncDebounced()
        }
    }

    // Refresh configurations
    fun triggerSync() {
        _triggerRefresh.value += 1
        val session = currentUserSession.value
        if (session != null) {
            if (hasRestoredForSession[session.uid] != true) {
                android.util.Log.d("TimeSnapViewModel", "Tránh ghi đè: Bỏ qua triggerSync vì đang đồng bộ/khôi phục đám mây.")
                return
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val list = repository.getEntries(session.uid).first()
                    val config = repository.getConfigDirect(session.uid) ?: UserConfig(userId = session.uid)
                    cloudSyncManager.uploadToServer(session.uid, list, config)
                    _cloudSyncStatus.value = "Đã đồng bộ"
                } catch (e: Exception) {
                    _cloudSyncStatus.value = "Lỗi đồng bộ tự động"
                }
            }
        }
    }

    fun triggerSyncDebounced() {
        _triggerRefresh.value += 1
        syncJob?.cancel()
        val session = currentUserSession.value
        if (session != null) {
            if (hasRestoredForSession[session.uid] != true) {
                android.util.Log.d("TimeSnapViewModel", "Tránh ghi đè: Bỏ qua triggerSyncDebounced vì đang đồng bộ/khôi phục đám mây.")
                return
            }
            syncJob = viewModelScope.launch(Dispatchers.IO) {
                delay(1500) // Wait 1.5s of typing silence before syncing to cloud database
                try {
                    val list = repository.getEntries(session.uid).first()
                    val config = repository.getConfigDirect(session.uid) ?: UserConfig(userId = session.uid)
                    cloudSyncManager.uploadToServer(session.uid, list, config)
                    _cloudSyncStatus.value = "Đã đồng bộ"
                } catch (e: Exception) {
                    _cloudSyncStatus.value = "Lỗi đồng bộ tự động"
                }
            }
        }
    }

    // Clear and pull fully from mock cloud server
    fun restoreDataFromServer(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                restoreDataFromServerSuspended(userId)
            } finally {
                hasRestoredForSession[userId] = true
            }
        }
    }

    private suspend fun restoreDataFromServerSuspended(userId: String) {
        _cloudSyncStatus.value = "Khôi phục dữ liệu..."
        try {
            val remoteData = cloudSyncManager.downloadFromServer(userId)
            if (remoteData != null) {
                val (entries, config) = remoteData
                if (config != null) {
                    repository.saveConfig(config)
                }
                database.timeEntryDao().clearAllForUser(userId)
                for (entry in entries) {
                    repository.insertOrUpdate(entry)
                }
            }
            syncAttendanceLogsFromFirestore(userId)
            _cloudSyncStatus.value = "Đã khôi phục đám mây"
            _triggerRefresh.value += 1
        } catch (e: Exception) {
            _cloudSyncStatus.value = "Lỗi khôi phục đám mây"
            android.util.Log.e("TimeSnapViewModel", "Failed suspended server restore", e)
        }
    }

    private suspend fun syncAttendanceLogsFromFirestore(userId: String) {
        if (userId.startsWith("demo") || userId.contains("demo")) return
        try {
            android.util.Log.d("TimeSnapViewModel", "Syncing attendance logs from Firestore for $userId")
            val remoteRecords = com.example.data.FirestoreService.getAttendanceLogsForUser(userId)
            if (remoteRecords.isEmpty()) return

            val currentEntries = repository.getEntries(userId).first().toMutableList()
            val config = repository.getConfigDirect(userId) ?: UserConfig(userId = userId)

            // Group remote records by normalized date and merge duplicates
            val remoteRecordsGrouped = remoteRecords.groupBy { log ->
                var formattedDate = com.example.data.SalaryCalculator.normalizeDateToDmy(log.dateString)
                if (formattedDate.isBlank() && log.clockInTime > 0) {
                    formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(log.clockInTime))
                }
                formattedDate
            }

            val mergedRemoteRecords = remoteRecordsGrouped.mapNotNull { (formattedDate, logs) ->
                if (formattedDate.isBlank()) return@mapNotNull null
                if (logs.size == 1) {
                    logs.first().copy(dateString = formattedDate)
                } else {
                    val validClockIns = logs.map { it.clockInTime }.filter { it > 0 }
                    val earliestClockIn = if (validClockIns.isNotEmpty()) validClockIns.minOrNull() ?: 0L else 0L
                    
                    val validClockOuts = logs.mapNotNull { it.clockOutTime }.filter { it > 0 }
                    val latestClockOut = if (validClockOuts.isNotEmpty()) validClockOuts.maxOrNull() else null
                    
                    val nonNormalStatus = logs.map { it.status }.firstOrNull { 
                        val u = it.trim().uppercase()
                        u != "ACTIVE" && u != "NORMAL" && u.isNotEmpty() 
                    }
                    val finalStatus = nonNormalStatus ?: logs.first().status
                    
                    val combinedNotes = logs.map { it.notes }.filter { it.isNotBlank() }.distinct().joinToString("; ")
                    
                    logs.first().copy(
                        dateString = formattedDate,
                        clockInTime = earliestClockIn,
                        clockOutTime = latestClockOut,
                        status = finalStatus,
                        notes = combinedNotes
                    )
                }
            }

            val remoteDates = mergedRemoteRecords.map {
                com.example.data.SalaryCalculator.normalizeDateToDmy(it.dateString)
            }.filter { it.isNotBlank() }.toSet()

            // Find local entries that have no corresponding record in Firestore (meaning deleted by Admin)
            val toDeleteLocally = currentEntries.filter { entry ->
                val normDate = com.example.data.SalaryCalculator.normalizeDateToDmy(entry.date)
                normDate.isNotBlank() && !remoteDates.contains(normDate)
            }

            for (entryToDelete in toDeleteLocally) {
                android.util.Log.d("TimeSnapViewModel", "Local entry for ${entryToDelete.date} was deleted on Firestore. Deleting locally.")
                repository.delete(entryToDelete)
                
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val todayDmy = com.example.data.SalaryCalculator.normalizeDateToDmy(todayStr)
                val entryNormDate = com.example.data.SalaryCalculator.normalizeDateToDmy(entryToDelete.date)
                
                if (entryNormDate == todayDmy) {
                    android.util.Log.d("TimeSnapViewModel", "Cancelling automated processes for today because today's entry was deleted.")
                    com.example.notification.NotificationHelper.cancelAutoCheckIn(getApplication(), userId)
                    com.example.notification.NotificationHelper.cancelAutoCheckOut(getApplication(), userId)
                }
            }
            currentEntries.removeAll(toDeleteLocally)

            for (log in mergedRemoteRecords) {
                val formattedDate = log.dateString

                val existingEntry = currentEntries.find { entry ->
                    com.example.data.SalaryCalculator.normalizeDateToDmy(entry.date) == formattedDate || 
                    (entry.checkInTime != null && log.clockInTime > 0 && 
                     SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(entry.checkInTime)) == formattedDate)
                }

                val remoteClockOut = log.clockOutTime
                val remoteClockIn = log.clockInTime
                val remoteStatusUpper = log.status.trim().uppercase(Locale.ROOT)
                val mappedDayType = when {
                    remoteStatusUpper == "PAID_LEAVE" || remoteStatusUpper == "NP" || remoteStatusUpper == "PHEP" || 
                    remoteStatusUpper.contains("PHÉP") || remoteStatusUpper.contains("PHEP") -> "PAID_LEAVE"
                    
                    remoteStatusUpper == "UNPAID_LEAVE" || remoteStatusUpper.contains("KHÔNG LƯƠNG") || 
                    remoteStatusUpper.contains("KHONG LUONG") -> "UNPAID_LEAVE"
                    
                    remoteStatusUpper == "UNAUTHORIZED_LEAVE" || remoteStatusUpper == "KP" || 
                    remoteStatusUpper.contains("KHÔNG PHÉP") || remoteStatusUpper.contains("KHONG PHEP") || 
                    remoteStatusUpper.contains("KHONGPHEP") || remoteStatusUpper == "ABSENT" -> "UNAUTHORIZED_LEAVE"
                    
                    remoteStatusUpper == "HOLIDAY_LEAVE" || remoteStatusUpper.contains("LỄ") || 
                    remoteStatusUpper.contains("LE") -> "HOLIDAY_LEAVE"
                    
                    remoteStatusUpper == "SUNDAY" || remoteStatusUpper.contains("CHỦ NHẬT") || 
                    remoteStatusUpper.contains("CHU NHAT") -> "SUNDAY"
                    
                    else -> null
                }
                val isLeaveType = mappedDayType == "PAID_LEAVE" || mappedDayType == "UNPAID_LEAVE" || mappedDayType == "UNAUTHORIZED_LEAVE" || mappedDayType == "HOLIDAY_LEAVE"
                val remoteIsWorking = if (isLeaveType) false else ((remoteClockOut == null || remoteClockOut == 0L) && (remoteClockIn > 0L))

                if (existingEntry != null) {
                    val finalDayType = mappedDayType ?: existingEntry.dayType
                    val needsUpdate = (existingEntry.checkOutTime != remoteClockOut) ||
                                      (existingEntry.isWorking != remoteIsWorking) ||
                                      (existingEntry.dayType != finalDayType) ||
                                      (remoteClockIn > 0L && existingEntry.checkInTime != remoteClockIn)

                    if (needsUpdate) {
                        android.util.Log.d("TimeSnapViewModel", "Updating local TimeEntry for $formattedDate from Firestore (dayType: ${existingEntry.dayType} -> $finalDayType, isWorking: ${existingEntry.isWorking} -> $remoteIsWorking)")
                        val updated = existingEntry.copy(
                            checkInTime = if (remoteClockIn > 0L) remoteClockIn else existingEntry.checkInTime,
                            checkOutTime = if (isLeaveType) null else remoteClockOut,
                            isWorking = remoteIsWorking,
                            dayType = finalDayType,
                            note = if (log.notes.isNotBlank()) log.notes else existingEntry.note
                        )
                        val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(updated, config)
                        repository.insertOrUpdate(calculated)
                        val idx = currentEntries.indexOfFirst { it.id == existingEntry.id || it.date == existingEntry.date }
                        if (idx >= 0) {
                            currentEntries[idx] = calculated
                        }
                    }
                } else {
                    val cal = Calendar.getInstance()
                    var isSunday = false
                    try {
                        val parser = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                        val date = parser.parse(formattedDate)
                        if (date != null) {
                            cal.time = date
                            isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val dayType = mappedDayType ?: if (isSunday) "SUNDAY" else "NORMAL"
                    val newEntry = TimeEntry(
                        id = 0,
                        userId = userId,
                        date = formattedDate,
                        checkInTime = if (remoteClockIn > 0L) remoteClockIn else null,
                        checkOutTime = if (isLeaveType) null else remoteClockOut,
                        isWorking = remoteIsWorking,
                        dayType = dayType,
                        note = log.notes.takeIf { it.isNotBlank() }
                    )
                    val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(newEntry, config)
                    repository.insertOrUpdate(calculated)
                    // Reload currentEntries from the database to ensure the newly generated primary key ID is loaded correctly
                    currentEntries.clear()
                    currentEntries.addAll(repository.getEntries(userId).first())
                }
            }
            // Run local deduplication/normalization to clean up any old duplicates
            normalizeTimeEntryDates(userId)
            
            val active = repository.getActiveEntry(userId)
            _activeWorkingEntry.value = active
            _triggerRefresh.value += 1
        } catch (e: Exception) {
            android.util.Log.e("TimeSnapViewModel", "Error syncing attendance logs from Firestore for $userId", e)
        }
    }

    private suspend fun normalizeTimeEntryDates(userId: String) {
        try {
            val entries = repository.getEntries(userId).first()
            val config = repository.getConfigDirect(userId) ?: UserConfig(userId = userId)
            val grouped = entries.groupBy { com.example.data.SalaryCalculator.normalizeDateToDmy(it.date) }
            
            var updatedCount = 0
            var deletedCount = 0
            
            for ((normalizedDate, group) in grouped) {
                if (group.size > 1) {
                    // Sort to pick the best entry: 1. Both clockIn and clockOut, 2. Only clockIn, 3. Highest ID
                    val bestToKeep = group.maxWithOrNull(compareBy<TimeEntry> {
                        if (it.checkInTime != null && it.checkOutTime != null) 2 
                        else if (it.checkInTime != null) 1 
                        else 0
                    }.thenBy { it.id })!!
                    
                    for (entry in group) {
                        if (entry.id != bestToKeep.id) {
                            repository.delete(entry)
                            deletedCount++
                        }
                    }
                    
                    if (bestToKeep.date != normalizedDate) {
                        val updated = bestToKeep.copy(date = normalizedDate)
                        val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(updated, config)
                        repository.insertOrUpdate(calculated)
                        updatedCount++
                    }
                } else {
                    val entry = group.first()
                    if (entry.date != normalizedDate) {
                        val updated = entry.copy(date = normalizedDate)
                        val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(updated, config)
                        repository.insertOrUpdate(calculated)
                        updatedCount++
                    }
                }
            }
            if (deletedCount > 0 || updatedCount > 0) {
                android.util.Log.d("TimeSnapViewModel", "Deduplicated and normalized entries: deleted $deletedCount, updated $updatedCount")
            }
        } catch (e: Exception) {
            android.util.Log.e("TimeSnapViewModel", "Error normalizing TimeEntry dates", e)
        }
    }

    private suspend fun migrateLegacyData(userId: String) {
        if (userId.startsWith("demo") || userId.contains("demo")) return
        try {
            android.util.Log.d("TimeSnapViewModel", "Starting legacy data migration for $userId")
            
            // 1. Get existing new entries to avoid duplicates
            val currentEntries = repository.getEntries(userId).first()
            val existingDates = currentEntries.map { com.example.data.SalaryCalculator.normalizeDateToDmy(it.date) }.toSet()
            
            val prefs = getApplication<Application>().getSharedPreferences("timesnap_deleted_prefs", android.content.Context.MODE_PRIVATE)
            val deletedSet = prefs.getStringSet("deleted_legacy_dates_$userId", emptySet()) ?: emptySet()
            
            val legacyRecordsToMigrate = mutableListOf<com.example.data.AttendanceRecord>()

            // 2. Check and migrate local legacy database "timesnap_pro.db"
            val dbFile = getApplication<Application>().getDatabasePath("timesnap_pro.db")
            if (dbFile.exists()) {
                android.util.Log.d("TimeSnapViewModel", "Local legacy database timesnap_pro.db found. Reading local records...")
                try {
                    com.example.data.DatabaseHelper.init(getApplication())
                    val localRecords = com.example.data.DatabaseHelper.instance.getRecords(userId).first()
                    android.util.Log.d("TimeSnapViewModel", "Found ${localRecords.size} local legacy records.")
                    legacyRecordsToMigrate.addAll(localRecords)
                } catch (e: Exception) {
                    android.util.Log.e("TimeSnapViewModel", "Error reading local legacy records", e)
                }
            } else {
                android.util.Log.d("TimeSnapViewModel", "No local legacy database timesnap_pro.db found.")
            }

            // 3. Check and migrate remote legacy records from Firestore "attendance_logs"
            try {
                android.util.Log.d("TimeSnapViewModel", "Checking for remote legacy records on Firestore...")
                val remoteRecords = com.example.data.FirestoreService.getAttendanceLogsForUser(userId)
                android.util.Log.d("TimeSnapViewModel", "Found ${remoteRecords.size} remote legacy records.")
                // Add those that aren't already in the local migration list (by date)
                for (remoteRecord in remoteRecords) {
                    if (legacyRecordsToMigrate.none { it.dateString == remoteRecord.dateString }) {
                        legacyRecordsToMigrate.add(remoteRecord)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Error reading remote legacy records from Firestore", e)
            }

            // 4. Perform the migration
            if (legacyRecordsToMigrate.isNotEmpty()) {
                android.util.Log.d("TimeSnapViewModel", "Total unique legacy records to migrate: ${legacyRecordsToMigrate.size}")
                var migratedCount = 0
                for (log in legacyRecordsToMigrate) {
                    val formattedDate = com.example.data.SalaryCalculator.normalizeDateToDmy(log.dateString)

                    // Only migrate if we don't already have an entry for this formattedDate and it was not previously deleted
                    if (!existingDates.contains(formattedDate) && !deletedSet.contains(formattedDate) && !deletedSet.contains(log.dateString)) {
                        val cal = Calendar.getInstance()
                        var isSunday = false
                        try {
                            val parser = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                            val date = parser.parse(formattedDate)
                            if (date != null) {
                                cal.time = date
                                isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        val dayType = if (isSunday) "SUNDAY" else "NORMAL"

                        val entry = TimeEntry(
                            id = 0,
                            userId = userId,
                            date = formattedDate,
                            checkInTime = log.clockInTime,
                            checkOutTime = log.clockOutTime,
                            isWorking = (log.clockOutTime == null),
                            dayType = dayType,
                            note = log.notes.takeIf { it.isNotBlank() }
                        )

                        // Calculate metrics using SalaryCalculator
                        val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(entry, userConfig.value)
                        repository.insertOrUpdate(calculated)
                        migratedCount++
                    }
                }

                android.util.Log.d("TimeSnapViewModel", "Successfully migrated $migratedCount legacy records to time_entries database for $userId")
                if (migratedCount > 0) {
                    _triggerRefresh.value += 1
                }
            } else {
                android.util.Log.d("TimeSnapViewModel", "No legacy records to migrate.")
            }
        } catch (e: Exception) {
            android.util.Log.e("TimeSnapViewModel", "Failed to execute legacy data migration", e)
        }
    }

    fun checkAndFetchSalaryConfigFromFirestore(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbConfig = com.example.data.FirestoreService.fetchUserSalaryConfigFromFirestore(userId)
                if (dbConfig != null) {
                    android.util.Log.d("TimeSnapViewModel", "Found user salary config on Firestore for $userId. Applying locally.")
                    repository.saveConfig(dbConfig)
                } else {
                    android.util.Log.d("TimeSnapViewModel", "No user salary config found on Firestore for $userId.")
                }
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Failed to restore salary settings from firestore: ${e.message}")
            }
        }
    }

    private suspend fun migrateGuestDataToUser(newUserId: String) {
        if (newUserId == "local_user" || newUserId.startsWith("local")) return
        try {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("timesnap_auth", android.content.Context.MODE_PRIVATE)
            val pendingGuestUid = sharedPrefs.getString("pending_migration_guest_uid", null)
            val lastUid = pendingGuestUid ?: sharedPrefs.getString("last_logged_in_uid", "local_user") ?: "local_user"
            
            if (lastUid != newUserId && lastUid.startsWith("local")) {
                val guestEntries = repository.getEntries(lastUid).first()
                if (guestEntries.isNotEmpty()) {
                    val userEntries = repository.getEntries(newUserId).first()
                    if (userEntries.isEmpty()) {
                        android.util.Log.d("TimeSnapViewModel", "Migrating ${guestEntries.size} entries from local UID $lastUid to $newUserId")
                        for (entry in guestEntries) {
                            repository.insertOrUpdate(entry.copy(userId = newUserId))
                        }
                        val guestConfig = repository.getConfigDirect(lastUid)
                        if (guestConfig != null) {
                            repository.saveConfig(guestConfig.copy(userId = newUserId))
                        }
                        // Clear guest entries so we don't migrate again
                        database.timeEntryDao().clearAllForUser(lastUid)
                    }
                }
            }
            if (pendingGuestUid != null) {
                sharedPrefs.edit().remove("pending_migration_guest_uid").apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("TimeSnapViewModel", "Failed to migrate guest data: ${e.message}")
        }
    }

    // Direct save matching realtime changes
    fun updateSalaryConfig(config: UserConfig) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = config.copy(userId = session.uid)
            repository.saveConfig(updated)
            try {
                com.example.data.FirestoreService.saveUserSalaryConfigToFirestore(updated)
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Failed to sync to Firestore users_salary list: ${e.message}")
            }
            triggerSyncDebounced()
        }
    }

    // Update Salary Settings
    fun updateSalarySettings(
        luongCoBan: Double,
        tienCom: Double,
        phuCap: Double,
        phuCapXangXe: Double,
        phuCapDienThoai: Double,
        phuCapNhaO: Double,
        phuCapChuyenCan: Double,
        thuong: Double,
        heSoOtDem: Double
    ) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigDirect(session.uid) ?: UserConfig(userId = session.uid)
            val updated = current.copy(
                luongCoBan = luongCoBan,
                tienComMoiNgay = tienCom,
                phuCap = phuCap,
                phuCapXangXe = phuCapXangXe,
                phuCapDienThoai = phuCapDienThoai,
                phuCapNhaO = phuCapNhaO,
                phuCapChuyenCan = phuCapChuyenCan,
                thuong = thuong,
                heSoOtDem = heSoOtDem
            )
            repository.saveConfig(updated)
            try {
                com.example.data.FirestoreService.saveUserSalaryConfigToFirestore(updated)
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Failed to sync to Firestore users_salary list: ${e.message}")
            }
            triggerSync()
        }
    }

    // ADD SINGLE ENTRY (CHẾ ĐỘ 1)
    fun addSingleEntry(
        dateStr: String,
        checkInHour: Int,
        checkInMin: Int,
        checkOutHour: Int?,
        checkOutMin: Int?,
        dayTypeOverride: String? = null,
        noteStr: String? = null,
        customBreakDeduction: Boolean? = null
    ) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val todayDate = Date()
            val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val todayStr = parser.format(todayDate)
            
            var isFuture = false
            try {
                val parsedDate = parser.parse(dateStr)
                if (parsedDate != null) {
                    val cal1 = Calendar.getInstance().apply { time = todayDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                    val cal2 = Calendar.getInstance().apply { time = parsedDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                    isFuture = cal2.after(cal1)
                }
            } catch (e: Exception) {}

            // Check if day is Sunday or Holiday
            var dayType = "NORMAL"
            try {
                val dateVal = parser.parse(dateStr)
                if (dateVal != null) {
                    val cal = Calendar.getInstance()
                    cal.time = dateVal
                    val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    if (isSunday) {
                        dayType = "SUNDAY"
                    } else if (isHolidayDate(dateStr)) {
                        dayType = "HOLIDAY"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Apply override if selected
            if (!dayTypeOverride.isNullOrEmpty()) {
                dayType = dayTypeOverride
            }

            // Auto detect night shift if checkInHour >= 18
            val isAutoNightShift = (checkInHour >= 18)
            if (isAutoNightShift && dayType == "NORMAL") {
                dayType = "NIGHT"
            }

            // ENFORCE RULE: Future dates cannot be normal working days. Only leaves allowed.
            if (isFuture) {
                if (dayType != "PAID_LEAVE" && dayType != "UNPAID_LEAVE" && dayType != "UNAUTHORIZED_LEAVE" && dayType != "HOLIDAY_LEAVE") {
                    val config = repository.getConfigDirect(session.uid)
                    val phepConLai = config?.phepNamConLai ?: 0
                    dayType = if (phepConLai > 0) "PAID_LEAVE" else "UNPAID_LEAVE"
                }
            }

            val baseCal = Calendar.getInstance()
            val parts = if (dateStr.contains("/")) dateStr.split("/") else dateStr.split("-")
            val yr = if (dateStr.contains("/")) parts[2].toInt() else parts[0].toInt()
            val mo = parts[1].toInt() - 1
            val dy = if (dateStr.contains("/")) parts[0].toInt() else parts[2].toInt()

            baseCal.set(yr, mo, dy, checkInHour, checkInMin, 0)
            val checkInMs = baseCal.timeInMillis

            val checkOutMs = if (checkOutHour != null && checkOutMin != null) {
                val outCal = Calendar.getInstance()
                outCal.set(yr, mo, dy, checkOutHour, checkOutMin, 0)
                if (outCal.timeInMillis < checkInMs) {
                    outCal.add(Calendar.DAY_OF_MONTH, 1)
                }
                outCal.timeInMillis
            } else {
                null
            }

            val isWorking = (checkOutMs == null && dayType != "PAID_LEAVE" && dayType != "UNPAID_LEAVE" && dayType != "UNAUTHORIZED_LEAVE" && dayType != "HOLIDAY_LEAVE")

            // Ensure no duplicate active shifts across dates
            val priorActive = repository.getActiveEntry(session.uid)
            if (priorActive != null && priorActive.date != dateStr && isWorking) {
                val closedPrior = priorActive.copy(
                    checkOutTime = priorActive.checkOutTime ?: System.currentTimeMillis(),
                    isWorking = false
                )
                val calculatedPrior = com.example.data.SalaryCalculator.calculateSingleEntry(closedPrior, userConfig.value)
                repository.insertOrUpdate(calculatedPrior)
            }

            val existing = repository.getEntryByDate(session.uid, dateStr)
            
            // Re-evaluate annual leave delta
            val config = repository.getConfigDirect(session.uid)
            var finalDayType = dayType
            
            if (config != null) {
                var phepNamDelta = 0
                if (existing?.dayType == "PAID_LEAVE") {
                    phepNamDelta += 1
                }
                if (finalDayType == "PAID_LEAVE") {
                    val currentRemaining = config.phepNamConLai + phepNamDelta
                    if (currentRemaining <= 0) {
                        // Out of leave quota, coerce to unpaid leave
                        finalDayType = "UNPAID_LEAVE"
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(
                                getApplication(),
                                "⚠️ Quỹ phép năm của bạn đã HẾT (0 ngày)! Đã tự động chuyển sang Phép thường (Nghỉ không hưởng lương).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        // Valid leave, consume 1
                        phepNamDelta -= 1
                    }
                }
                if (phepNamDelta != 0) {
                    val updatedConfig = config.copy(
                        phepNamConLai = (config.phepNamConLai + phepNamDelta).coerceAtLeast(0)
                    )
                    repository.saveConfig(updatedConfig)
                }
            }

            val isLeave = finalDayType == "PAID_LEAVE" || finalDayType == "UNPAID_LEAVE" || finalDayType == "UNAUTHORIZED_LEAVE" || finalDayType == "HOLIDAY_LEAVE"
            val sId = if (isLeave) null else if (isAutoNightShift) "ca_dem" else if (checkOutHour != null && checkOutHour >= 20) "ca2" else "ca1"
            val sType = if (sId == "ca_dem") "NIGHT" else if (sId == "ca2") "DAY_REST" else if (sId == "ca1") "DAY" else null

            val newEntry = TimeEntry(
                id = existing?.id ?: 0,
                userId = session.uid,
                date = dateStr,
                checkInTime = if (isLeave) null else checkInMs,
                checkOutTime = if (isLeave) null else checkOutMs,
                isWorking = isWorking,
                dayType = finalDayType,
                note = noteStr,
                shiftId = sId,
                shiftType = sType,
                customBreakDeduction = customBreakDeduction
            )

            val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(newEntry, userConfig.value)
            repository.insertOrUpdate(calculated)
            syncTimeEntryToLegacyLog(calculated)
            triggerSync()

            if (isLeave) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val todayDmy = com.example.data.SalaryCalculator.normalizeDateToDmy(todayStr)
                val entryNormDate = com.example.data.SalaryCalculator.normalizeDateToDmy(dateStr)
                if (entryNormDate == todayDmy) {
                    com.example.notification.NotificationHelper.cancelAutoCheckIn(getApplication(), session.uid)
                    com.example.notification.NotificationHelper.cancelAutoCheckOut(getApplication(), session.uid)
                }
                com.example.notification.NotificationHelper.scheduleNextCheckInReminder(getApplication(), session.uid)
            }
        }
    }

    // BULK ENTRY (CHẾ ĐỘ 2)
    fun addBulkEntries(
        selectedDates: List<String>,
        checkInHour: Int,
        checkInMin: Int,
        checkOutHour: Int,
        checkOutMin: Int,
        skipSunday: Boolean,
        skipHoliday: Boolean,
        autoRecognizeOt: Boolean,
        isNightShiftOverride: Boolean = false
    ) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val todayDate = Date()
            val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val todayStr = parser.format(todayDate)
            
            for (dateStr in selectedDates) {
                var isFuture = false
                try {
                    val parsedDate = parser.parse(dateStr)
                    if (parsedDate != null) {
                        val cal1 = Calendar.getInstance().apply { time = todayDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        val cal2 = Calendar.getInstance().apply { time = parsedDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        isFuture = cal2.after(cal1)
                    }
                } catch (e: Exception) {}
                
                if (isFuture) continue

                var dateVal: Date? = null
                var isSunday = false
                var isHoliday = false
                try {
                    dateVal = parser.parse(dateStr)
                    if (dateVal != null) {
                        val cal = Calendar.getInstance()
                        cal.time = dateVal
                        isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        isHoliday = isHolidayDate(dateStr)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (skipSunday && isSunday) continue
                if (skipHoliday && isHoliday) continue

                val isAutoNightShift = (checkInHour >= 18)
                val dayType = when {
                    isAutoNightShift -> "NIGHT"
                    autoRecognizeOt && isHoliday -> "HOLIDAY"
                    autoRecognizeOt && isSunday -> "SUNDAY"
                    else -> "NORMAL"
                }

                val parts = if (dateStr.contains("/")) dateStr.split("/") else dateStr.split("-")
                val yr = if (dateStr.contains("/")) parts[2].toInt() else parts[0].toInt()
                val mo = parts[1].toInt() - 1
                val dy = if (dateStr.contains("/")) parts[0].toInt() else parts[2].toInt()

                val inCal = Calendar.getInstance()
                inCal.set(yr, mo, dy, checkInHour, checkInMin, 0)
                val checkInMs = inCal.timeInMillis

                val outCal = Calendar.getInstance()
                outCal.set(yr, mo, dy, checkOutHour, checkOutMin, 0)
                if (outCal.timeInMillis < checkInMs) {
                    outCal.add(Calendar.DAY_OF_MONTH, 1)
                }
                val checkOutMs = outCal.timeInMillis

                val existing = repository.getEntryByDate(session.uid, dateStr)
                
                // If existing was PAID_LEAVE and is being overwritten by work, restore 1 leave
                if (existing?.dayType == "PAID_LEAVE") {
                    val config = repository.getConfigDirect(session.uid)
                    if (config != null) {
                        repository.saveConfig(config.copy(phepNamConLai = config.phepNamConLai + 1))
                    }
                }

                val sId = if (isNightShiftOverride || isAutoNightShift) "ca_dem" else if (checkOutHour >= 20) "ca2" else "ca1"
                val sType = if (sId == "ca_dem") "NIGHT" else if (sId == "ca2") "DAY_REST" else "DAY"

                val entry = TimeEntry(
                    id = existing?.id ?: 0,
                    userId = session.uid,
                    date = dateStr,
                    checkInTime = checkInMs,
                    checkOutTime = checkOutMs,
                    isWorking = false,
                    dayType = dayType,
                    shiftId = sId,
                    shiftType = sType
                )
                val calculated = com.example.data.SalaryCalculator.calculateSingleEntry(entry, userConfig.value)
                repository.insertOrUpdate(calculated)
                syncTimeEntryToLegacyLog(calculated)
            }
            triggerSync()
        }
    }

    private suspend fun recordDeletedDate(uid: String, dateStr: String) {
        try {
            com.example.data.DatabaseHelper.init(getApplication())
            com.example.data.DatabaseHelper.instance.deleteAttendanceRecord(uid, dateStr)
            
            val prefs = getApplication<Application>().getSharedPreferences("timesnap_deleted_prefs", android.content.Context.MODE_PRIVATE)
            val setKey = "deleted_legacy_dates_$uid"
            val existingSet = prefs.getStringSet(setKey, emptySet()) ?: emptySet()
            val updatedSet = existingSet.toMutableSet().apply {
                add(dateStr)
                add(com.example.data.SalaryCalculator.normalizeDateToDmy(dateStr))
            }
            prefs.edit().putStringSet(setKey, updatedSet).apply()
        } catch (e: Exception) {
            android.util.Log.e("TimeSnapViewModel", "Failed to record deleted date: ${e.message}")
        }
    }

    fun deleteEntry(entry: TimeEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entry)
            deleteTimeEntryFromLegacyLog(entry)
            recordDeletedDate(entry.userId, entry.date)
            
            // Revert leave quota if deleted entry was PAID_LEAVE
            if (entry.dayType == "PAID_LEAVE") {
                val session = currentUserSession.value
                if (session != null) {
                    val config = repository.getConfigDirect(session.uid)
                    if (config != null) {
                        repository.saveConfig(config.copy(phepNamConLai = config.phepNamConLai + 1))
                    }
                }
            }
            triggerSync()
        }
    }

    fun deleteBulkEntries(selectedDatesList: List<String>) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val entriesToDelete = mutableListOf<TimeEntry>()
            var restoredLeaves = 0
            for (dateStr in selectedDatesList) {
                val existing = repository.getEntryByDate(session.uid, dateStr)
                if (existing != null) {
                    entriesToDelete.add(existing)
                    if (existing.dayType == "PAID_LEAVE") {
                        restoredLeaves++
                    }
                }
            }
            if (entriesToDelete.isEmpty()) return@launch

            // 1. Delete from local database IMMEDIATELY so UI updates instantly
            for (entry in entriesToDelete) {
                repository.delete(entry)
                recordDeletedDate(session.uid, entry.date)
            }

            if (restoredLeaves > 0) {
                val config = repository.getConfigDirect(session.uid)
                if (config != null) {
                    repository.saveConfig(config.copy(phepNamConLai = config.phepNamConLai + restoredLeaves))
                }
            }

            // 2. Perform Firestore deletions in PARALLEL in the background without blocking sequentially
            kotlinx.coroutines.coroutineScope {
                for (entry in entriesToDelete) {
                    launch {
                        deleteTimeEntryFromLegacyLog(entry)
                    }
                }
            }
            triggerSync()
        }
    }

    fun clearAllEntriesInSelectedMonth() {
        val session = currentUserSession.value ?: return
        val month = currentSelectedMonth.value
        viewModelScope.launch(Dispatchers.IO) {
            val parts = month.split("-")
            val monthPattern = if (parts.size == 2) "%/${parts[1]}/${parts[0]}" else "%"
            val altMonthPattern = if (parts.size == 2) "${parts[0]}-${parts[1]}-%" else "%"
            val allEntries = repository.getEntriesInMonthDirect(session.uid, monthPattern, altMonthPattern)
            val phepToRestore = allEntries.count { it.dayType == "PAID_LEAVE" }
            
            if (allEntries.isEmpty()) return@launch

            // 1. Delete from local database IMMEDIATELY so UI updates instantly
            repository.deleteEntriesInMonth(session.uid, monthPattern, altMonthPattern)
            for (entry in allEntries) {
                recordDeletedDate(session.uid, entry.date)
            }
            
            if (phepToRestore > 0) {
                val config = repository.getConfigDirect(session.uid)
                if (config != null) {
                    repository.saveConfig(config.copy(phepNamConLai = config.phepNamConLai + phepToRestore))
                }
            }
            
            // 2. Perform Firestore deletions in PARALLEL in the background without blocking sequentially
            kotlinx.coroutines.coroutineScope {
                for (entry in allEntries) {
                    launch {
                        deleteTimeEntryFromLegacyLog(entry)
                    }
                }
            }
            triggerSync()
        }
    }

    private fun isHolidayDate(dateStr: String): Boolean {
        return com.example.data.SalaryCalculator.isHoliday(dateStr)
    }

    private fun roundCheckInGrace(timeMs: Long): Long {
        return com.example.data.SalaryCalculator.getRoundedTime(timeMs, true)
    }

    private fun roundCheckOutGrace(timeMs: Long): Long {
        return com.example.data.SalaryCalculator.getRoundedTime(timeMs, false)
    }

    private fun isSundayDate(dateStr: String): Boolean {
        return try {
            val parser = if (dateStr.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            }
            val dateVal = parser.parse(dateStr)
            if (dateVal != null) {
                val cal = Calendar.getInstance()
                cal.time = dateVal
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateSalarySummary(entries: List<TimeEntry>, config: UserConfig, firstEntryDate: String? = null): SalarySummary {
        return calculateSalarySummaryForMonth(currentSelectedMonth.value, entries, config, firstEntryDate)
    }

    private fun calculateSalarySummaryForMonth(
        selectedMonth: String,
        entries: List<TimeEntry>,
        config: UserConfig,
        firstEntryDate: String? = null
    ): SalarySummary {
        var targetYear = 2026
        var targetMonth = 5
        try {
            val parts = selectedMonth.split("-")
            targetYear = parts[0].toInt()
            targetMonth = parts[1].toInt()
        } catch (e: Exception) {}

        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

        val isCurrentSelectedMonth = (targetYear == currentYear && targetMonth == currentMonth)
        val todayStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, todayDayOfMonth)

        val maxDaysInMo = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Find all public holidays in the selected month
        val holidayDatesInMonth = mutableSetOf<String>()
        for (day in 1..maxDaysInMo) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            if (isHolidayDate(dateStr)) {
                holidayDatesInMonth.add(dateStr)
            }
        }

        val effectiveJoinDate: String? = if (config.ngayVaoLam.isNotBlank()) {
            config.ngayVaoLam.trim()
        } else if (firstEntryDate != null && firstEntryDate.startsWith(selectedMonth)) {
            firstEntryDate
        } else {
            null
        }

        var expectedWorkDaysSoFar = 0
        var totalWorkDaysInMonth = 0

        // Calculate total work days in month first
        for (day in 1..maxDaysInMo) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            val cal = Calendar.getInstance()
            cal.set(targetYear, targetMonth - 1, day)
            val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            val isHoliday = isHolidayDate(dateStr)
            if (!isSunday && !isHoliday) {
                totalWorkDaysInMonth++
            }
        }

        if (isCurrentSelectedMonth) {
            for (day in 1..todayDayOfMonth) {
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day)
                if (effectiveJoinDate != null && dateStr < effectiveJoinDate) {
                    continue
                }
                val cal = Calendar.getInstance()
                cal.set(currentYear, currentMonth - 1, day)
                val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                val isHoliday = isHolidayDate(dateStr)
                if (!isSunday && !isHoliday) {
                    expectedWorkDaysSoFar++
                }
            }
        } else {
            if (effectiveJoinDate != null && effectiveJoinDate.startsWith(selectedMonth)) {
                for (day in 1..maxDaysInMo) {
                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                    if (dateStr < effectiveJoinDate) {
                        continue
                    }
                    val cal = Calendar.getInstance()
                    cal.set(targetYear, targetMonth - 1, day)
                    val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    val isHoliday = isHolidayDate(dateStr)
                    if (!isSunday && !isHoliday) {
                        expectedWorkDaysSoFar++
                    }
                }
            } else {
                expectedWorkDaysSoFar = totalWorkDaysInMonth
            }
        }

        return com.example.data.SalaryCalculator.calculateMonthlySalary(
            entries = entries,
            config = config,
            scheduledDaysSoFar = expectedWorkDaysSoFar,
            totalScheduledDaysInMonth = totalWorkDaysInMonth,
            earliestDate = effectiveJoinDate,
            selectedMonth = selectedMonth,
            todayStr = todayStr,
            isCurrentSelectedMonth = isCurrentSelectedMonth,
            holidayDatesInMonth = holidayDatesInMonth
        )
    }

    // Notification Center Management & Persistent Local Caching
    private val _adminNotifications = MutableStateFlow<List<com.example.data.model.AdminNotification>>(emptyList())
    val adminNotifications: StateFlow<List<com.example.data.model.AdminNotification>> = _adminNotifications.asStateFlow()

    private val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()

    private val _deletedNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedNotificationIds: StateFlow<Set<String>> = _deletedNotificationIds.asStateFlow()

    private val _isRefreshingNotifications = MutableStateFlow(false)
    val isRefreshingNotifications: StateFlow<Boolean> = _isRefreshingNotifications.asStateFlow()

    val unreadNotificationCount: StateFlow<Int> = combine(_adminNotifications, _readNotificationIds) { list, readSet ->
        list.count { it.id !in readSet }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun getNotifPrefs() = getApplication<Application>().getSharedPreferences("admin_notif_prefs_v2", Context.MODE_PRIVATE)

    private fun loadLocalCachedNotifications(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = getNotifPrefs()
            val readSet = prefs.getStringSet("read_ids_$uid", emptySet()) ?: emptySet()
            _readNotificationIds.value = readSet

            val deletedSet = prefs.getStringSet("deleted_ids_$uid", emptySet()) ?: emptySet()
            _deletedNotificationIds.value = deletedSet

            val cachedJson = prefs.getString("cached_notifs_$uid", "") ?: ""
            if (cachedJson.isNotBlank()) {
                try {
                    val array = org.json.JSONArray(cachedJson)
                    val list = mutableListOf<com.example.data.model.AdminNotification>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.optString("id")
                        val isPinned = obj.optBoolean("isPinned", false)
                        if (isPinned || id !in deletedSet) {
                            list.add(
                                com.example.data.model.AdminNotification(
                                    id = id,
                                    targetUid = obj.optString("targetUid", "ALL"),
                                    targetName = obj.optString("targetName", "Tất cả"),
                                    title = obj.optString("title"),
                                    message = obj.optString("message"),
                                    type = obj.optString("type", "GENERAL"),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                    sentBy = obj.optString("sentBy", "Admin"),
                                    isPinned = isPinned
                                )
                            )
                        }
                    }
                    _adminNotifications.value = list.sortedWith(
                        compareByDescending<com.example.data.model.AdminNotification> { it.isPinned }
                            .thenByDescending { it.createdAt }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("TimeSnapViewModel", "Lỗi đọc cache thông báo local", e)
                }
            }
        }
    }

    private fun saveLocalCachedNotifications(uid: String, list: List<com.example.data.model.AdminNotification>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = getNotifPrefs()
                val array = org.json.JSONArray()
                list.forEach { notif ->
                    val obj = org.json.JSONObject()
                    obj.put("id", notif.id)
                    obj.put("targetUid", notif.targetUid)
                    obj.put("targetName", notif.targetName)
                    obj.put("title", notif.title)
                    obj.put("message", notif.message)
                    obj.put("type", notif.type)
                    obj.put("createdAt", notif.createdAt)
                    obj.put("sentBy", notif.sentBy)
                    obj.put("isPinned", notif.isPinned)
                    array.put(obj)
                }
                prefs.edit().putString("cached_notifs_$uid", array.toString()).apply()
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Lỗi lưu cache thông báo local", e)
            }
        }
    }

    fun fetchAdminNotifications() {
        val uid = currentUserSession.value?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingNotifications.value = true
            try {
                // Tải danh sách ID đã xóa từ Cloud của user này để đồng bộ đa thiết bị
                val remoteDeleted = com.example.data.FirestoreService.getUserDeletedNotificationIds(uid)
                val allDeleted = _deletedNotificationIds.value + remoteDeleted
                _deletedNotificationIds.value = allDeleted
                getNotifPrefs().edit().putStringSet("deleted_ids_$uid", allDeleted).apply()

                val serverNotifs = com.example.data.FirestoreService.getUnreadAdminNotifications(uid, 0L)
                
                val currentLocal = _adminNotifications.value.filter { it.id !in allDeleted }
                val notifMap = mutableMapOf<String, com.example.data.model.AdminNotification>()
                currentLocal.forEach { notifMap[it.id] = it }
                serverNotifs.filter { it.id !in allDeleted }.forEach { notifMap[it.id] = it }

                // Pinned thông báo luôn ở đầu danh sách, sau đó đến mới nhất
                val mergedList = notifMap.values.sortedWith(
                    compareByDescending<com.example.data.model.AdminNotification> { it.isPinned }
                        .thenByDescending { it.createdAt }
                )
                _adminNotifications.value = mergedList
                saveLocalCachedNotifications(uid, mergedList)

                // Phát sinh thông báo hệ thống nếu có thông báo mới tinh
                val context = getApplication<Application>()
                val sharedPrefs = com.example.notification.NotificationHelper.getPrefs(context, uid)
                val lastCheckTime = sharedPrefs.getLong("admin_notif_last_check_${uid}", 0L)

                var newestTimestamp = lastCheckTime
                var hasNew = false
                for (notif in mergedList) {
                    if (notif.createdAt > lastCheckTime) {
                        val notifId = (notif.createdAt % 1000000).toInt() + 20000
                        com.example.notification.NotificationHelper.showNotification(
                            context = context,
                            title = if (notif.title.isNotBlank()) notif.title else "📢 Thông báo từ Admin",
                            message = notif.message,
                            notificationId = notifId
                        )
                        if (notif.createdAt > newestTimestamp) {
                            newestTimestamp = notif.createdAt
                        }
                        hasNew = true
                    }
                }
                if (hasNew) {
                    sharedPrefs.edit().putLong("admin_notif_last_check_${uid}", newestTimestamp).apply()
                }
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Lỗi tải thông báo từ server", e)
            } finally {
                _isRefreshingNotifications.value = false
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        val uid = currentUserSession.value?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _readNotificationIds.value.toMutableSet()
            updated.add(id)
            _readNotificationIds.value = updated
            getNotifPrefs().edit().putStringSet("read_ids_$uid", updated).apply()
        }
    }

    fun markAllNotificationsAsRead() {
        val uid = currentUserSession.value?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val allIds = _adminNotifications.value.map { it.id }.toSet()
            _readNotificationIds.value = allIds
            getNotifPrefs().edit().putStringSet("read_ids_$uid", allIds).apply()
        }
    }

    fun deleteNotificationLocally(id: String) {
        val uid = currentUserSession.value?.uid ?: return
        // Không cho phép xóa thông báo nếu được ghim bởi Admin
        val targetNotif = _adminNotifications.value.firstOrNull { it.id == id }
        if (targetNotif?.isPinned == true) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val updatedDeleted = _deletedNotificationIds.value.toMutableSet()
            updatedDeleted.add(id)
            _deletedNotificationIds.value = updatedDeleted
            getNotifPrefs().edit().putStringSet("deleted_ids_$uid", updatedDeleted).apply()

            val newList = _adminNotifications.value.filter { it.id != id }
            _adminNotifications.value = newList
            saveLocalCachedNotifications(uid, newList)

            try {
                // Lưu vĩnh viễn trạng thái đã xóa cho user này trên Firestore mà không xóa mất thông báo của user mới khác
                com.example.data.FirestoreService.recordUserDeletedNotification(uid, id)
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Lỗi ghi nhận user xóa thông báo", e)
            }
        }
    }

    fun deleteNotificationAsAdmin(id: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = com.example.data.FirestoreService.deleteAdminNotification("", id)
                if (success) {
                    val uid = currentUserSession.value?.uid ?: ""
                    val updatedDeleted = _deletedNotificationIds.value.toMutableSet()
                    updatedDeleted.add(id)
                    _deletedNotificationIds.value = updatedDeleted
                    if (uid.isNotBlank()) {
                        getNotifPrefs().edit().putStringSet("deleted_ids_$uid", updatedDeleted).apply()
                    }
                    val newList = _adminNotifications.value.filter { it.id != id }
                    _adminNotifications.value = newList
                    if (uid.isNotBlank()) {
                        saveLocalCachedNotifications(uid, newList)
                    }
                }
                withContext(Dispatchers.Main) {
                    onComplete(success)
                }
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Lỗi admin xóa thông báo toàn cục", e)
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

}

// Monthly Salary Point for comparison chart
data class MonthlySalaryPoint(
    val monthStr: String,
    val luongThucNhan: Double,
    val workingDays: Double,
    val luongDuKien: Double
)

// Data wrapper for salary analysis
data class SalarySummary(
    val workingDays: Double,
    val standardHours: Double,
    val otDayHours: Double,
    val otNightHours: Double,
    val tienOtNgay: Double,
    val tienOtDem: Double,
    val tongTienCom: Double,
    val phuCap: Double,
    val phuCapXangXe: Double = 0.0,
    val phuCapDienThoai: Double = 0.0,
    val phuCapNhaO: Double = 0.0,
    val phuCapChuyenCan: Double = 0.0,
    val thuong: Double,
    val tienBh: Double,
    val doanPhi: Double,
    val tienKhauTruNghi: Double,
    val luongThucNhan: Double,
    val baseBasicSalary: Double = 0.0,
    val expectedWorkDays: Int = 26,
    val standardWorkDays: Int = 26,
    val scheduledDaysSoFar: Int = 0,
    val isCurrentMonth: Boolean = false,
    
    val pcKyThuatVal: Double = 0.0,
    val pcTrachNhiemVal: Double = 0.0,
    val pcChucVuVal: Double = 0.0,
    val pcHieuSuatVal: Double = 0.0,
    val pcSanPhamVal: Double = 0.0,
    val pcComCaVal: Double = 0.0,
    val pcComOtVal: Double = 0.0,
    val pcNhaOVal: Double = 0.0,
    val pcDocHaiVal: Double = 0.0,
    val pcDtDoanhThuVal: Double = 0.0,
    val pcXangXeVal: Double = 0.0,
    val pcThamNienVal: Double = 0.0,
    val pcKhac1Val: Double = 0.0,
    val pcKhacVal: Double = 0.0,
    val pcCaDemVal: Double = 0.0,
    val caDemCount: Int = 0,
    val tienChuNhat: Double = 0.0,
    val tienChuNhatNgay: Double = 0.0,
    val tienChuNhatDem: Double = 0.0,
    val chuNhatHours: Double = 0.0,
    val chuNhatDayHours: Double = 0.0,
    val chuNhatNightHours: Double = 0.0,
    val otLeHours: Double = 0.0,
    val tienOtLe: Double = 0.0
)
