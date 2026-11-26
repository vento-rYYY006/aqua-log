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
)

@Entity(tableName = "logs", indices = [Index("tankId")])
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tankId: Long,
    val date: Long,
    val type: String,
    val amount: String = "",
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
)

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
