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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.atomu.aqualog.data.KIND_ROUTINE
import com.atomu.aqualog.data.LogEntry
import com.atomu.aqualog.data.Reminder
import com.atomu.aqualog.data.STATUS_ALIVE
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.data.cycleText
import com.atomu.aqualog.data.fmtTime
import com.atomu.aqualog.data.isDaily
import com.atomu.aqualog.data.nextDue
import com.atomu.aqualog.data.timeList
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HEAD_DATE = DateTimeFormatter.ofPattern("yyyy.M.d EEE", Locale.ENGLISH)

/** 今日やることの1行 */
private data class TodayItem(val r: Reminder, val sortKey: Int, val done: Boolean)

@Composable
fun HomeScreen(vm: AppViewModel, onOpenTank: (Long) -> Unit) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val live by vm.livestock.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val t = today()
    val mono = FontFamily.Monospace

    val todays = reminders
        .filter { it.enabled && it.kind == KIND_ROUTINE }
        .mapNotNull { r ->
            val doneToday = r.lastDone >= t && (r.isDaily || r.doneTime != null)
            when {
                r.isDaily -> TodayItem(r, r.timeList.firstOrNull() ?: 24 * 60, doneToday)
                doneToday -> TodayItem(r, 24 * 60 + 1, true)
                r.nextDue <= t + 1 -> TodayItem(r, 24 * 60 + 1, false)
                else -> null
            }
        }
        .sortedWith(compareBy({ it.sortKey }, { it.r.id }))
    val doneCount = todays.count { it.done }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 10.dp)) {
                Text(
                    LocalDate.now().format(HEAD_DATE).uppercase(),
                    fontFamily = mono,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (todays.isNotEmpty()) {
                    Text("$doneCount / ${todays.size} 済", fontFamily = mono, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
        }
        item {
            Text(
                "今日やること",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
            )
        }
        if (todays.isEmpty()) {
            item { EmptyNote("リマインドで「毎日」の項目を作ると、ここにチェックリストが出ます") }
        }
        items(todays, key = { "r" + it.r.id }) { ti ->
            TodayRow(ti, tanks, onCheck = { checked ->
                if (checked) vm.checkItem(ti.r) else if (ti.r.isDaily) vm.uncheckItem(ti.r)
            })
        }
        items(tanks, key = { "t" + it.id }) { tank ->
            TankCard(
                vm = vm,
                tank = tank,
                logs = logs.filter { it.tankId == tank.id },
                liveCount = live.filter { it.tankId == tank.id && it.status == STATUS_ALIVE }.sumOf { it.count },
                change = reminders.firstOrNull { it.enabled && it.tankId == tank.id && it.logType == "換水" },
                onOpen = { onOpenTank(tank.id) },
            )
        }
    }
}

@Composable
private fun TodayRow(item: TodayItem, tanks: List<Tank>, onCheck: (Boolean) -> Unit) {
    val r = item.r
    val mono = FontFamily.Monospace
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Checkbox(
                checked = item.done,
                onCheckedChange = onCheck,
                enabled = !item.done || r.isDaily,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            )
            Text(
                r.timeList.firstOrNull()?.fmtTime() ?: "—",
                fontFamily = mono,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(48.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${tanks.nameOf(r.tankId)}／${if (r.isDaily) "毎日" else r.cycleText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                item.done -> Text(
                    (r.doneTime?.fmtTime()?.plus(" ") ?: "") + "済",
                    fontFamily = mono, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                !r.isDaily && r.nextDue < today() -> WarnBadge("+${today() - r.nextDue}日")
                r.timeList.isNotEmpty() -> Text(
                    "通知 " + r.timeList.joinToString(" ") { it.fmtTime() },
                    fontFamily = mono, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                else -> Text(dueText(r), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun TankCard(
    vm: AppViewModel,
    tank: Tank,
    logs: List<LogEntry>,
    liveCount: Int,
    change: Reminder?,
    onOpen: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            vm.importPhoto(uri)?.let { vm.saveTank(tank.copy(photoPath = it), previousPhoto = tank.photoPath) }
        }
    }
    var viewing by remember { mutableStateOf(false) }
    val mono = FontFamily.Monospace
    val t = today()

    Column(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
        // 水槽の写真（なければ追加ボタン）
        if (tank.photoPath != null) {
            AsyncImage(
                model = File(tank.photoPath),
                contentDescription = "${tank.name}の写真",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(170.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .clickable { viewing = true },
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(72.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center,
            ) {
                Text("＋ 水槽の写真を追加", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tank.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val lastChange = logs.firstOrNull { it.type == "換水" }
                val info = buildList {
                    tank.volumeL?.let { add("約${it.fmt()}L") }
                    add("生体 $liveCount")
                    add("最終換水 " + (lastChange?.date?.fmtShort() ?: "—"))
                }.joinToString("／")
                Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                change?.let { r ->
                    val left = r.nextDue - t
                    if (left < 0) WarnBadge("換水の予定から${-left}日たちました", Modifier.padding(top = 2.dp))
                    else Text(
                        if (left == 0L) "今日は換水の日" else "換水まで あと${left}日",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Text("詳細 ›", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // 直近14日の給餌
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("直近14日の給餌", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val fedDays = logs.filter { it.type == "給餌" }.map { it.date }.toSet()
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (d in (t - 13)..t) {
                    Box(
                        Modifier.weight(1f).height(10.dp).background(
                            if (d in fedDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }
            Row {
                Text((t - 13).fmtShort(), fontFamily = mono, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("今日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (viewing && tank.photoPath != null) {
        SinglePhotoDialog(tank.photoPath, tank.name) { viewing = false }
    }
}
