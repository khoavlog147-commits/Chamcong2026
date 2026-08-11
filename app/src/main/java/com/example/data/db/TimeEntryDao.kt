package com.example.data.db

import androidx.room.*
import com.example.data.model.TimeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {
    @Query("SELECT * FROM time_entries WHERE userId = :userId ORDER BY date DESC")
    fun getEntriesForUser(userId: String): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getEntryByDate(userId: String, date: String): TimeEntry?

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND isWorking = 1 ORDER BY checkInTime DESC LIMIT 1")
    suspend fun getActiveEntry(userId: String): TimeEntry?

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND isWorking = 1 ORDER BY checkInTime DESC LIMIT 1")
    fun getActiveEntryFlow(userId: String): Flow<TimeEntry?>

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND (date LIKE :monthPattern OR date LIKE :altMonthPattern) ORDER BY date ASC")
    fun getEntriesForUserInMonth(userId: String, monthPattern: String, altMonthPattern: String): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND (date LIKE :monthPattern OR date LIKE :altMonthPattern) ORDER BY date ASC")
    suspend fun getEntriesForUserInMonthDirect(userId: String, monthPattern: String, altMonthPattern: String): List<TimeEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: TimeEntry)

    @Delete
    suspend fun delete(entry: TimeEntry)

    @Query("DELETE FROM time_entries WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM time_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteByDate(userId: String, date: String)

    @Query("DELETE FROM time_entries WHERE userId = :userId AND (date LIKE :monthPattern OR date LIKE :altMonthPattern)")
    suspend fun deleteEntriesInMonth(userId: String, monthPattern: String, altMonthPattern: String)

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND checkInTime IS NOT NULL AND checkOutTime IS NOT NULL ORDER BY date DESC LIMIT :limit")
    suspend fun getLastCompletedEntries(userId: String, limit: Int): List<TimeEntry>
}
