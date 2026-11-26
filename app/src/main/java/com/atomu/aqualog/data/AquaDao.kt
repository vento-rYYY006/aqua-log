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

    @Query("DELETE FROM diary WHERE tankId = :tankId")
    abstract suspend fun deleteDiaryOf(tankId: Long)

    @Query("SELECT photoPath FROM diary WHERE tankId = :tankId AND photoPath IS NOT NULL")
    abstract suspend fun diaryPhotosOf(tankId: Long): List<String>

    @Query("DELETE FROM reminders WHERE tankId = :tankId")
    abstract suspend fun deleteRemindersOf(tankId: Long)

    @Query("SELECT photoPath FROM livestock WHERE tankId = :tankId AND photoPath IS NOT NULL")
    abstract suspend fun photosOf(tankId: Long): List<String>

    @Query("SELECT photoPath FROM tanks WHERE id = :tankId")
    abstract suspend fun tankPhoto(tankId: Long): String?

    @Query("SELECT p.path FROM livestock_photos p JOIN livestock l ON p.livestockId = l.id WHERE l.tankId = :tankId")
    abstract suspend fun galleryPathsOfTank(tankId: Long): List<String>

    @Query("DELETE FROM livestock_photos WHERE livestockId IN (SELECT id FROM livestock WHERE tankId = :tankId)")
    abstract suspend fun deleteGalleryOfTank(tankId: Long)

    @Query("DELETE FROM livestock_notes WHERE livestockId IN (SELECT id FROM livestock WHERE tankId = :tankId)")
    abstract suspend fun deleteNotesOfTank(tankId: Long)

    @Transaction
    open suspend fun deleteTankCascade(tank: Tank) {
        deleteGalleryOfTank(tank.id)
        deleteDiaryOf(tank.id)
        deleteNotesOfTank(tank.id)
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

    @Query("DELETE FROM logs WHERE type = :type AND date = :date AND time = :time")
    abstract suspend fun deleteLogsAt(type: String, date: Long, time: Int)

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

    // 生体の成長記録・メモ
    @Query("SELECT * FROM livestock_photos ORDER BY date ASC, id ASC")
    abstract fun gallery(): Flow<List<LivestockPhoto>>

    @Upsert
    abstract suspend fun upsertPhoto(p: LivestockPhoto): Long

    @Delete
    abstract suspend fun deletePhoto(p: LivestockPhoto)

    @Query("SELECT path FROM livestock_photos WHERE livestockId = :id")
    abstract suspend fun galleryPathsOf(id: Long): List<String>

    @Query("DELETE FROM livestock_photos WHERE livestockId = :id")
    abstract suspend fun deleteGalleryOf(id: Long)

    @Query("SELECT * FROM livestock_notes ORDER BY date DESC, id DESC")
    abstract fun notes(): Flow<List<LivestockNote>>

    @Upsert
    abstract suspend fun upsertNote(n: LivestockNote): Long

    @Delete
    abstract suspend fun deleteNote(n: LivestockNote)

    @Query("DELETE FROM livestock_notes WHERE livestockId = :id")
    abstract suspend fun deleteNotesOf(id: Long)

    // 日記
    @Query("SELECT * FROM diary ORDER BY date DESC, id DESC")
    abstract fun diary(): Flow<List<DiaryEntry>>

    @Upsert
    abstract suspend fun upsertDiary(d: DiaryEntry): Long

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
            "WHERE logType = :type AND (tankId IS NULL OR tankId = :tankId) AND lastDone < :date " +
            "AND NOT (intervalMonths = 0 AND intervalDays <= 1)"
    )
    abstract suspend fun markDoneByType(type: String, tankId: Long, date: Long)
}
