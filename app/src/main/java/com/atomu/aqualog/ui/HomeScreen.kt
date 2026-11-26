package com.atomu.aqualog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atomu.aqualog.data.nextDue
import com.atomu.aqualog.data.Livestock
import com.atomu.aqualog.data.LogEntry
import com.atomu.aqualog.data.STATUS_ALIVE
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.data.WaterTest

@Composable
fun HomeScreen(vm: AppViewModel) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val tests by vm.tests.collectAsStateWithLifecycle()
    val live by vm.livestock.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val t = today()
    var viewing by remember { mutableStateOf<Tank?>(null) }
    viewing?.let { tk ->
        tk.photoPath?.let { SinglePhotoDialog(it, tk.name) { viewing = null } }
    }
    val upcoming = reminders.filter { it.enabled && it.nextDue <= t + 1 }.sortedBy { it.nextDue }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("今日・明日の作業", style = MaterialTheme.typography.titleMedium) }
        if (upcoming.isEmpty()) {
            item { Text("予定の作業はありません", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(upcoming, key = { "r" + it.id }) { r ->
                ReminderCard(r, tanks, onDone = { vm.completeReminder(r) })
            }
        }
        item { Text("水槽", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        items(tanks, key = { "t" + it.id }) { tank ->
            TankSummary(
                tank = tank,
                logs = logs.filter { it.tankId == tank.id },
                tests = tests.filter { it.tankId == tank.id },
                live = live.filter { it.tankId == tank.id && it.status == STATUS_ALIVE },
                onPhotoClick = { viewing = tank },
            )
        }
    }
}

@Composable
private fun TankSummary(
    tank: Tank,
    logs: List<LogEntry>,
    tests: List<WaterTest>,
    live: List<Livestock>,
    onPhotoClick: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        tank.photoPath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = tank.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp).clickable(onClick = onPhotoClick),
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(tank.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                tank.volumeL?.let {
                    Text(
                        "  約${it.fmt()}L",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            val byCat = live.groupBy { it.category }.map { (c, l) -> "$c ${l.sumOf { it.count }}" }
            Line("生体", if (byCat.isEmpty()) "未登録" else byCat.joinToString("・"))

            val last = tests.lastOrNull()
            Line("水質", if (last == null) "未測定" else "${daysAgo(last.date)}　${last.summary()}")

            val lastChange = logs.firstOrNull { it.type == "換水" }
            Line("換水", lastChange?.let { daysAgo(it.date) + if (it.amount.isNotBlank()) "（${it.amount}）" else "" } ?: "記録なし")

            val lastLog = logs.firstOrNull()
            Line("最新", lastLog?.let { "${daysAgo(it.date)}　${it.type}" } ?: "記録なし")
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
