package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность Room — одна запись об отправленном времени приёма лекарства.
 *
 * Хранится в таблице [medicines] SQLite-базы данных.
 * Создаётся в [com.example.myapplication.data.MedicineRepository.sendMedicineTime]
 * после успешной отправки данных на Arduino.
 *
 * @property id Первичный ключ, генерируется автоматически Room
 * @property dateTime Запланированное время приёма лекарства: формат "YYYY-MM-DD HH:MM"
 * @property sentAt Unix-timestamp (миллисекунды) момента фактической отправки
 * @property deviceName Bluetooth-имя устройства Arduino (null, если имя недоступно)
 */
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTime: String,
    val sentAt: Long,
    val deviceName: String? = null
)
