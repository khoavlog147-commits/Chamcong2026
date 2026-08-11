package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper private constructor(private val database: LegacyAppDatabase) {

    private val userConfigDao = database.userConfigDao()
    private val attendanceDao = database.attendanceDao()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        val db = Room.databaseBuilder(
                            context.applicationContext,
                            LegacyAppDatabase::class.java,
                            "timesnap_pro.db"
                        ).fallbackToDestructiveMigration(dropAllTables = true).build()
                        INSTANCE = DatabaseHelper(db)
                    }
                }
            }
        }

        fun getInstance(context: Context): DatabaseHelper {
            if (INSTANCE == null) {
                init(context)
            }
            return INSTANCE!!
        }

        val instance: DatabaseHelper
            get() = INSTANCE ?: throw IllegalStateException("DatabaseHelper not initialized. Call init(context) in Application or MainActivity.")
    }

    /**
     * Tự động tạo một bản ghi (Record) cấu hình lương mặc định cho UID mới này
     * trong bảng user_config dưới SQLite (Room) và Firestore
     */
    suspend fun insertDefaultConfig(uid: String, name: String = "Nhân viên mới") {
        withContext(Dispatchers.IO) {
            val existing = userConfigDao.getConfig(uid)
            val config = if (existing == null) {
                UserConfig(
                    uid = uid,
                    fullName = name,
                    hourlyRate = 50000.0, // Default 50k VND/hour
                    currency = "đ",
                    dailyTargetHours = 8.0
                )
            } else {
                existing
            }
            if (existing == null) {
                userConfigDao.insertConfig(config)
            }
            try {
                FirestoreService.saveUserConfig(config)
            } catch (e: Throwable) {
                android.util.Log.e("DatabaseHelper", "Failed default config sync: ${e.message}")
            }
        }
    }

    suspend fun getConfig(uid: String): UserConfig? {
        return withContext(Dispatchers.IO) {
            userConfigDao.getConfig(uid)
        }
    }

    fun getConfigFlow(uid: String): Flow<UserConfig?> {
        return userConfigDao.getConfigFlow(uid)
    }

    suspend fun saveConfig(config: UserConfig) {
        withContext(Dispatchers.IO) {
            userConfigDao.insertConfig(config)
            try {
                FirestoreService.saveUserConfig(config)
            } catch (e: Throwable) {
                android.util.Log.e("DatabaseHelper", "Failed save config sync: ${e.message}")
            }
        }
    }

    fun getRecords(uid: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getRecordsForUser(uid)
    }

    suspend fun getActiveRecord(uid: String): AttendanceRecord? {
        return withContext(Dispatchers.IO) {
            attendanceDao.getActiveRecordForUser(uid)
        }
    }

    suspend fun clockIn(uid: String, notes: String = ""): Boolean {
        return withContext(Dispatchers.IO) {
            val active = attendanceDao.getActiveRecordForUser(uid)
            if (active != null) {
                false // Already clocked in
            } else {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val todayString = formatter.format(Date())
                val newRecord = AttendanceRecord(
                    uid = uid,
                    dateString = todayString,
                    clockInTime = System.currentTimeMillis(),
                    clockOutTime = null,
                    status = "Active",
                    notes = notes
                )
                attendanceDao.insertRecord(newRecord)
                serviceScope.launch {
                    try {
                        FirestoreService.saveAttendanceRecord(newRecord)
                    } catch (e: Throwable) {
                        android.util.Log.e("DatabaseHelper", "Failed clockIn sync: ${e.message}")
                    }
                }
                true
            }
        }
    }

    suspend fun clockOut(uid: String, notes: String = ""): Boolean {
        return withContext(Dispatchers.IO) {
            val active = attendanceDao.getActiveRecordForUser(uid)
            if (active == null) {
                false // Not clocked in
            } else {
                val updated = active.copy(
                    clockOutTime = System.currentTimeMillis(),
                    status = "Completed",
                    notes = if (notes.isNotBlank()) notes else active.notes
                )
                attendanceDao.updateRecord(updated)
                serviceScope.launch {
                    try {
                        FirestoreService.saveAttendanceRecord(updated)
                    } catch (e: Throwable) {
                        android.util.Log.e("DatabaseHelper", "Failed clockOut sync: ${e.message}")
                    }
                }
                true
            }
        }
    }

    suspend fun deleteRecord(record: AttendanceRecord) {
        withContext(Dispatchers.IO) {
            attendanceDao.deleteRecord(record)
            serviceScope.launch {
                try {
                    FirestoreService.deleteAttendanceRecord(record.uid, record.dateString)
                } catch (e: Throwable) {
                    android.util.Log.e("DatabaseHelper", "Failed deleteRecord sync: ${e.message}")
                }
            }
        }
    }

    suspend fun deleteAttendanceRecord(uid: String, dateString: String) {
        deleteRecordsForDates(uid, listOf(dateString))
    }

    suspend fun deleteRecordsForDates(uid: String, dates: List<String>) {
        if (dates.isEmpty()) return
        withContext(Dispatchers.IO) {
            attendanceDao.deleteRecordsForDates(uid, dates)
            serviceScope.launch {
                dates.forEach { date ->
                    try {
                        FirestoreService.deleteAttendanceRecord(uid, date)
                    } catch (e: Throwable) {
                        android.util.Log.e("DatabaseHelper", "Failed deleteRecordsForDates sync for date $date: ${e.message}")
                    }
                }
            }
        }
    }

    suspend fun insertManualRecord(record: AttendanceRecord) {
        withContext(Dispatchers.IO) {
            attendanceDao.insertRecord(record)
            serviceScope.launch {
                try {
                    FirestoreService.saveAttendanceRecord(record)
                } catch (e: Throwable) {
                    android.util.Log.e("DatabaseHelper", "Failed insertManualRecord sync: ${e.message}")
                }
            }
        }
    }

    suspend fun insertManualRecords(records: List<AttendanceRecord>) {
        withContext(Dispatchers.IO) {
            records.forEach { record ->
                attendanceDao.insertRecord(record)
            }
            serviceScope.launch {
                records.forEach { record ->
                    try {
                        FirestoreService.saveAttendanceRecord(record)
                    } catch (e: Throwable) {
                        android.util.Log.e("DatabaseHelper", "Failed insertManualRecords sync: ${e.message}")
                    }
                }
            }
        }
    }
}
