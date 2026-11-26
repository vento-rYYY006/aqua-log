package com.atomu.aqualog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AquaDao {
    // 水槽
    @Query("SELECT * FROM tanks ORDER BY sortOrder, id")
    abstract fun tanks(): Flow<List<Tank>>

    @Query("SELECT * FROM tanks ORDER BY sortOrder, id")
    abstract suspend fun tanksOnce(): List<Tank>

    @Query("SELECT COUNT(*) FROM tanks")
    abstract suspend fun tankCount(): Int

    @Upsert
    abstract suspend fun upsertTank(tank: Tank): Long

    @Delete
    abstract suspend fun deleteTankOnly(tank: Tank)

    @Query("DELETE FROM logs WHERE tankId = :tankId")
    abstract suspend fun deleteLogsOf(tankId: Long)

    @Query("DELETE FROM water_tests WHERE tankId = :tankId")
    abstract suspend fun deleteTestsOf(tankId: Long)

    @Query("DELETE FROM livestock WHERE tankId = :tankId")
    abstract suspend fun deleteLivestockOf(tankId: Long)

    @Query("DELETE FROM reminders WHERE tankId = :tankId")
    abstract suspend fun deleteRemindersOf(tankId: Long)

    @Query("SELECT photoPath FROM livestock WHERE tankId = :tankId AND photoPath IS NOT NULL")
    abstract suspend fun photosOf(tankId: Long): List<String>

    @Transaction
    open suspend fun deleteTankCascade(tank: Tank) {
        deleteLogsOf(tank.id)
        deleteTestsOf(tank.id)
        deleteLivestockOf(tank.id)
        deleteRemindersOf(tank.id)
        deleteTankOnly(tank)
    }

    // 作業ログ
    @Query("SELECT * FROM logs ORDER BY date DESC, id DESC")
    abstract fun logs(): Flow<List<LogEntry>>

    @Upsert
    abstract suspend fun upsertLog(entry: LogEntry): Long

    @Delete
    abstract suspend fun deleteLog(entry: LogEntry)

    // 水質
    @Query("SELECT * FROM water_tests ORDER BY date ASC, id ASC")
    abstract fun tests(): Flow<List<WaterTest>>

    @Upsert
    abstract suspend fun upsertTest(test: WaterTest): Long

    @Delete
    abstract suspend fun deleteTest(test: WaterTest)

    // 生体
    @Query("SELECT * FROM livestock ORDER BY id")
    abstract fun livestock(): Flow<List<Livestock>>

    @Upsert
    abstract suspend fun upsertLivestock(item: Livestock): Long

    @Delete
    abstract suspend fun deleteLivestock(item: Livestock)

    // リマインド
    @Query("SELECT * FROM reminders ORDER BY id")
    abstract fun reminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    abstract suspend fun remindersOnce(): List<Reminder>

    @Upsert
    abstract suspend fun upsertReminder(r: Reminder): Long

    @Delete
    abstract suspend fun deleteReminder(r: Reminder)

    @Query(
        "UPDATE reminders SET lastDone = :date " +
            "WHERE logType = :type AND (tankId IS NULL OR tankId = :tankId) AND lastDone < :date"
    )
    abstract suspend fun markDoneByType(type: String, tankId: Long, date: Long)
}
