package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTime: String, // Формат: YYYY-MM-DD HH:MM
    val sentAt: Long, // Timestamp отправки
    val deviceName: String? = null // Имя подключенного Bluetooth устройства
)
