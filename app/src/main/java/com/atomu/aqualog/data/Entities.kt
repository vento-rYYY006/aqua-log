package com.atomu.aqualog.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 日付はすべて LocalDate.toEpochDay() の値で持つ */

@Entity(tableName = "tanks")
data class Tank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val volumeL: Double? = null,
    val note: String = "",
    val sortOrder: Int = 0,
    val photoPath: String? = null,
    /** 点灯・消灯の時刻（0時からの分） */
    val lightOn: Int? = null,
    val lightOff: Int? = null,
)

@Entity(tableName = "logs", indices = [Index("tankId")])
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tankId: Long,
    val date: Long,
    val type: String,
    val amount: String = "",
    val note: String = "",
    /** 記録した時刻（0時からの分）。null = 時刻なし */
    val time: Int? = null,
)

/** 日記：水槽ごと・日ごとの写真と備考 */
@Entity(tableName = "diary", indices = [Index(value = ["tankId", "date"])])
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tankId: Long,
    val date: Long,
    val photoPath: String? = null,
    val photoTime: Int? = null,
    val note: String = "",
)

@Entity(tableName = "water_tests", indices = [Index("tankId")])
data class WaterTest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tankId: Long,
    val date: Long,
    val temp: Double? = null,
    val salinity: Double? = null,
    val no3: Double? = null,
    val po4: Double? = null,
    val kh: Double? = null,
    val ca: Double? = null,
    val mg: Double? = null,
    val note: String = "",
)

@Entity(tableName = "livestock", indices = [Index("tankId")])
data class Livestock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tankId: Long,
    val name: String,
    val category: String,
    val count: Int = 1,
    val addedDate: Long? = null,
    val source: String = "",
    val price: Int? = null,
    val photoPath: String? = null,
    val status: String = STATUS_ALIVE,
    val endDate: Long? = null,
    val note: String = "",
    /** 写真がないときに出すアイコン（null = 分類から自動） */
    val iconKind: String? = null,
)

/** 生体の成長記録（写真） */
@Entity(tableName = "livestock_photos", indices = [Index("livestockId")])
data class LivestockPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val livestockId: Long,
    val date: Long,
    val path: String,
)

/** 生体ごとのメモ（混泳・餌の食いつきなど） */
@Entity(tableName = "livestock_notes", indices = [Index("livestockId")])
data class LivestockNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val livestockId: Long,
    val date: Long,
    val tag: String,
    val text: String,
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** null = すべての水槽 */
    val tankId: Long? = null,
    val title: String,
    val intervalDays: Int,
    val lastDone: Long,
    /** この種類の作業を記録したら自動で「完了」にする */
    val logType: String? = null,
    val enabled: Boolean = true,
    /** 0 より大きければ「○か月ごと」（intervalDays より優先） */
    @ColumnInfo(defaultValue = "0") val intervalMonths: Int = 0,
    /** 0 = 毎日・短い周期、1 = 設備・消耗品 */
    @ColumnInfo(defaultValue = "0") val kind: Int = KIND_ROUTINE,
    /** 通知する時刻（"7:30,19:00"）。空なら毎日19時のまとめ通知 */
    val times: String? = null,
    /** 最後に完了した時刻（0時からの分） */
    val doneTime: Int? = null,
)

/** 毎日の項目（今日やること・時刻で通知） */
val Reminder.isDaily: Boolean get() = intervalMonths == 0 && intervalDays <= 1

/** 通知時刻（0時からの分）の一覧 */
val Reminder.timeList: List<Int>
    get() = times.orEmpty().split(',', '、', ' ').mapNotNull { parseTime(it) }.distinct().sorted()

fun parseTime(s: String): Int? {
    val t = s.trim().replace('：', ':')
    if (t.isEmpty()) return null
    val parts = t.split(':')
    val h = parts[0].toIntOrNull() ?: return null
    val m = if (parts.size > 1) parts[1].toIntOrNull() ?: return null else 0
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}

fun Int.fmtTime(): String = "%d:%02d".format(this / 60, this % 60)

val Reminder.nextDue: Long
    get() = if (intervalMonths > 0) {
        java.time.LocalDate.ofEpochDay(lastDone).plusMonths(intervalMonths.toLong()).toEpochDay()
    } else {
        lastDone + intervalDays
    }

val Reminder.cycleText: String
    get() = if (intervalMonths > 0) "${intervalMonths}か月ごと" else "${intervalDays}日ごと"

const val KIND_ROUTINE = 0
const val KIND_EQUIPMENT = 1
val NOTE_TAGS = listOf("メモ", "餌の食いつき", "混泳", "体調")

const val STATUS_ALIVE = "飼育中"
val STATUSES = listOf(STATUS_ALIVE, "移動済み", "譲渡", "死亡")
val CATEGORIES = listOf("魚", "エビ・カニ", "貝", "サンゴ", "イソギンチャク", "その他")
const val TYPE_WATER_TEST = "水質測定"
val LOG_TYPES = listOf(
    "換水", "足し水", "ガラス掃除", "ろ材掃除", "スキマー掃除",
    "機材メンテ", "給餌", "生体追加", "添加剤", TYPE_WATER_TEST, "その他",
)
