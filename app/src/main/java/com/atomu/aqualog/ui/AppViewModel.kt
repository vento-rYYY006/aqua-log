package com.atomu.aqualog.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.atomu.aqualog.data.AppDatabase
import com.atomu.aqualog.data.Livestock
import com.atomu.aqualog.data.DiaryEntry
import com.atomu.aqualog.data.isDaily
import com.atomu.aqualog.notify.TimeAlarms
import com.atomu.aqualog.data.LivestockNote
import com.atomu.aqualog.data.LivestockPhoto
import com.atomu.aqualog.data.LogEntry
import com.atomu.aqualog.data.Reminder
import com.atomu.aqualog.data.TYPE_WATER_TEST
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.data.WaterTest
import com.atomu.aqualog.notify.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()

    private fun <T> Flow<List<T>>.state(): StateFlow<List<T>> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tanks = dao.tanks().state()
    val logs = dao.logs().state()
    val tests = dao.tests().state()
    val livestock = dao.livestock().state()
    val reminders = dao.reminders().state()
    val gallery = dao.gallery().state()
    val notes = dao.notes().state()
    val diary = dao.diary().state()

    /** 画面上部で選んでいる水槽（null = すべて） */
    val selectedTank = MutableStateFlow<Long?>(null)

    // 水槽
    fun saveTank(t: Tank, previousPhoto: String? = null) = viewModelScope.launch {
        if (previousPhoto != null && previousPhoto != t.photoPath) deleteFile(previousPhoto)
        dao.upsertTank(t)
    }
    fun deleteTank(t: Tank) = viewModelScope.launch {
        dao.photosOf(t.id).forEach { deleteFile(it) }
        dao.galleryPathsOfTank(t.id).forEach { deleteFile(it) }
        dao.diaryPhotosOf(t.id).forEach { deleteFile(it) }
        dao.tankPhoto(t.id)?.let { deleteFile(it) }
        dao.deleteTankCascade(t)
        if (selectedTank.value == t.id) selectedTank.value = null
    }

    // 作業ログ（記録するとリマインドも自動で完了扱い）
    fun saveLog(e: LogEntry) = viewModelScope.launch {
        dao.upsertLog(e)
        dao.markDoneByType(e.type, e.tankId, e.date)
    }
    fun deleteLog(e: LogEntry) = viewModelScope.launch { dao.deleteLog(e) }

    // 水質
    fun saveTest(t: WaterTest) = viewModelScope.launch {
        dao.upsertTest(t)
        dao.markDoneByType(TYPE_WATER_TEST, t.tankId, t.date)
    }
    fun deleteTest(t: WaterTest) = viewModelScope.launch { dao.deleteTest(t) }

    // 生体
    fun saveLivestock(item: Livestock, previousPhoto: String?) = viewModelScope.launch {
        if (previousPhoto != null && previousPhoto != item.photoPath) deleteFile(previousPhoto)
        dao.upsertLivestock(item)
    }
    fun deleteLivestock(item: Livestock) = viewModelScope.launch {
        item.photoPath?.let { deleteFile(it) }
        dao.galleryPathsOf(item.id).forEach { deleteFile(it) }
        dao.deleteGalleryOf(item.id)
        dao.deleteNotesOf(item.id)
        dao.deleteLivestock(item)
    }

    // 成長記録・メモ
    fun addGalleryPhoto(livestockId: Long, path: String, date: Long) = viewModelScope.launch {
        dao.upsertPhoto(LivestockPhoto(livestockId = livestockId, date = date, path = path))
    }
    fun deleteGalleryPhoto(p: LivestockPhoto) = viewModelScope.launch {
        deleteFile(p.path)
        dao.deletePhoto(p)
    }
    fun saveNote(n: LivestockNote) = viewModelScope.launch { dao.upsertNote(n) }
    fun deleteNote(n: LivestockNote) = viewModelScope.launch { dao.deleteNote(n) }

    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = getApplication<Application>()
            val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
            val file = File(dir, UUID.randomUUID().toString() + ".jpg")
            ctx.contentResolver.openInputStream(uri)!!.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            file.absolutePath
        }.getOrNull()
    }

    fun discardPhoto(path: String) = viewModelScope.launch { deleteFile(path) }

    private suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        runCatching { File(path).delete() }
    }

    // リマインド
    fun saveReminder(r: Reminder) = viewModelScope.launch {
        dao.upsertReminder(r)
        TimeAlarms.rescheduleAll(getApplication())
    }
    fun deleteReminder(r: Reminder) = viewModelScope.launch {
        dao.deleteReminder(r)
        TimeAlarms.rescheduleAll(getApplication())
    }

    /** 今日やることのチェック：完了にして作業ログ（時刻つき）にも残す */
    fun checkItem(r: Reminder) = viewModelScope.launch {
        val t = today()
        val m = nowMinutes()
        dao.upsertReminder(r.copy(lastDone = t, doneTime = m))
        val type = r.logType ?: r.title
        val note = if (r.logType != null && r.title != r.logType) r.title else ""
        val ids = r.tankId?.let { listOf(it) } ?: dao.tanksOnce().map { it.id }
        ids.forEach {
            dao.upsertLog(LogEntry(tankId = it, date = t, type = type, note = note, time = m))
            if (!r.isDaily) dao.markDoneByType(type, it, t)
        }
    }

    /** チェックを外す：今日つけた記録も消す */
    fun uncheckItem(r: Reminder) = viewModelScope.launch {
        val t = today()
        val m = r.doneTime
        dao.upsertReminder(r.copy(lastDone = t - 1, doneTime = null))
        if (m != null) dao.deleteLogsAt(r.logType ?: r.title, t, m)
    }

    // 日記
    fun saveDiary(d: DiaryEntry, previousPhoto: String?) = viewModelScope.launch {
        if (previousPhoto != null && previousPhoto != d.photoPath) deleteFile(previousPhoto)
        dao.upsertDiary(d)
    }
    fun completeReminder(r: Reminder) = viewModelScope.launch {
        dao.upsertReminder(r.copy(lastDone = maxOf(r.lastDone, today())))
    }

    /** 設備・消耗品の「実施した」：作業ログにも残す（全水槽なら各水槽に） */
    fun completeMaintenance(r: Reminder) = viewModelScope.launch {
        val t = today()
        dao.upsertReminder(r.copy(lastDone = maxOf(r.lastDone, t)))
        val ids = r.tankId?.let { listOf(it) } ?: dao.tanksOnce().map { it.id }
        ids.forEach { dao.upsertLog(LogEntry(tankId = it, date = t, type = "機材メンテ", note = r.title)) }
    }

    fun sendTestNotification() {
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(ReminderWorker.KEY_TEST to true))
            .build()
        WorkManager.getInstance(getApplication()).enqueue(req)
    }
}
