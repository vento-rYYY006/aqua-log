package com.atomu.aqualog.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.atomu.aqualog.MainActivity
import com.atomu.aqualog.R
import com.atomu.aqualog.data.AppDatabase
import com.atomu.aqualog.data.nextDue
import java.time.LocalDate

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val dao = AppDatabase.get(ctx).dao()
        val today = LocalDate.now().toEpochDay()
        val tanks = dao.tanksOnce().associateBy { it.id }
        val due = dao.remindersOnce().filter { it.enabled && it.nextDue <= today }

        if (due.isEmpty()) {
            if (inputData.getBoolean(KEY_TEST, false)) {
                post(ctx, 0, "テスト通知", "リマインドの期限が来ると、こんなふうにお知らせします")
            }
            return Result.success()
        }
        due.forEach { r ->
            val tankName = r.tankId?.let { tanks[it]?.name } ?: "全水槽"
            val over = today - r.nextDue
            val body = if (over == 0L) "今日が予定日です" else "予定日から${over}日たちました"
            post(ctx, r.id.toInt() + 1, "$tankName：${r.title}", body)
        }
        return Result.success()
    }

    private fun post(ctx: Context, id: Int, title: String, text: String) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val NOTIFY_HOUR = 19
        const val KEY_TEST = "test"
    }
}
