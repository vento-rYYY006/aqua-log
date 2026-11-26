package com.atomu.aqualog.data

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
)

val Reminder.nextDue: Long get() = lastDone + intervalDays

const val STATUS_ALIVE = "飼育中"
val STATUSES = listOf(STATUS_ALIVE, "移動済み", "譲渡", "死亡")
val CATEGORIES = listOf("魚", "エビ・カニ", "貝", "サンゴ", "イソギンチャク", "その他")
const val TYPE_WATER_TEST = "水質測定"
val LOG_TYPES = listOf(
    "換水", "足し水", "ガラス掃除", "ろ材掃除", "スキマー掃除",
    "機材メンテ", "給餌", "生体追加", "添加剤", TYPE_WATER_TEST, "その他",
)
