package com.example.data.db

import androidx.room.*
import com.example.data.model.SalaryAdvance
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryAdvanceDao {
    @Query("SELECT * FROM salary_advances WHERE userId = :userId AND month = :month ORDER BY createdAt DESC")
    fun getAdvancesForMonth(userId: String, month: String): Flow<List<SalaryAdvance>>

    @Query("SELECT * FROM salary_advances WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllAdvances(userId: String): Flow<List<SalaryAdvance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(advance: SalaryAdvance): Long

    @Delete
    suspend fun deleteAdvance(advance: SalaryAdvance)

    @Update
    suspend fun updateAdvance(advance: SalaryAdvance)

    @Query("DELETE FROM salary_advances WHERE userId = :userId AND month = :month")
    suspend fun deleteAdvancesForMonth(userId: String, month: String)
}
