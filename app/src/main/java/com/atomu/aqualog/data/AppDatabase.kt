package com.atomu.aqualog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Tank::class, LogEntry::class, WaterTest::class, Livestock::class, Reminder::class,
        LivestockPhoto::class, LivestockNote::class, DiaryEntry::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AquaDao

    companion object {
        /** v2: 水槽に写真を追加 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tanks ADD COLUMN photoPath TEXT")
            }
        }

        /** v3: 生体アイコン・成長記録・生体メモ、リマインドの月単位とグループ */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE livestock ADD COLUMN iconKind TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN intervalMonths INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN kind INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `livestock_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`livestockId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `path` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_livestock_photos_livestockId` ON `livestock_photos` (`livestockId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `livestock_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`livestockId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `tag` TEXT NOT NULL, `text` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_livestock_notes_livestockId` ON `livestock_notes` (`livestockId`)")
            }
        }

        /** v4: 作業の時刻、通知時刻、点灯・消灯、日記。毎日の給餌（朝・夜）を追加 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN time INTEGER")
                db.execSQL("ALTER TABLE tanks ADD COLUMN lightOn INTEGER")
                db.execSQL("ALTER TABLE tanks ADD COLUMN lightOff INTEGER")
                db.execSQL("ALTER TABLE reminders ADD COLUMN times TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN doneTime INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `diary` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tankId` INTEGER NOT NULL, " +
                        "`date` INTEGER NOT NULL, `photoPath` TEXT, `photoTime` INTEGER, `note` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_tankId_date` ON `diary` (`tankId`, `date`)")
                val c = db.query("SELECT COUNT(*) FROM reminders WHERE logType = '給餌'")
                val has = c.moveToFirst() && c.getInt(0) > 0
                c.close()
                if (!has) {
                    val yesterday = java.time.LocalDate.now().toEpochDay() - 1
                    listOf("給餌（朝）" to "7:30", "給餌（夜）" to "19:00").forEach { (title, time) ->
                        db.execSQL(
                            "INSERT INTO reminders (tankId, title, intervalDays, lastDone, logType, enabled, intervalMonths, kind, times) " +
                                "VALUES (NULL, ?, 1, ?, '給餌', 1, 0, 0, ?)",
                            arrayOf<Any>(title, yesterday, time),
                        )
                    }
                }
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aqualog.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
