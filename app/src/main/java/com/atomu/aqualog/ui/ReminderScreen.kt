package com.atomu.aqualog.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atomu.aqualog.data.nextDue
import com.atomu.aqualog.data.cycleText
import com.atomu.aqualog.data.timeList
import com.atomu.aqualog.data.fmtTime
import com.atomu.aqualog.data.parseTime
import com.atomu.aqualog.data.isDaily
import com.atomu.aqualog.data.KIND_EQUIPMENT
import com.atomu.aqualog.data.KIND_ROUTINE
import com.atomu.aqualog.data.LOG_TYPES
import com.atomu.aqualog.data.Reminder
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.notify.ReminderWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(vm: AppViewModel) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Reminder?>(null) }
    val context = LocalContext.current

    fun granted() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var hasPermission by remember { mutableStateOf(granted()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val kind = if (tab == 0) KIND_ROUTINE else KIND_EQUIPMENT
    val sorted = reminders.filter { it.kind == kind }.sortedWith(compareBy<Reminder>({ !it.enabled }, { it.nextDue }))

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SecondaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("毎日・短い周期") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("設備・消耗品") })
                }
            }
            if (!hasPermission) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("通知がオフになっています", fontWeight = FontWeight.Bold)
                            Text("期限が来たリマインドを通知するには許可が必要です。")
                            Button(onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("通知を許可する")
                            }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "毎日${ReminderWorker.NOTIFY_HOUR}時ごろ、期限の来たものを通知します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { vm.sendTestNotification() }) { Text("テスト通知") }
                }
            }
            if (sorted.isEmpty()) {
                item {
                    EmptyNote(
                        if (tab == 0) "右下の＋からリマインドを追加できます"
                        else "右下の＋から追加できます\n（例：RO浄水器のフィルター交換 3か月ごと、ヒーター点検 1年ごと）"
                    )
                }
            }
            if (tab == 1 && sorted.isNotEmpty()) {
                item {
                    Text(
                        "「実施した」を押すと作業ログに記録され、次回の予定日が更新されます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(sorted, key = { it.id }) { r ->
                ReminderCard(
                    r, tanks,
                    onDone = { if (r.kind == KIND_EQUIPMENT) vm.completeMaintenance(r) else vm.checkItem(r) },
                    onToggle = { vm.saveReminder(r.copy(enabled = it)) },
                    onClick = { editing = r },
                )
            }
        }
        FloatingActionButton(
            onClick = {
                editing = if (tab == 0) Reminder(title = "", intervalDays = 7, lastDone = today())
                else Reminder(title = "", intervalDays = 90, intervalMonths = 3, lastDone = today(), kind = KIND_EQUIPMENT)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "リマインドを追加") }
    }

    editing?.let { r ->
        ReminderDialog(
            init = r,
            tanks = tanks,
            onDismiss = { editing = null },
            onSave = { vm.saveReminder(it); editing = null },
            onDelete = if (r.id != 0L) ({ vm.deleteReminder(r); editing = null }) else null,
        )
    }
}

fun dueText(r: Reminder): String {
    val left = r.nextDue - today()
    return when {
        left > 1 -> "あと${left}日"
        left == 1L -> "明日"
        left == 0L -> "今日"
        else -> "予定日から${-left}日たちました"
    }
}

@Composable
fun ReminderCard(
    r: Reminder,
    tanks: List<Tank>,
    onDone: () -> Unit,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val overdue = r.enabled && r.nextDue <= today()
    val mod = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ElevatedCard(mod) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${tanks.nameOf(r.tankId)}：${r.title}", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (r.isDaily) "毎日" + (if (r.timeList.isNotEmpty()) "・通知 " + r.timeList.joinToString(" ") { it.fmtTime() } else "")
                    else "${r.cycleText}・前回 ${r.lastDone.fmtShortY()}・次回 ${r.nextDue.fmtShortY()}" +
                        (if (r.timeList.isNotEmpty()) "・通知 " + r.timeList.joinToString(" ") { it.fmtTime() } else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (r.enabled) {
                    if (overdue && r.nextDue < today()) {
                        WarnBadge(dueText(r), Modifier.padding(top = 4.dp))
                    } else {
                        Text(
                            dueText(r),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (overdue) LocalWarn.current.text else MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            if (onToggle != null) {
                Switch(checked = r.enabled, onCheckedChange = onToggle, modifier = Modifier.padding(horizontal = 8.dp))
            }
            if (r.enabled) {
                FilledTonalButton(onClick = onDone) { Text(if (r.kind == KIND_EQUIPMENT) "実施した" else "完了") }
            }
        }
    }
}

@Composable
private fun ReminderDialog(
    init: Reminder,
    tanks: List<Tank>,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember { mutableStateOf(init.title) }
    var tankId by remember { mutableStateOf(init.tankId) }
    var useMonths by remember { mutableStateOf(init.intervalMonths > 0) }
    var interval by remember { mutableStateOf(if (init.intervalMonths > 0) init.intervalMonths.toString() else init.intervalDays.toString()) }
    var kind by remember { mutableIntStateOf(init.kind) }
    var times by remember { mutableStateOf(init.timeList.joinToString(", ") { it.fmtTime() }) }
    var logType by remember { mutableStateOf(init.logType) }
    var lastDone by remember { mutableLongStateOf(init.lastDone) }
    val intervalNum = interval.trim().toIntOrNull()

    EditDialog(
        title = if (init.id == 0L) "リマインドを追加" else "リマインドを編集",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank() && intervalNum != null && intervalNum > 0,
        onSave = {
            onSave(
                init.copy(
                    title = title.trim(), tankId = tankId,
                    intervalDays = if (useMonths) (intervalNum ?: 1) * 30 else (intervalNum ?: 7),
                    intervalMonths = if (useMonths) (intervalNum ?: 1) else 0,
                    logType = logType, lastDone = lastDone, kind = kind,
                    times = times.split(',', '、', ' ').mapNotNull { parseTime(it) }.distinct().sorted()
                        .joinToString(",") { it.fmtTime() }.ifBlank { null },
                )
            )
        },
        onDelete = onDelete,
    ) {
        TextInput("内容（例：換水、スキマー掃除）", title, { title = it })
        FieldLabel("水槽")
        ChoiceRow(listOf<Long?>(null) + tanks.map { it.id }, tankId, { tanks.nameOf(it) }, { tankId = it })
        FieldLabel("種類")
        ChoiceRow(listOf(KIND_ROUTINE, KIND_EQUIPMENT), kind, { if (it == KIND_ROUTINE) "毎日・短い周期" else "設備・消耗品" }, { kind = it })
        FieldLabel("周期の単位")
        ChoiceRow(listOf(false, true), useMonths, { if (it) "か月" else "日" }, { useMonths = it })
        NumField(if (useMonths) "何か月ごと" else "何日ごと", interval, { interval = it }, Modifier.fillMaxWidth(), integer = true)
        TextInput("通知する時刻（例 7:30, 19:00）", times, { times = it })
        Text(
            "「1日ごと」にすると、ホームの「今日やること」にチェック項目として出ます",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DateField("前回", lastDone, { if (it != null) lastDone = it })
        FieldLabel("この作業を記録したら自動で完了にする")
        ChoiceRow(listOf<String?>(null) + LOG_TYPES, logType, { it ?: "連動しない" }, {
            logType = it
            if (it != null && title.isBlank()) title = it
        })
    }
}
