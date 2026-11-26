package com.atomu.aqualog.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.atomu.aqualog.data.LogEntry
import com.atomu.aqualog.data.STATUS_ALIVE
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.data.WaterTest
import com.atomu.aqualog.data.fmtTime
import java.io.File

/** ホームの水槽カードから開く：水質の記録とグラフ、最近の作業 */
@Composable
fun TankDetail(vm: AppViewModel, tank: Tank, onBack: () -> Unit) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val tests by vm.tests.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val live by vm.livestock.collectAsStateWithLifecycle()
    var param by rememberSaveable { mutableStateOf(Param.NO3) }
    var editingTest by remember { mutableStateOf<WaterTest?>(null) }
    var editingLog by remember { mutableStateOf<LogEntry?>(null) }
    var viewing by remember { mutableStateOf(false) }
    var editingTank by remember { mutableStateOf(false) }

    val forTank = tests.filter { it.tankId == tank.id }
    val points = forTank.mapNotNull { t -> param.get(t)?.let { t.date to it } }
    val recent = logs.filter { it.tankId == tank.id }.take(12)
    val mono = FontFamily.Monospace

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "← ホーム",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onBack).padding(vertical = 6.dp),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(tank.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { editingTank = true }) { Text("編集") }
                }
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
            }
        }
        tank.photoPath?.let { path ->
            item {
                AsyncImage(
                    model = File(path),
                    contentDescription = "${tank.name}の写真",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable { viewing = true },
                )
            }
        }
        item {
            val info = buildList {
                tank.volumeL?.let { add("約${it.fmt()}L") }
                add("生体 ${live.filter { it.tankId == tank.id && it.status == STATUS_ALIVE }.sumOf { it.count }}")
                if (tank.lightOn != null && tank.lightOff != null) add("点灯 ${tank.lightOn.fmtTime()}〜${tank.lightOff.fmtTime()}")
            }.joinToString("／")
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tank.note.isNotBlank()) {
                    Text(tank.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 水質
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("水質", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { editingTest = WaterTest(tankId = tank.id, date = today()) }) { Text("＋ 記録") }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceRow(Param.entries.toList(), param, { it.label }, { param = it })
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(param.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (param.unit.isNotEmpty()) Text("（${param.unit}）", style = MaterialTheme.typography.bodySmall)
                    points.lastOrNull()?.let {
                        Text("　最新 ${it.second.fmt()}", fontFamily = mono, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }
                LineChart(points, Modifier.fillMaxWidth().height(180.dp))
            }
        }
        if (forTank.isEmpty()) item { EmptyNote("「＋ 記録」から測定結果を残せます") }
        items(forTank.reversed().take(10), key = { "w" + it.id }) { t ->
            Column(Modifier.fillMaxWidth().clickable { editingTest = t }) {
                Row(Modifier.padding(vertical = 8.dp)) {
                    Text(t.date.fmtShortY(), fontFamily = mono, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(t.summary().ifBlank { "（値なし）" }, style = MaterialTheme.typography.bodyMedium)
                        if (t.note.isNotBlank()) Text(t.note, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        // 最近の作業
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("最近の作業", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    editingLog = LogEntry(tankId = tank.id, date = today(), type = "換水", time = nowMinutes())
                }) { Text("＋ 記録") }
            }
        }
        if (recent.isEmpty()) item { EmptyNote("作業の記録はまだありません") }
        items(recent, key = { "l" + it.id }) { e ->
            Column(Modifier.fillMaxWidth().clickable { editingLog = e }) {
                Row(Modifier.padding(vertical = 8.dp)) {
                    Text(
                        e.date.fmtShort() + (e.time?.let { " " + it.fmtTime() } ?: ""),
                        fontFamily = mono, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        listOf(e.type, e.amount, e.note).filter { it.isNotBlank() }.joinToString("　"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    editingTest?.let { t ->
        WaterDialog(
            init = t, tanks = tanks,
            onDismiss = { editingTest = null },
            onSave = { vm.saveTest(it); editingTest = null },
            onDelete = if (t.id != 0L) ({ vm.deleteTest(t); editingTest = null }) else null,
        )
    }
    editingLog?.let { e ->
        LogDialog(
            init = e, tanks = tanks,
            onDismiss = { editingLog = null },
            onSave = { vm.saveLog(it); editingLog = null },
            onDelete = if (e.id != 0L) ({ vm.deleteLog(e); editingLog = null }) else null,
        )
    }
    if (viewing && tank.photoPath != null) SinglePhotoDialog(tank.photoPath, tank.name) { viewing = false }
    if (editingTank) TankEditDialog(vm, tank) { editingTank = false }
}
