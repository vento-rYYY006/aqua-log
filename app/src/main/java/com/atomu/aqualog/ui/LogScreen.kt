package com.atomu.aqualog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atomu.aqualog.data.LOG_TYPES
import com.atomu.aqualog.data.LogEntry
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.data.fmtTime
import com.atomu.aqualog.data.parseTime

@Composable
fun LogScreen(vm: AppViewModel) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val sel by vm.selectedTank.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<LogEntry?>(null) }
    val list = logs.filter { sel == null || it.tankId == sel }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)) {
            item { TankFilter(tanks, sel, allowAll = true) { vm.selectedTank.value = it } }
            if (list.isEmpty()) item { EmptyNote("右下の＋から作業を記録できます") }
            items(list, key = { it.id }) { e ->
                val support: (@Composable () -> Unit)? =
                    if (e.note.isNotBlank()) { { Text(e.note) } } else null
                ListItem(
                    overlineContent = { Text(e.date.fmtDate() + "・" + tanks.nameOf(e.tankId)) },
                    headlineContent = { Text(e.type + if (e.amount.isNotBlank()) "　" + e.amount else "") },
                    supportingContent = support,
                    modifier = Modifier.clickable { editing = e },
                )
                HorizontalDivider()
            }
        }
        FloatingActionButton(
            onClick = {
                val id = sel ?: tanks.firstOrNull()?.id
                if (id != null) editing = LogEntry(tankId = id, date = today(), type = "換水")
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "作業を記録") }
    }

    editing?.let { e ->
        LogDialog(
            init = e,
            tanks = tanks,
            onDismiss = { editing = null },
            onSave = { vm.saveLog(it); editing = null },
            onDelete = if (e.id != 0L) ({ vm.deleteLog(e); editing = null }) else null,
        )
    }
}

@Composable
internal fun LogDialog(
    init: LogEntry,
    tanks: List<Tank>,
    onDismiss: () -> Unit,
    onSave: (LogEntry) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var tankId by remember { mutableLongStateOf(init.tankId) }
    var date by remember { mutableLongStateOf(init.date) }
    var type by remember { mutableStateOf(init.type) }
    var amount by remember { mutableStateOf(init.amount) }
    var note by remember { mutableStateOf(init.note) }
    var time by remember { mutableStateOf(init.time?.fmtTime() ?: "") }

    EditDialog(
        title = if (init.id == 0L) "作業を記録" else "作業を編集",
        onDismiss = onDismiss,
        onSave = {
            onSave(init.copy(tankId = tankId, date = date, type = type, amount = amount.trim(), note = note.trim(), time = parseTime(time)))
        },
        onDelete = onDelete,
    ) {
        FieldLabel("水槽")
        ChoiceRow(tanks.map { it.id }, tankId, { tanks.nameOf(it) }, { tankId = it })
        DateField("日付", date, { if (it != null) date = it })
        TextInput("時刻（例 19:00・空でもよい）", time, { time = it })
        FieldLabel("作業")
        ChoiceRow(LOG_TYPES, type, { it }, { type = it })
        TextInput("量・内容（例：10L、ウールマット交換）", amount, { amount = it })
        TextInput("メモ", note, { note = it }, singleLine = false)
    }
}
