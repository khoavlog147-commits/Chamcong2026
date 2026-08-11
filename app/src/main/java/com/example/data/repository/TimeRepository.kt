package com.example.data.repository

import com.example.data.db.TimeEntryDao
import com.example.data.db.UserConfigDao
import com.example.data.model.CompanyConfig
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import kotlinx.coroutines.flow.Flow

class TimeRepository(
    private val timeEntryDao: TimeEntryDao,
    private val userConfigDao: UserConfigDao
) {
    fun getEntries(userId: String): Flow<List<TimeEntry>> = timeEntryDao.getEntriesForUser(userId)

    fun getEntriesInMonth(userId: String, monthPattern: String, altMonthPattern: String): Flow<List<TimeEntry>> {
        return timeEntryDao.getEntriesForUserInMonth(userId, monthPattern, altMonthPattern)
    }

    suspend fun getEntryByDate(userId: String, date: String): TimeEntry? {
        return timeEntryDao.getEntryByDate(userId, date)
    }

    suspend fun getActiveEntry(userId: String): TimeEntry? {
        return timeEntryDao.getActiveEntry(userId)
    }

    fun getActiveEntryFlow(userId: String): Flow<TimeEntry?> {
        return timeEntryDao.getActiveEntryFlow(userId)
    }

    suspend fun insertOrUpdate(entry: TimeEntry) {
        timeEntryDao.insertOrUpdate(entry)
    }

    suspend fun delete(entry: TimeEntry) {
        timeEntryDao.delete(entry)
    }

    suspend fun deleteByDate(userId: String, date: String) {
        timeEntryDao.deleteByDate(userId, date)
    }

    suspend fun deleteEntriesInMonth(userId: String, monthPattern: String, altMonthPattern: String) {
        timeEntryDao.deleteEntriesInMonth(userId, monthPattern, altMonthPattern)
    }

    suspend fun getEntriesInMonthDirect(userId: String, monthPattern: String, altMonthPattern: String): List<TimeEntry> {
        return timeEntryDao.getEntriesForUserInMonthDirect(userId, monthPattern, altMonthPattern)
    }

    suspend fun getLastCompletedEntries(userId: String, limit: Int): List<TimeEntry> {
        return timeEntryDao.getLastCompletedEntries(userId, limit)
    }

    fun getConfig(userId: String): Flow<UserConfig?> = userConfigDao.getConfigFlow(userId)

    suspend fun getConfigDirect(userId: String): UserConfig? {
        return userConfigDao.getConfigForUser(userId)
    }

    suspend fun saveConfig(config: UserConfig) {
        userConfigDao.saveConfig(config)
    }

    suspend fun insertDefaultConfig(
        userId: String,
        defaultName: String = "User Demo",
        defaultEmail: String = "",
        defaultMaNhanVien: String = "",
        company: CompanyConfig? = null
    ) {
        val existing = userConfigDao.getConfigForUser(userId)
        val maNhan = if (defaultMaNhanVien.isNotEmpty()) defaultMaNhanVien else "demo_${userId.takeLast(6)}"
        val comp = company ?: CompanyConfig.DEFAULT_COMPANY
        if (existing == null) {
            val initial = comp.applyToUserConfig(
                UserConfig(
                    userId = userId,
                    hoVaTen = defaultName,
                    emailDangKy = defaultEmail,
                    maNhanVien = maNhan
                )
            )
            userConfigDao.saveConfig(initial)
        } else {
            val updated = existing.copy(
                emailDangKy = if (existing.emailDangKy.isEmpty()) defaultEmail else existing.emailDangKy,
                maNhanVien = if (defaultMaNhanVien.isNotEmpty()) defaultMaNhanVien else existing.maNhanVien,
                companyId = if (existing.companyId.isBlank()) comp.companyId else existing.companyId,
                companyName = if (existing.companyName.isBlank()) comp.companyName else existing.companyName,
                companyCode = if (existing.companyCode.isBlank()) comp.companyCode else existing.companyCode
            )
            userConfigDao.saveConfig(updated)
        }
    }
}
