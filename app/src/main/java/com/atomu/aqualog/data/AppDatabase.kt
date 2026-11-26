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
        LivestockPhoto::class, LivestockNote::class,
    ],
    version = 3,
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

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aqualog.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
