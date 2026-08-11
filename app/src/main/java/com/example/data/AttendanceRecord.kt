package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val dateString: String,
    val clockInTime: Long,
    val clockOutTime: Long? = null,
    val status: String = "",
    val notes: String = ""
)
