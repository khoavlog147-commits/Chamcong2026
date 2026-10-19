package com.example.data.db

import androidx.room.*
import com.example.data.model.UserConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigDao {
    @Query("SELECT * FROM user_config WHERE userId = :userId LIMIT 1")
    suspend fun getConfigForUser(userId: String): UserConfig?

    @Query("SELECT * FROM user_config WHERE userId = :userId LIMIT 1")
    fun getConfigFlow(userId: String): Flow<UserConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: UserConfig)
}
