package com.atomu.aqualog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.atomu.aqualog.data.Livestock
import com.atomu.aqualog.data.LivestockNote
import com.atomu.aqualog.data.LivestockPhoto
import com.atomu.aqualog.data.NOTE_TAGS
import com.atomu.aqualog.data.STATUS_ALIVE
import com.atomu.aqualog.data.Tank
import kotlinx.coroutines.launch
import java.io.File

/** 比較・一覧用の写真（台帳の写真 or 成長記録） */
private data class Shot(val path: String, val date: Long?, val record: LivestockPhoto?)

@Composable
fun LivestockDetail(
    vm: AppViewModel,
    ls: Livestock,
    tanks: List<Tank>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val gallery by vm.gallery.collectAsStateWithLifecycle()
    val allNotes by vm.notes.collectAsStateWithLifecycle()
    val notes = allNotes.filter { it.livestockId == ls.id }
    val shots = buildList {
        ls.photoPath?.let { add(Shot(it, ls.addedDate, null)) }
        gallery.filter { it.livestockId == ls.id }.forEach { add(Shot(it.path, it.date, it)) }
    }.sortedBy { it.date ?: Long.MIN_VALUE }

    var viewing by remember { mutableStateOf<Shot?>(null) }
    var editingNote by remember { mutableStateOf<LivestockNote?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            vm.importPhoto(uri)?.let { vm.addGalleryPhoto(ls.id, it, today()) }
        }
    }
    val mono = FontFamily.Monospace

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "← 生体台帳",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onBack).padding(vertical = 6.dp),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(ls.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            ls.category + if (ls.status != STATUS_ALIVE) "・${ls.status}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(2.dp)) { Text("編集") }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
            }
        }
        item {
            val end = if (ls.status == STATUS_ALIVE) today() else (ls.endDate ?: today())
            Row(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)) {
                Stat("水槽", tanks.nameOf(ls.tankId), Modifier.weight(1f))
                Stat("導入", ls.addedDate?.fmtShortY() ?: "—", Modifier.weight(1f))
                Stat("飼育", ls.addedDate?.let { "${end - it}日" } ?: "—", Modifier.weight(1f))
                Stat("数", ls.count.toString(), Modifier.weight(0.6f), last = true)
            }
        }

        // 成長記録
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("成長記録", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("＋ 写真を追加") }
            }
        }
        item {
            if (shots.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().height(120.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconFor(ls.iconKind, ls.category), contentDescription = null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val first = shots.first()
                val last = shots.last()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompareShot(first, if (shots.size > 1) "導入時" else "写真", Modifier.weight(1f)) { viewing = first }
                    if (shots.size > 1) CompareShot(last, "最新", Modifier.weight(1f)) { viewing = last }
                    else Box(Modifier.weight(1f))
                }
            }
        }
        if (shots.size > 2) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(shots) { s ->
                        AsyncImage(
                            model = File(s.path),
                            contentDescription = "写真 ${s.date?.fmtShortY() ?: ""}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(2.dp)).clickable { viewing = s },
                        )
                    }
                }
            }
        }

        // メモ
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("メモ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        editingNote = LivestockNote(livestockId = ls.id, date = today(), tag = NOTE_TAGS.first(), text = "")
                    }) { Text("＋ 追加") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
        if (ls.note.isNotBlank() || ls.source.isNotBlank() || ls.price != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val buy = buildList {
                        if (ls.source.isNotBlank()) add(ls.source)
                        ls.price?.let { add("%,d円".format(it)) }
                    }
                    if (buy.isNotEmpty()) Text("購入：" + buy.joinToString("・"), style = MaterialTheme.typography.bodySmall)
                    if (ls.note.isNotBlank()) Text(ls.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (notes.isEmpty()) {
            item { EmptyNote("混泳の相性や餌の食いつきを残せます") }
        }
        items(notes, key = { it.id }) { n ->
            Column(Modifier.fillMaxWidth().clickable { editingNote = n }) {
                Row(Modifier.padding(vertical = 9.dp)) {
                    Text(n.date.fmtShort(), fontFamily = mono, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp).size(width = 44.dp, height = 18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(n.tag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(n.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    viewing?.let { s ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(
                        model = File(s.path),
                        contentDescription = ls.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    s.date?.let { Text(it.fmtDate(), style = MaterialTheme.typography.bodySmall) }
                    if (s.record == null) {
                        Text("台帳の写真です（編集から変更できます）", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("閉じる") } },
            dismissButton = {
                s.record?.let { rec ->
                    TextButton(onClick = { vm.deleteGalleryPhoto(rec); viewing = null }) {
                        Text("この写真を削除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }

    editingNote?.let { n ->
        var tag by remember(n) { mutableStateOf(n.tag) }
        var text by remember(n) { mutableStateOf(n.text) }
        var date by remember(n) { mutableStateOf(n.date) }
        EditDialog(
            title = if (n.id == 0L) "メモを追加" else "メモを編集",
            onDismiss = { editingNote = null },
            saveEnabled = text.isNotBlank(),
            onSave = { vm.saveNote(n.copy(tag = tag, text = text.trim(), date = date)); editingNote = null },
            onDelete = if (n.id != 0L) ({ vm.deleteNote(n); editingNote = null }) else null,
        ) {
            FieldLabel("種類")
            ChoiceRow(NOTE_TAGS, tag, { it }, { tag = it })
            DateField("日付", date, { if (it != null) date = it })
            TextInput("内容", text, { text = it }, singleLine = false)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, last: Boolean = false) {
    Row(modifier) {
        Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        if (!last) Box(Modifier.size(width = 1.dp, height = 52.dp).background(MaterialTheme.colorScheme.outline))
    }
}

@Composable
private fun CompareShot(s: Shot, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AsyncImage(
            model = File(s.path),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
                .clickable(onClick = onClick),
        )
        Text(
            "$label ${s.date?.fmtShortY() ?: ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
