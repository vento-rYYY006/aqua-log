package com.atomu.aqualog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.atomu.aqualog.data.DiaryEntry
import com.atomu.aqualog.data.LogEntry
import com.atomu.aqualog.data.Reminder
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.data.cycleText
import com.atomu.aqualog.data.fmtTime
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_HEAD = DateTimeFormatter.ofPattern("M月d日（E）", Locale.JAPAN)

@Composable
fun DiaryScreen(vm: AppViewModel) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val sel by vm.selectedTank.collectAsStateWithLifecycle()
    var day by rememberSaveable { mutableLongStateOf(today()) }
    var monthView by rememberSaveable { mutableStateOf(false) }
    val tankId = sel ?: tanks.firstOrNull()?.id
    val tank = tanks.firstOrNull { it.id == tankId }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            TankFilter(tanks, tankId, allowAll = false) { vm.selectedTank.value = it }
        }
        if (tank == null) {
            EmptyNote("水槽を登録すると日記が使えます")
        } else if (monthView) {
            DiaryMonth(vm, tank, day, onPick = { day = it; monthView = false }, onMonthChange = { day = it }, onClose = { monthView = false })
        } else {
            DiaryDay(vm, tank, day, onDayChange = { day = it }, onOpenMonth = { monthView = true })
        }
    }
}

