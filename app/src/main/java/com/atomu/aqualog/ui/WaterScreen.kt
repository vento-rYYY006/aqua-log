package com.atomu.aqualog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.data.WaterTest

@Composable
fun WaterScreen(vm: AppViewModel) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val tests by vm.tests.collectAsStateWithLifecycle()
    val sel by vm.selectedTank.collectAsStateWithLifecycle()
    var param by rememberSaveable { mutableStateOf(Param.SAL) }
    var editing by remember { mutableStateOf<WaterTest?>(null) }

    val tankId = sel ?: tanks.firstOrNull()?.id
    val forTank = tests.filter { it.tankId == tankId }
    val points = forTank.mapNotNull { t -> param.get(t)?.let { t.date to it } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)) {
            item { TankFilter(tanks, tankId, allowAll = false) { vm.selectedTank.value = it } }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceRow(Param.entries.toList(), param, { it.label }, { param = it })
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(param.label, style = MaterialTheme.typography.titleMedium)
                            if (param.unit.isNotEmpty()) {
                                Text("（${param.unit}）", style = MaterialTheme.typography.bodySmall)
                            }
                            points.lastOrNull()?.let {
                                Text(
                                    "　最新 ${it.second.fmt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        LineChart(points, Modifier.fillMaxWidth().height(200.dp))
                    }
                }
            }
            item {
                Text(
                    "測定履歴",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }
            if (forTank.isEmpty()) item { EmptyNote("右下の＋から測定結果を記録できます") }
            items(forTank.reversed(), key = { it.id }) { t ->
                val support: (@Composable () -> Unit)? =
                    if (t.note.isNotBlank()) { { Text(t.note) } } else null
                ListItem(
                    overlineContent = { Text(t.date.fmtDate()) },
                    headlineContent = { Text(t.summary().ifBlank { "（値なし）" }) },
                    supportingContent = support,
                    modifier = Modifier.clickable { editing = t },
                )
                HorizontalDivider()
            }
        }
        FloatingActionButton(
            onClick = { if (tankId != null) editing = WaterTest(tankId = tankId, date = today()) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "測定結果を記録") }
    }

    editing?.let { t ->
        WaterDialog(
            init = t,
            tanks = tanks,
            onDismiss = { editing = null },
            onSave = { vm.saveTest(it); editing = null },
            onDelete = if (t.id != 0L) ({ vm.deleteTest(t); editing = null }) else null,
        )
    }
}

@Composable
private fun WaterDialog(
    init: WaterTest,
    tanks: List<Tank>,
    onDismiss: () -> Unit,
    onSave: (WaterTest) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var tankId by remember { mutableLongStateOf(init.tankId) }
    var date by remember { mutableLongStateOf(init.date) }
    var temp by remember { mutableStateOf(init.temp?.fmt() ?: "") }
    var sal by remember { mutableStateOf(init.salinity?.fmt() ?: "") }
    var no3 by remember { mutableStateOf(init.no3?.fmt() ?: "") }
    var po4 by remember { mutableStateOf(init.po4?.fmt() ?: "") }
    var kh by remember { mutableStateOf(init.kh?.fmt() ?: "") }
    var ca by remember { mutableStateOf(init.ca?.fmt() ?: "") }
    var mg by remember { mutableStateOf(init.mg?.fmt() ?: "") }
    var note by remember { mutableStateOf(init.note) }

    val hasValue = listOf(temp, sal, no3, po4, kh, ca, mg).any { it.toNum() != null }

    EditDialog(
        title = if (init.id == 0L) "水質を記録" else "水質を編集",
        onDismiss = onDismiss,
        saveEnabled = hasValue,
        onSave = {
            onSave(
                init.copy(
                    tankId = tankId, date = date,
                    temp = temp.toNum(), salinity = sal.toNum(), no3 = no3.toNum(), po4 = po4.toNum(),
                    kh = kh.toNum(), ca = ca.toNum(), mg = mg.toNum(), note = note.trim(),
                )
            )
        },
        onDelete = onDelete,
    ) {
        FieldLabel("水槽")
        ChoiceRow(tanks.map { it.id }, tankId, { tanks.nameOf(it) }, { tankId = it })
        DateField("日付", date, { if (it != null) date = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("比重", sal, { sal = it }, Modifier.weight(1f))
            NumField("水温 ℃", temp, { temp = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("NO3", no3, { no3 = it }, Modifier.weight(1f))
            NumField("PO4", po4, { po4 = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("KH", kh, { kh = it }, Modifier.weight(1f))
            NumField("Ca", ca, { ca = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Mg", mg, { mg = it }, Modifier.weight(1f))
            Box(Modifier.weight(1f))
        }
        TextInput("メモ", note, { note = it }, singleLine = false)
    }
}
