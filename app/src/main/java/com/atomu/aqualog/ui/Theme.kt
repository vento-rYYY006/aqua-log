package com.atomu.aqualog.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/** ログブック風：生成りの紙＋インク */
private val Light = lightColorScheme(
    primary = Color(0xFF1F2A2E),
    onPrimary = Color(0xFFF4EFE4),
    primaryContainer = Color(0xFFE6DDC9),
    onPrimaryContainer = Color(0xFF1F2A2E),
    secondary = Color(0xFF1F5F8B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBE3D1),
    onSecondaryContainer = Color(0xFF1F2A2E),
    tertiary = Color(0xFF1F5F8B),
    background = Color(0xFFF4EFE4),
    onBackground = Color(0xFF1F2A2E),
    surface = Color(0xFFF4EFE4),
    onSurface = Color(0xFF1F2A2E),
    surfaceVariant = Color(0xFFEBE3D1),
    onSurfaceVariant = Color(0xFF5B574D),
    surfaceContainerLowest = Color(0xFFFAF7F0),
    surfaceContainerLow = Color(0xFFF7F2E9),
    surfaceContainer = Color(0xFFEFE8DA),
    surfaceContainerHigh = Color(0xFFEBE3D1),
    surfaceContainerHighest = Color(0xFFE6DDC9),
    outline = Color(0xFF1F2A2E),
    outlineVariant = Color(0xFFD6CCB7),
    error = Color(0xFFA83E26),
    errorContainer = Color(0xFFF6DDD3),
    onErrorContainer = Color(0xFF4A1508),
)

/** ナイトモード：夜の水槽前でまぶしくない紺 */
private val Night = darkColorScheme(
    primary = Color(0xFFE6E1D6),
    onPrimary = Color(0xFF0F1A22),
    primaryContainer = Color(0xFF22323D),
    onPrimaryContainer = Color(0xFFE6E1D6),
    secondary = Color(0xFF8CC4EA),
    onSecondary = Color(0xFF0F1A22),
    secondaryContainer = Color(0xFF1A2833),
    onSecondaryContainer = Color(0xFFE6E1D6),
    tertiary = Color(0xFF8CC4EA),
    background = Color(0xFF0F1A22),
    onBackground = Color(0xFFE6E1D6),
    surface = Color(0xFF0F1A22),
    onSurface = Color(0xFFE6E1D6),
    surfaceVariant = Color(0xFF1A2833),
    onSurfaceVariant = Color(0xFF9CA8AD),
    surfaceContainerLowest = Color(0xFF0B141A),
    surfaceContainerLow = Color(0xFF13202A),
    surfaceContainer = Color(0xFF16242E),
    surfaceContainerHigh = Color(0xFF1A2833),
    surfaceContainerHighest = Color(0xFF22323D),
    outline = Color(0xFF6B7A83),
    outlineVariant = Color(0xFF2A3A45),
    error = Color(0xFFF2A08A),
    errorContainer = Color(0xFF4A2418),
    onErrorContainer = Color(0xFFF6DDD3),
)

private val LogbookShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)

/** 期限切れなどの「やさしい警告」の色（赤ではなくオレンジ寄り） */
data class WarnColors(val text: Color, val bg: Color)

val LocalWarn = staticCompositionLocalOf { WarnColors(Color(0xFF9A5200), Color(0xFFFBE9CF)) }
val LocalNight = staticCompositionLocalOf { false }

enum class ThemeMode(val label: String) { AUTO("自動"), LIGHT("ライト"), NIGHT("ナイト") }

/** 表示モードの保存（自動／ライト／ナイト） */
object ThemePrefs {
    private const val FILE = "settings"
    private const val KEY = "themeMode"
    val mode = MutableStateFlow(ThemeMode.AUTO)

    fun load(context: Context) {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY, 0)
        mode.value = ThemeMode.entries.getOrElse(v) { ThemeMode.AUTO }
    }

    fun set(context: Context, m: ThemeMode) {
        mode.value = m
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(KEY, m.ordinal).apply()
    }
}

@Composable
fun isNight(): Boolean {
    val mode by ThemePrefs.mode.collectAsStateWithLifecycle()
    return when (mode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.NIGHT -> true
    }
}

@Composable
fun AquaTheme(content: @Composable () -> Unit) {
    val night = isNight()
    val warn = if (night) WarnColors(Color(0xFFF0A956), Color(0xFF3A2A14)) else WarnColors(Color(0xFF9A5200), Color(0xFFFBE9CF))
    CompositionLocalProvider(LocalWarn provides warn, LocalNight provides night) {
        MaterialTheme(
            colorScheme = if (night) Night else Light,
            shapes = LogbookShapes,
            content = content,
        )
    }
}

@Composable
fun rememberAppContext(): Context = LocalContext.current.applicationContext
