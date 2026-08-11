package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigDao {
    @Query("SELECT * FROM user_config WHERE uid = :uid LIMIT 1")
    suspend fun getConfig(uid: String): UserConfig?

    @Query("SELECT * FROM user_config WHERE uid = :uid LIMIT 1")
    fun getConfigFlow(uid: String): Flow<UserConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: UserConfig)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE uid = :uid ORDER BY clockInTime DESC")
    fun getRecordsForUser(uid: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE uid = :uid AND clockOutTime IS NULL LIMIT 1")
    suspend fun getActiveRecordForUser(uid: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE uid = :uid AND dateString IN (:dates)")
    suspend fun deleteRecordsForDates(uid: String, dates: List<String>)
}

@Database(entities = [UserConfig::class, AttendanceRecord::class], version = 2, exportSchema = false)
abstract class LegacyAppDatabase : RoomDatabase() {
    abstract fun userConfigDao(): UserConfigDao
    abstract fun attendanceDao(): AttendanceDao
}
