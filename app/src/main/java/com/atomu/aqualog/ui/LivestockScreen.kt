package com.atomu.aqualog.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.atomu.aqualog.data.CATEGORIES
import com.atomu.aqualog.data.Livestock
import com.atomu.aqualog.data.STATUSES
import com.atomu.aqualog.data.STATUS_ALIVE
import com.atomu.aqualog.data.Tank
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LivestockScreen(vm: AppViewModel) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    val all by vm.livestock.collectAsStateWithLifecycle()
    val sel by vm.selectedTank.collectAsStateWithLifecycle()
    var showPast by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Livestock?>(null) }
    var viewing by remember { mutableStateOf<Long?>(null) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }

    val list = all
        .filter { (sel == null || it.tankId == sel) && (showPast || it.status == STATUS_ALIVE) }
        .sortedWith(
            compareBy<Livestock>({ it.status != STATUS_ALIVE }, { CATEGORIES.indexOf(it.category) })
                .thenBy { it.addedDate ?: Long.MAX_VALUE }
        )

    val detail = detailId?.let { id -> all.firstOrNull { it.id == id } }
    if (detail != null) {
        BackHandler { detailId = null }
        LivestockDetail(
            vm = vm,
            ls = detail,
            tanks = tanks,
            onBack = { detailId = null },
            onEdit = { editing = detail },
        )
    } else Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { TankFilter(tanks, sel, allowAll = true) { vm.selectedTank.value = it } }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("いなくなった生体も表示", modifier = Modifier.weight(1f))
                    Switch(checked = showPast, onCheckedChange = { showPast = it })
                }
            }
            if (list.isEmpty()) item { EmptyNote("右下の＋から生体を登録できます") }
            items(list, key = { it.id }) { item ->
                LivestockCard(
                    item, tanks, showTank = sel == null,
                    onPhotoClick = { viewing = item.id },
                ) { detailId = item.id }
            }
        }
        FloatingActionButton(
            onClick = {
                val id = sel ?: tanks.firstOrNull()?.id
                if (id != null) editing = Livestock(tankId = id, name = "", category = "魚", addedDate = today())
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "生体を登録") }
    }

    viewing?.let { id ->
        val withPhotos = list.filter { it.photoPath != null }
        PhotoViewer(
            items = withPhotos,
            tanks = tanks,
            startIndex = withPhotos.indexOfFirst { it.id == id },
            onDismiss = { viewing = null },
        )
    }

    editing?.let { item ->
        LivestockDialog(
            init = item,
            tanks = tanks,
            importPhoto = { vm.importPhoto(it) },
            discardPhoto = { vm.discardPhoto(it) },
            onDismiss = { editing = null },
            onSave = { vm.saveLivestock(it, item.photoPath); editing = null },
            onDelete = if (item.id != 0L) ({ vm.deleteLivestock(item); editing = null }) else null,
        )
    }
}

