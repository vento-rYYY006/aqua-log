package com.atomu.aqualog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 線画アイコン（24×24）をパス文字列から作る */
private fun lineIcon(name: String, path: String, width: Float = 1.6f): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = addPathNodes(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()

/** 目の点（小さな円） */
private fun dot(cx: Float, cy: Float, r: Float = 0.6f) = " M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0"

/** 生体アイコン：表示名 → アイコン */
val SPECIES_ICONS: Map<String, ImageVector> = linkedMapOf(
    "クマノミ" to lineIcon("クマノミ",
        "M2.5 12c3-5.5 10.5-6.5 15 0-4.5 6.5-12 5.5-15 0z M17.5 12l4.5-3.5v7z M7.5 8.3v7.4 M12.5 7.6v8.8" + dot(5.2f, 11.2f)),
    "ハギ" to lineIcon("ハギ",
        "M3 12c2.5-7 11-8 14.5 0-3.5 8-12 7-14.5 0z M17.5 12c1.5-2.5 3-4 4.5-4.5v9c-1.5-0.5-3-2-4.5-4.5z M14.5 13.5l1.5 0.6 M7 6.5c2-1.5 5-1.5 7 0.3" + dot(6f, 10.8f)),
    "ヤッコ" to lineIcon("ヤッコ",
        "M4 12c0-4.5 3.5-7 7.5-7S18 8 18 12s-2.5 7-6.5 7S4 16.5 4 12z M18 10.5l3.5-1.5v6l-3.5-1.5 M9 5.6v12.8" + dot(6.6f, 10.8f)),
    "チョウチョウウオ" to lineIcon("チョウチョウウオ",
        "M6 12c0-4.5 3-7.5 7-7.5s7 3 7 7.5-3 7.5-7 7.5-7-3-7-7.5z M6 12.2H2.5 M9.5 6.3l1.5 11.4" + dot(15.5f, 15f, 1.3f)),
    "ハゼ" to lineIcon("ハゼ",
        "M2 14.5c4-3.5 13-3.5 17 0-4 2.8-13 2.8-17 0z M19 14.5l3-2.5v5z M8 11.9l1.5-4.4 3 4.2" + dot(4.6f, 13.9f)),
    "ベラ" to lineIcon("ベラ",
        "M2 12c5-3.2 13-3.2 17.5 0-4.5 3.2-12.5 3.2-17.5 0z M19.5 12l3-3v6z M5 12h12" + dot(4.4f, 11.2f)),
    "ゴンベ" to lineIcon("ゴンベ",
        "M3 13c2.5-5 10-5.5 13.5 0-3.5 5-11 4.5-13.5 0z M16.5 13l4-3v6z M7 9.2V7.2 M9.5 8.6V6.4 M12 8.8V6.8 M8.5 15.8l-1 3.2 M11 16l0.5 3" + dot(5.6f, 12.3f)),
    "エビ" to lineIcon("エビ",
        "M5 16.5c0.5-5 5-8 10.5-7.5 3 0.3 3.8 3 1.6 4.8-3.3 2.8-8 4-12.1 2.7z M5 16.5l-2.5 2 M5 16.5l-2.5-0.8 " +
            "M16.8 9.5c2-3 3.8-4.5 5.7-5 M15.8 9.1c0.7-3 2-5 3.5-6.4 M9 15.8l-0.5 2.5 M11.5 15.2l-0.3 2.6 M14 14.2l0.2 2.5"),
    "カニ" to lineIcon("カニ",
        "M6 14a6 4 0 1 0 12 0a6 4 0 1 0 -12 0 M6.5 12.5L4 9.5 M17.5 12.5L20 9.5 " +
            "M4 9.5c-1.5-0.5-1.8-2.6-0.3-3.4 M4 9.5c1-0.2 1.8-1.1 1.6-2.2 M20 9.5c1.5-0.5 1.8-2.6 0.3-3.4 M20 9.5c-1-0.2-1.8-1.1-1.6-2.2 " +
            "M7 16.5l-2.5 2.5 M17 16.5l2.5 2.5 M8.5 17.6l-1.3 2.6 M15.5 17.6l1.3 2.6"),
    "貝" to lineIcon("貝",
        "M3 18.5h15c2 0 3.2-2 2.3-3.9C18.2 9.8 12.5 7.5 8.5 10c-3 1.9-2.4 6.3 0.9 6.5 2.4 0.1 3.4-2.4 2-3.7 M3 18.5l2.5-2"),
    "サンゴ" to lineIcon("サンゴ",
        "M12 20.5V11 M12 14.5L8 10.5V6 M12 12.5l4-4V4.5 M8 10.5L5 8.5 M16 8.5l3-1.2 M5 20.5h14"),
)

val FISH_ICON_KEYS = listOf("クマノミ", "ハギ", "ヤッコ", "チョウチョウウオ", "ハゼ", "ベラ", "ゴンベ")
val OTHER_ICON_KEYS = listOf("エビ", "カニ", "貝", "サンゴ")

/** アイコン未設定のときは分類から選ぶ */
fun iconFor(kind: String?, category: String): ImageVector {
    kind?.let { k -> SPECIES_ICONS[k]?.let { return it } }
    val fallback = when (category) {
        "エビ・カニ" -> "エビ"
        "貝" -> "貝"
        "サンゴ", "イソギンチャク" -> "サンゴ"
        else -> "ベラ"
    }
    return SPECIES_ICONS.getValue(fallback)
}

val WarnIcon: ImageVector = lineIcon("注意", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0 M12 7v5.5 M12 16.5v0.01", 2f)
val MoonIcon: ImageVector = lineIcon("ナイトモード", "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z", 1.8f)
val SunIcon: ImageVector = lineIcon(
    "ライトモード",
    "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0 M12 2.5v2 M12 19.5v2 M2.5 12h2 M19.5 12h2 M5.3 5.3l1.4 1.4 M17.3 17.3l1.4 1.4 M5.3 18.7l1.4-1.4 M17.3 6.7l1.4-1.4",
    1.8f,
)
val AutoIcon: ImageVector = lineIcon("自動", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0 M12 3v18 M12 3a9 9 0 0 1 0 18", 1.8f)

/** やさしい警告表示（オレンジ＋アイコン＋薄い背景） */
@Composable
fun WarnBadge(text: String, modifier: Modifier = Modifier) {
    val w = LocalWarn.current
    Row(
        modifier = modifier.background(w.bg).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(WarnIcon, contentDescription = null, tint = w.text, modifier = Modifier.size(14.dp))
        Text(text, color = w.text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
