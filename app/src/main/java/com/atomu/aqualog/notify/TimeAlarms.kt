package com.atomu.aqualog.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.atomu.aqualog.data.AppDatabase
import com.atomu.aqualog.data.Reminder
import com.atomu.aqualog.data.isDaily
import com.atomu.aqualog.data.timeList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 「給餌 7:30」のように時刻を決めた項目を、その時刻に通知する。
 * 時刻が来たら通知して、次の時刻をまた予約する。
 */
object TimeAlarms {
    private const val ACTION = "com.atomu.aqualog.TIME_ALARM"
    private const val EXTRA_ID = "reminderId"
    private const val MAX_SLOTS = 8
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun requestCode(id: Long, slot: Int) = (id * MAX_SLOTS + slot).toInt()

    private fun pending(ctx: Context, id: Long, slot: Int, create: Boolean): PendingIntent? {
        val intent = Intent(ctx, TimeAlarmReceiver::class.java).setAction(ACTION).putExtra(EXTRA_ID, id)
        val flags = PendingIntent.FLAG_IMMUTABLE or (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(ctx, requestCode(id, slot), intent, flags)
    }

    /** すべての時刻つき項目を予約し直す */
    fun rescheduleAll(context: Context) {
        val ctx = context.applicationContext
        scope.launch {
            val list = AppDatabase.get(ctx).dao().remindersOnce()
            val am = ctx.getSystemService(AlarmManager::class.java)
            list.forEach { r ->
                for (slot in 0 until MAX_SLOTS) pending(ctx, r.id, slot, create = false)?.let { am.cancel(it) }
                if (!r.enabled) return@forEach
                r.timeList.take(MAX_SLOTS).forEachIndexed { slot, minutes ->
                    val at = nextTrigger(minutes)
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx, r.id, slot, create = true)!!)
                }
            }
        }
    }

    private fun nextTrigger(minutes: Int): Long {
        val now = LocalDateTime.now()
        var t = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** 時刻が来たときの処理 */
    internal fun fire(context: Context, id: Long) {
        val ctx = context.applicationContext
        scope.launch {
            val dao = AppDatabase.get(ctx).dao()
            val r: Reminder? = dao.remindersOnce().firstOrNull { it.id == id }
            val today = LocalDate.now().toEpochDay()
            if (r != null && r.enabled) {
                val done = if (r.isDaily) r.lastDone >= today else false
                if (!done) {
                    val tank = r.tankId?.let { tid -> dao.tanksOnce().firstOrNull { it.id == tid }?.name } ?: "全水槽"
                    ReminderWorker.post(ctx, 10_000 + r.id.toInt(), "${r.title}の時間です", tank)
                }
            }
            rescheduleAll(ctx)
        }
    }

    internal const val ACTION_NAME = ACTION
    internal const val EXTRA = EXTRA_ID
}

class TimeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimeAlarms.ACTION_NAME -> {
                val id = intent.getLongExtra(TimeAlarms.EXTRA, -1)
                if (id >= 0) {
                    val pr = goAsync()
                    TimeAlarms.fire(context, id)
                    // 処理は短いので少し待ってから終える
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ pr.finish() }, 3000)
                }
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED" -> TimeAlarms.rescheduleAll(context)
        }
    }
}
