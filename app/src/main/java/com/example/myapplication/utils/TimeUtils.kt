package com.example.myapplication.utils

import java.util.Calendar

object TimeUtils {
    /**
     * Форматирует дату и время в формат YYYY-MM-DD HH:MM
     * @param year Год
     * @param month Месяц (0-11, как в Calendar)
     * @param day День месяца
     * @param hour Час (0-23)
     * @param minute Минута (0-59)
     * @return Строка в формате "YYYY-MM-DD HH:MM"
     */
    fun formatDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): String {
        val monthFormatted = (month + 1).toString().padStart(2, '0')
        val dayFormatted = day.toString().padStart(2, '0')
        val hourFormatted = hour.toString().padStart(2, '0')
        val minuteFormatted = minute.toString().padStart(2, '0')

        return "$year-$monthFormatted-$dayFormatted $hourFormatted:$minuteFormatted"
    }

    /**
     * Получает текущую дату и время в формате YYYY-MM-DD HH:MM
     * @return Строка в формате "YYYY-MM-DD HH:MM"
     */
    fun formatCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        return formatDateTime(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    /**
     * Валидирует формат строки даты и времени
     * @param dateTime Строка в формате "YYYY-MM-DD HH:MM"
     * @return true если формат корректен
     */
    fun isValidFormat(dateTime: String): Boolean {
        val pattern = Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")
        return pattern.matches(dateTime)
    }
}