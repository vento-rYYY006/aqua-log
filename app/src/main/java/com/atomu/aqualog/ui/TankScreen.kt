package com.atomu.aqualog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atomu.aqualog.data.Tank

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TankScreen(vm: AppViewModel, onBack: () -> Unit) {
    val tanks by vm.tanks.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Tank?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水槽の設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "戻る") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Tank(name = "", sortOrder = tanks.size) }) {
                Icon(Icons.Filled.Add, contentDescription = "水槽を追加")
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(tanks, key = { it.id }) { t ->
                    val vol = t.volumeL?.let { "約${it.fmt()}L" } ?: ""
                    val thumb: (@Composable () -> Unit)? = t.photoPath?.let { path ->
                        {
                            AsyncImage(
                                model = File(path),
                                contentDescription = t.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                    ListItem(
                        leadingContent = thumb,
                        headlineContent = { Text(t.name) },
                        supportingContent = { Text(listOf(vol, t.note).filter { it.isNotBlank() }.joinToString("・")) },
                        modifier = Modifier.fillMaxWidth().clickable { editing = t },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    editing?.let { t ->
        var name by remember(t) { mutableStateOf(t.name) }
        var volume by remember(t) { mutableStateOf(t.volumeL?.fmt() ?: "") }
        var note by remember(t) { mutableStateOf(t.note) }
        var photo by remember(t) { mutableStateOf(t.photoPath) }
        EditDialog(
            title = if (t.id == 0L) "水槽を追加" else "水槽を編集",
            onDismiss = {
                val p = photo
                if (p != null && p != t.photoPath) vm.discardPhoto(p)
                editing = null
            },
            saveEnabled = name.isNotBlank(),
            onSave = {
                vm.saveTank(
                    t.copy(name = name.trim(), volumeL = volume.toNum(), note = note.trim(), photoPath = photo),
                    previousPhoto = t.photoPath,
                )
                editing = null
            },
            onDelete = if (t.id != 0L) ({ vm.deleteTank(t); editing = null }) else null,
            deleteMessage = "「${t.name}」を削除しますか？\nこの水槽の作業・水質・生体・リマインドの記録もすべて消えます。",
        ) {
            WidePhotoPicker(
                photo = photo,
                originalPath = t.photoPath,
                importPhoto = { vm.importPhoto(it) },
                discardPhoto = { vm.discardPhoto(it) },
                onChange = { photo = it },
            )
            TextInput("名前", name, { name = it })
            NumField("水量（L）", volume, { volume = it }, Modifier.fillMaxWidth())
            TextInput("メモ（機材など）", note, { note = it }, singleLine = false)
        }
    }
}
