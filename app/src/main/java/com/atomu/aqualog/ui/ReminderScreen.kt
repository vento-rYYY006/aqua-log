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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atomu.aqualog.data.nextDue
import com.atomu.aqualog.data.LOG_TYPES
import com.atomu.aqualog.data.Reminder
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.notify.ReminderWorker

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

    val sorted = reminders.sortedWith(compareBy<Reminder>({ !it.enabled }, { it.nextDue }))

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            if (sorted.isEmpty()) item { EmptyNote("右下の＋からリマインドを追加できます") }
            items(sorted, key = { it.id }) { r ->
                ReminderCard(
                    r, tanks,
                    onDone = { vm.completeReminder(r) },
                    onToggle = { vm.saveReminder(r.copy(enabled = it)) },
                    onClick = { editing = r },
                )
            }
        }
        FloatingActionButton(
            onClick = { editing = Reminder(title = "", intervalDays = 7, lastDone = today()) },
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
        else -> "${-left}日超過"
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
                    "${r.intervalDays}日ごと・次回 ${r.nextDue.fmtDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (r.enabled) {
                    Text(
                        dueText(r),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (onToggle != null) {
                Switch(checked = r.enabled, onCheckedChange = onToggle, modifier = Modifier.padding(horizontal = 8.dp))
            }
            if (r.enabled) {
                FilledTonalButton(onClick = onDone) { Text("完了") }
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
    var interval by remember { mutableStateOf(init.intervalDays.toString()) }
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
                    title = title.trim(), tankId = tankId, intervalDays = intervalNum ?: 7,
                    logType = logType, lastDone = lastDone,
                )
            )
        },
        onDelete = onDelete,
    ) {
        TextInput("内容（例：換水、スキマー掃除）", title, { title = it })
        FieldLabel("水槽")
        ChoiceRow(listOf<Long?>(null) + tanks.map { it.id }, tankId, { tanks.nameOf(it) }, { tankId = it })
        NumField("何日ごと", interval, { interval = it }, Modifier.fillMaxWidth(), integer = true)
        DateField("前回", lastDone, { if (it != null) lastDone = it })
        FieldLabel("この作業を記録したら自動で完了にする")
        ChoiceRow(listOf<String?>(null) + LOG_TYPES, logType, { it ?: "連動しない" }, {
            logType = it
            if (it != null && title.isBlank()) title = it
        })
    }
}