@Composable
fun Thumb(path: String?, name: String, size: Int, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val base = Modifier.size(size.dp).clip(RoundedCornerShape(2.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
    val mod = if (path != null && onClick != null) base.clickable(onClick = onClick) else base
    if (path != null) {
        AsyncImage(
            model = File(path),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = mod,
        )
    } else {
        Box(mod.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size((size * 0.62f).dp),
                )
            } else {
                Text(
                    name.take(1).ifEmpty { "?" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun LivestockCard(
    item: Livestock,
    tanks: List<Tank>,
    showTank: Boolean,
    onPhotoClick: () -> Unit,
    onClick: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumb(item.photoPath, item.name, 64, icon = iconFor(item.iconKind, item.category), onClick = onPhotoClick)
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (item.count > 1) Text("  ×${item.count}", style = MaterialTheme.typography.titleSmall)
                    if (item.status != STATUS_ALIVE) {
                        Text("  ${item.status}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val sub = buildList {
                    add(item.category)
                    if (showTank) add(tanks.nameOf(item.tankId))
                }.joinToString("・")
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.addedDate?.let { start ->
                    val end = if (item.status == STATUS_ALIVE) today() else (item.endDate ?: today())
                    Text("導入 ${start.fmtDate()}（${end - start}日）", style = MaterialTheme.typography.bodySmall)
                }
                val buy = buildList {
                    if (item.source.isNotBlank()) add(item.source)
                    item.price?.let { add("%,d円".format(it)) }
                }
                if (buy.isNotEmpty()) Text(buy.joinToString("・"), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LivestockDialog(
    init: Livestock,
    tanks: List<Tank>,
    importPhoto: suspend (Uri) -> String?,
    discardPhoto: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Livestock) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var tankId by remember { mutableLongStateOf(init.tankId) }
    var name by remember { mutableStateOf(init.name) }
    var category by remember { mutableStateOf(init.category) }
    var count by remember { mutableStateOf(init.count.toString()) }
    var added by remember { mutableStateOf(init.addedDate) }
    var source by remember { mutableStateOf(init.source) }
    var price by remember { mutableStateOf(init.price?.toString() ?: "") }
    var photo by remember { mutableStateOf(init.photoPath) }
    var status by remember { mutableStateOf(init.status) }
    var endDate by remember { mutableStateOf(init.endDate) }
    var note by remember { mutableStateOf(init.note) }
    var iconKind by remember { mutableStateOf(init.iconKind) }

    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val newPath = importPhoto(uri) ?: return@launch
                val old = photo
                if (old != null && old != init.photoPath) discardPhoto(old)
                photo = newPath
            }
        }
    }
    val cancel = {
        val p = photo
        if (p != null && p != init.photoPath) discardPhoto(p)
        onDismiss()
    }

    EditDialog(
        title = if (init.id == 0L) "生体を登録" else "生体を編集",
        onDismiss = cancel,
        saveEnabled = name.isNotBlank(),
        onSave = {
            onSave(
                init.copy(
                    tankId = tankId,
                    name = name.trim(),
                    category = category,
                    count = count.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    addedDate = added,
                    source = source.trim(),
                    price = price.trim().replace(",", "").toIntOrNull(),
                    photoPath = photo,
                    status = status,
                    endDate = if (status == STATUS_ALIVE) null else (endDate ?: today()),
                    note = note.trim(),
                    iconKind = iconKind,
                )
            )
        },
        onDelete = onDelete,
        deleteMessage = "「${init.name}」を台帳から削除しますか？\n（死亡・移動の記録として残すなら、状態を変更してください）",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumb(photo, name, 72, icon = iconFor(iconKind, category))
            Column(Modifier.padding(start = 8.dp)) {
                OutlinedButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (photo == null) "写真を選ぶ" else "写真を変える") }
                if (photo != null) {
                    TextButton(onClick = {
                        val p = photo
                        if (p != null && p != init.photoPath) discardPhoto(p)
                        photo = null
                    }) { Text("写真を外す") }
                }
            }
        }
        FieldLabel(if (photo == null) "アイコン（写真がないとき表示）" else "アイコン（写真を外したとき表示）")
        IconPicker(selected = iconKind ?: "", onSelect = { iconKind = it })
        TextInput("名前", name, { name = it })
        FieldLabel("水槽")
        ChoiceRow(tanks.map { it.id }, tankId, { tanks.nameOf(it) }, { tankId = it })
        FieldLabel("分類")
        ChoiceRow(CATEGORIES, category, { it }, { category = it })
        NumField("数", count, { count = it }, Modifier.fillMaxWidth(), integer = true)
        DateField("導入日", added, { added = it }, allowClear = true)
        TextInput("購入先", source, { source = it })
        NumField("値段（円）", price, { price = it }, Modifier.fillMaxWidth(), integer = true)
        FieldLabel("状態")
        ChoiceRow(STATUSES, status, { it }, { status = it })
        if (status != STATUS_ALIVE) {
            DateField("日付", endDate ?: today(), { endDate = it })
        }
        TextInput("メモ", note, { note = it }, singleLine = false)
    }
}

@Composable
private fun IconPicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(FISH_ICON_KEYS, OTHER_ICON_KEYS).forEach { keys ->
            keys.chunked(4).forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    rowKeys.forEach { k ->
                        val on = k == selected
                        Surface(
                            onClick = { onSelect(k) },
                            shape = RoundedCornerShape(2.dp),
                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(SPECIES_ICONS.getValue(k), contentDescription = null, modifier = Modifier.size(26.dp))
                                Text(
                                    if (k == "チョウチョウウオ") "チョウ" else k,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                    repeat(4 - rowKeys.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}