@Composable
private fun DiaryDay(vm: AppViewModel, tank: Tank, day: Long, onDayChange: (Long) -> Unit, onOpenMonth: () -> Unit) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val tests by vm.tests.collectAsStateWithLifecycle()
    val diary by vm.diary.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val entry = diary.firstOrNull { it.tankId == tank.id && it.date == day }
    val tankLogs = logs.filter { it.tankId == tank.id }
    val dayLogs = tankLogs.filter { it.date == day }.sortedWith(compareBy({ it.time ?: Int.MAX_VALUE }, { it.id }))
    val dayTests = tests.filter { it.tankId == tank.id && it.date == day }
    val mono = FontFamily.Monospace
    val isToday = day == today()

    var editingNote by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<LogEntry?>(null) }
    var viewing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = vm.importPhoto(uri) ?: return@launch
            val base = entry ?: DiaryEntry(tankId = tank.id, date = day)
            vm.saveDiary(base.copy(photoPath = path, photoTime = if (isToday) nowMinutes() else base.photoTime), previousPhoto = entry?.photoPath)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onDayChange(day - 1) }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                    Text(
                        LocalDate.ofEpochDay(day).format(DAY_HEAD),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onDayChange(day + 1) }, enabled = !isToday) {
                        Text("›", style = MaterialTheme.typography.titleLarge)
                    }
                    TextButton(onClick = onOpenMonth) { Text("月") }
                }
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
            }
        }
        // 写真
        item {
            val path = entry?.photoPath
            if (path != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "この日の写真",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable { viewing = true },
                    )
                    Row {
                        Text(
                            entry?.photoTime?.let { it.fmtTime() + " 撮影" } ?: "",
                            fontFamily = mono, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                        )
                        Text(
                            "写真を変える",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        )
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(110.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("＋ この日の写真を追加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // 換水・給餌・点灯・足し水
        item {
            val change = reminders.firstOrNull { it.tankId == tank.id && it.logType == "換水" }
            DiaryStats(tank, tankLogs, day, change)
        }
        // 備考
        item {
            Column(Modifier.fillMaxWidth().clickable { editingNote = true }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("備考", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    entry?.note?.takeIf { it.isNotBlank() } ?: "タップして書く（例：スターポリプがよく開いている）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (entry?.note.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        // この日の記録
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("この日の記録", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    editingLog = LogEntry(tankId = tank.id, date = day, type = "換水", time = if (isToday) nowMinutes() else null)
                }) { Text("＋ 記録") }
            }
        }
        if (dayLogs.isEmpty() && dayTests.isEmpty()) item { EmptyNote("この日の記録はありません") }
        items(dayLogs, key = { "l" + it.id }) { e ->
            Column(Modifier.fillMaxWidth().clickable { editingLog = e }) {
                Row(Modifier.padding(vertical = 8.dp)) {
                    Text(e.time?.fmtTime() ?: "—", fontFamily = mono, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
                    Column {
                        Text(listOf(e.type, e.amount).filter { it.isNotBlank() }.joinToString("　"), style = MaterialTheme.typography.bodyMedium)
                        if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        items(dayTests, key = { "w" + it.id }) { t ->
            Column {
                Row(Modifier.padding(vertical = 8.dp)) {
                    Text("—", fontFamily = mono, modifier = Modifier.width(56.dp))
                    Text("水質測定　" + t.summary(), style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    if (editingNote) {
        var note by remember { mutableStateOf(entry?.note ?: "") }
        EditDialog(
            title = "備考",
            onDismiss = { editingNote = false },
            onSave = {
                val base = entry ?: DiaryEntry(tankId = tank.id, date = day)
                vm.saveDiary(base.copy(note = note.trim()), previousPhoto = null)
                editingNote = false
            },
        ) {
            TextInput("この日の様子", note, { note = it }, singleLine = false)
        }
    }
    editingLog?.let { e ->
        LogDialog(
            init = e, tanks = tanks,
            onDismiss = { editingLog = null },
            onSave = { vm.saveLog(it); editingLog = null },
            onDelete = if (e.id != 0L) ({ vm.deleteLog(e); editingLog = null }) else null,
        )
    }
    if (viewing) entry?.photoPath?.let { SinglePhotoDialog(it, "${tank.name}　${LocalDate.ofEpochDay(day).format(DAY_HEAD)}") { viewing = false } }
}

@Composable
private fun DiaryStats(tank: Tank, logs: List<LogEntry>, day: Long, change: Reminder?) {
    val isToday = day == today()
    val now = nowMinutes()
    fun lastOf(type: String) = logs.filter { it.type == type && it.date <= day }
        .maxWithOrNull(compareBy({ it.date }, { it.time ?: -1 }))

    val ch = lastOf("換水")
    val changeMain = ch?.let { if (it.date == day) "今日" else "${day - it.date}日前" } ?: "—"
    val changeSub = ch?.let { "最終 ${it.date.fmtShort()}" + (change?.let { r -> "（${r.cycleText}）" } ?: "") } ?: "記録なし"

    val feed = lastOf("給餌")
    val feedMain = when {
        feed == null -> "—"
        isToday && feed.date == day && feed.time != null -> {
            val h = (now - feed.time) / 60
            if (h <= 0) "${now - feed.time}分空き" else "${h}時間空き"
        }
        feed.date == day -> "${logs.count { it.type == "給餌" && it.date == day }}回"
        else -> "${day - feed.date}日空き"
    }
    val feedSub = feed?.let { f -> "最終 " + (if (f.date == day) f.time?.fmtTime() ?: f.date.fmtShort() else f.date.fmtShort()) } ?: "記録なし"

    val on = tank.lightOn
    val off = tank.lightOff
    val lightMain = on?.fmtTime() ?: "未設定"
    val lightSub = if (on != null && off != null) {
        val len = ((off - on + 24 * 60) % (24 * 60)) / 60.0
        "消灯 ${off.fmtTime()}（${len.fmt()}時間）"
    } else "水槽の編集で設定"

    val top = lastOf("足し水")
    val topMain = when {
        top == null -> "—"
        isToday && top.date == day && top.time != null -> {
            val h = (now - top.time) / 60
            if (h <= 0) "${now - top.time}分前" else "${h}時間前"
        }
        top.date == day -> "この日"
        else -> "${day - top.date}日前"
    }
    val topSub = top?.let { t -> "最終 " + (if (t.date == day) t.time?.fmtTime() ?: "" else t.date.fmtShort()) } ?: "記録なし"

    Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.height(72.dp)) {
            StatCell("換水", changeMain, changeSub, Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxSize().background(MaterialTheme.colorScheme.outline))
            StatCell("給餌", feedMain, feedSub, Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(Modifier.height(72.dp)) {
            StatCell("点灯", lightMain, lightSub, Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxSize().background(MaterialTheme.colorScheme.outline))
            StatCell("足し水", topMain, topSub, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, main: String, sub: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(main, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun DiaryMonth(
    vm: AppViewModel,
    tank: Tank,
    day: Long,
    onPick: (Long) -> Unit,
    onMonthChange: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    val diary by vm.diary.collectAsStateWithLifecycle()
    val ym = YearMonth.from(LocalDate.ofEpochDay(day))
    val first = ym.atDay(1)
    val lead = first.dayOfWeek.value % 7 // 日曜はじまり
    val days = ym.lengthOfMonth()
    val t = today()
    val tankLogs = logs.filter { it.tankId == tank.id && YearMonth.from(LocalDate.ofEpochDay(it.date)) == ym }
    val photos = diary.filter { it.tankId == tank.id && it.photoPath != null }.associateBy { it.date }
    val changeDays = tankLogs.filter { it.type == "換水" }.map { it.date }.toSet()
    val feedDays = tankLogs.filter { it.type == "給餌" }.map { it.date }.toSet()
    val warn = LocalWarn.current
    val mono = FontFamily.Monospace

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onMonthChange(ym.minusMonths(1).atDay(1).toEpochDay()) }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                    Text("${ym.year}年${ym.monthValue}月", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { onMonthChange(minOf(ym.plusMonths(1).atDay(1).toEpochDay(), t)) },
                        enabled = ym.plusMonths(1).atDay(1).toEpochDay() <= t,
                    ) { Text("›", style = MaterialTheme.typography.titleLarge) }
                    TextButton(onClick = onClose) { Text("日") }
                }
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
            }
        }
        item {
            Row {
                listOf("日", "月", "火", "水", "木", "金", "土").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            val cells = lead + days
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (row in 0 until (cells + 6) / 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (col in 0 until 7) {
                            val n = row * 7 + col - lead + 1
                            if (n < 1 || n > days) {
                                Box(Modifier.weight(1f).aspectRatio(0.8f))
                            } else {
                                val d = first.plusDays((n - 1).toLong()).toEpochDay()
                                val future = d > t
                                Box(
                                    Modifier.weight(1f).aspectRatio(0.8f)
                                        .border(1.dp, if (d == day) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant)
                                        .let { if (!future) it.clickable { onPick(d) } else it },
                                ) {
                                    photos[d]?.photoPath?.let { p ->
                                        AsyncImage(model = File(p), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                    Text(
                                        "$n", fontFamily = mono, style = MaterialTheme.typography.labelSmall,
                                        color = if (future) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(3.dp)
                                            .let { if (photos[d] != null) it.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) else it },
                                    )
                                    Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        if (d in changeDays) Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.secondary))
                                        if (!future && d !in feedDays) Box(Modifier.size(7.dp).background(warn.text))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend(MaterialTheme.colorScheme.secondary, "換水")
                Legend(warn.text, "給餌なし")
            }
        }
        item {
            val pastDays = (1..days).count { first.plusDays((it - 1).toLong()).toEpochDay() <= t }
            val photoCount = photos.keys.count { YearMonth.from(LocalDate.ofEpochDay(it)) == ym }
            Row(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)) {
                StatCell("写真", "${photoCount}日", "", Modifier.weight(1f))
                StatCell("換水", "${changeDays.size}回", "", Modifier.weight(1f))
                StatCell("給餌", "${feedDays.size}/${pastDays}日", "", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
