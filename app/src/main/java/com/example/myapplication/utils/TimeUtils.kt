package com.example.myapplication.utils

import java.util.Calendar

/**
 * Утилитный объект для работы с форматом даты и времени приложения.
 *
 * Все методы работают с форматом "YYYY-MM-DD HH:MM" — стандартом,
 * используемым для передачи данных на Arduino и хранения в Room.
 *
 * Важно: Calendar и DatePicker используют месяцы в диапазоне 0–11
 * (0 = январь, 11 = декабрь). Этот объект принимает месяц в том же формате
 * и самостоятельно преобразует его в 1–12 при форматировании.
 */
object TimeUtils {

    /**
     * Форматирует компоненты даты и времени в строку "YYYY-MM-DD HH:MM".
     *
     * @param year Год (например, 2026)
     * @param month Месяц в формате Calendar/DatePicker: 0 = январь, 11 = декабрь
     * @param day День месяца (1–31)
     * @param hour Час в 24-часовом формате (0–23)
     * @param minute Минута (0–59)
     * @return Строка вида "2026-04-23 14:30"
     */
    fun formatDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): String {
        val monthFormatted = (month + 1).toString().padStart(2, '0')
        val dayFormatted = day.toString().padStart(2, '0')
        val hourFormatted = hour.toString().padStart(2, '0')
        val minuteFormatted = minute.toString().padStart(2, '0')

        return "$year-$monthFormatted-$dayFormatted $hourFormatted:$minuteFormatted"
    }

    /**
     * Возвращает текущие дату и время устройства в формате "YYYY-MM-DD HH:MM".
     * Используется для фиксации момента фактической отправки данных на Arduino.
     *
     * @return Строка текущего времени, например "2026-04-23 09:15"
     */
    fun formatCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        return formatDateTime(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),        // 0-based
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    /**
     * Проверяет, соответствует ли строка формату "YYYY-MM-DD HH:MM".
     * Используется для валидации данных перед обработкой.
     *
     * Пример валидной строки: "2026-04-23 14:30"
     * Пример невалидной строки: "23.04.2026 14:30", "2026-4-23 9:5"
     *
     * @param dateTime Строка для проверки
     * @return true — строка соответствует ожидаемому формату
     */
    fun isValidFormat(dateTime: String): Boolean {
        val pattern = Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")
        return pattern.matches(dateTime)
    }
}
