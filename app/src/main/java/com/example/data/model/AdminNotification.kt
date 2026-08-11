package com.example.data.model

data class AdminNotification(
    val id: String = "",
    val targetUid: String = "ALL", // "ALL" hoặc UID cụ thể của nhân viên
    val targetName: String = "Tất cả nhân viên",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL", // "SHIFT_REMINDER", "SHIFT_CHANGE", "AUTO_TIME_APPROVED", "GENERAL"
    val createdAt: Long = System.currentTimeMillis(),
    val sentBy: String = "Admin"
)
