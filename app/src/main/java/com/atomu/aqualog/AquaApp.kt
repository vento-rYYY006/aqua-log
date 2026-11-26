package com.atomu.aqualog

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.atomu.aqualog.data.AppDatabase
import com.atomu.aqualog.data.Seed
import com.atomu.aqualog.notify.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class AquaApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createChannel()
        scheduleDailyCheck()
        appScope.launch {
            val dao = AppDatabase.get(this@AquaApp).dao()
            if (dao.tankCount() == 0) Seed.run(dao)
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            ReminderWorker.CHANNEL_ID,
            "水槽のリマインド",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "換水や水質チェックの予定日にお知らせします" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** 毎日19時ごろに期限の来たリマインドを通知する */
    private fun scheduleDailyCheck() {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(ReminderWorker.NOTIFY_HOUR, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMin = Duration.between(now, next).toMinutes()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMin, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily-reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
