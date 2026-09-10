package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "salary_advances")
data class SalaryAdvance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val month: String, // format "yyyy-MM" e.g. "2026-09"
    val date: String,  // format "yyyy-MM-dd" or "dd/MM/yyyy"
    val amount: Double = 0.0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
