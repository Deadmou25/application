package com.example.myapplication.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.R
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.utils.TimeUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar

class CheckMedicineWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).medicineDao()

        val now = Calendar.getInstance()
        val soon = Calendar.getInstance().apply { add(Calendar.MINUTE, 30) }

        val nowStr = TimeUtils.formatDateTime(
            now.get(Calendar.YEAR), now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)
        )
        val soonStr = TimeUtils.formatDateTime(
            soon.get(Calendar.YEAR), soon.get(Calendar.MONTH),
            soon.get(Calendar.DAY_OF_MONTH), soon.get(Calendar.HOUR_OF_DAY), soon.get(Calendar.MINUTE)
        )

        val upcoming = dao.getAll().first().filter { it.dateTime in nowStr..soonStr }
        upcoming.forEach { sendNotification(it.dateTime) }

        return Result.success()
    }

    private fun sendNotification(dateTime: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Напоминания о лекарствах", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Время принять лекарство")
            .setContentText("Запланировано на $dateTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(dateTime.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "medicine_reminders"
        const val WORK_NAME = "check_medicine_worker"
    }
}
