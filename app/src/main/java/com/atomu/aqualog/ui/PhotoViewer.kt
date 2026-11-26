package com.atomu.aqualog.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.atomu.aqualog.data.Livestock
import com.atomu.aqualog.data.STATUS_ALIVE
import com.atomu.aqualog.data.Tank
import java.io.File

/** 写真を全画面で表示。左右スワイプで次の生体、ピンチ・ダブルタップで拡大 */
@Composable
fun PhotoViewer(items: List<Livestock>, tanks: List<Tank>, startIndex: Int, onDismiss: () -> Unit) {
    if (items.isEmpty()) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, items.size - 1)) { items.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                key = { items[it].id },
            ) { page ->
                ZoomableImage(items[page].photoPath ?: "", items[page].name)
            }

            val cur = items[pager.currentPage]
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    cur.name + if (cur.count > 1) "  ×${cur.count}" else "",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                val info = buildList {
                    add(cur.category)
                    add(tanks.nameOf(cur.tankId))
                    if (cur.status != STATUS_ALIVE) add(cur.status)
                }.joinToString("・")
                Text(info, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                cur.addedDate?.let { start ->
                    val end = if (cur.status == STATUS_ALIVE) today() else (cur.endDate ?: today())
                    Text(
                        "導入 ${start.fmtDate()}（${end - start}日）",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (cur.note.isNotBlank()) {
                    Text(cur.note, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (items.size > 1) {
                Text(
                    "${pager.currentPage + 1} / ${items.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "閉じる", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ZoomableImage(path: String, name: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + pan * scale
    }
    AsyncImage(
        model = File(path),
        contentDescription = name,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                    }
                })
            }
            // 拡大していないときは1本指の横スワイプをページ送りに回す
            .transformable(state = state, canPan = { scale > 1f })
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

/** 1枚だけ全画面で見る */
@Composable
fun SinglePhotoDialog(path: String, title: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ZoomableImage(path, title)
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "閉じる", tint = Color.White)
            }
        }
    }
}

/** 横長の写真を選ぶ欄（水槽用）。保存前に差し替えた写真はキャンセル時に消す */
@Composable
fun WidePhotoPicker(
    photo: String?,
    originalPath: String?,
    importPhoto: suspend (Uri) -> String?,
    discardPhoto: (String) -> Unit,
    onChange: (String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val newPath = importPhoto(uri) ?: return@launch
                if (photo != null && photo != originalPath) discardPhoto(photo)
                onChange(newPath)
            }
        }
    }
    Column {
        if (photo != null) {
            AsyncImage(
                model = File(photo),
                contentDescription = "水槽の写真",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
        Row {
            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (photo == null) "写真を選ぶ" else "写真を変える") }
            if (photo != null) {
                TextButton(onClick = {
                    if (photo != originalPath) discardPhoto(photo)
                    onChange(null)
                }) { Text("写真を外す") }
            }
        }
    }
}
