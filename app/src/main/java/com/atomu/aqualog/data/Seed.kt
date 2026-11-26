package com.atomu.aqualog.data

import java.time.LocalDate

/** 初回起動時だけ入れる初期データ（アプリ内で自由に編集・削除できる） */
object Seed {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day).toEpochDay()

    suspend fun run(dao: AquaDao) {
        val today = LocalDate.now().toEpochDay()
        val t60 = dao.upsertTank(Tank(name = "60cm", volumeL = 60.0, note = "グラステリア600／2217＋QQ1", sortOrder = 0))
        val t40 = dao.upsertTank(Tank(name = "40cmキューブ", note = "アクロ40キューブ／PowerBox S605＋QQ1", sortOrder = 1))

        val monthOnly = "導入日は月初で仮入力"
        listOf(
            Livestock(tankId = t60, name = "フレームエンゼル", category = "魚", addedDate = d(2024, 12, 1), note = monthOnly),
            Livestock(tankId = t60, name = "ブラックオセラリス", category = "魚", count = 2, addedDate = d(2024, 2, 1), note = monthOnly),
            Livestock(tankId = t60, name = "クダゴンベ", category = "魚", addedDate = d(2023, 12, 1), note = monthOnly),
            Livestock(tankId = t60, name = "スカンクシュリンプ", category = "エビ・カニ"),
            Livestock(tankId = t60, name = "スターポリプ", category = "サンゴ"),
            Livestock(tankId = t60, name = "ナガレハナサンゴ（軸グリーン）", category = "サンゴ"),
            Livestock(tankId = t40, name = "デバスズメ", category = "魚", count = 3, addedDate = d(2026, 10, 26)),
            Livestock(tankId = t40, name = "カンギクガイ", category = "貝", addedDate = d(2026, 11, 3)),
            Livestock(tankId = t40, name = "タカセガイ", category = "貝", count = 3, addedDate = d(2026, 11, 6)),
            Livestock(tankId = t40, name = "マメスナ", category = "サンゴ", addedDate = d(2026, 11, 6)),
            Livestock(tankId = t40, name = "ウミキノコ", category = "サンゴ", note = "45cmから移動"),
        ).forEach { dao.upsertLivestock(it) }

        dao.upsertReminder(Reminder(tankId = t60, title = "換水", intervalDays = 14, lastDone = today, logType = "換水"))
        dao.upsertReminder(Reminder(tankId = t40, title = "換水", intervalDays = 14, lastDone = today, logType = "換水"))
        dao.upsertReminder(Reminder(tankId = null, title = "比重チェック", intervalDays = 7, lastDone = today, logType = TYPE_WATER_TEST))
    }
}
