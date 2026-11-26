package com.atomu.aqualog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel

private data class TabDef(val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabDef("ホーム", Icons.Filled.Home),
    TabDef("作業", Icons.Filled.Build),
    TabDef("水質", Icons.Filled.Info),
    TabDef("生体", SPECIES_ICONS.getValue("クマノミ")),
    TabDef("リマインド", Icons.Filled.Notifications),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquaRoot(vm: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showTanks by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val mode by ThemePrefs.mode.collectAsStateWithLifecycle()

    if (showTanks) {
        BackHandler { showTanks = false }
        TankScreen(vm, onBack = { showTanks = false })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (tab == 0) "水槽ログ" else TABS[tab].label) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    actions = {
                        IconButton(onClick = {
                            val next = ThemeMode.entries[(mode.ordinal + 1) % ThemeMode.entries.size]
                            ThemePrefs.set(context, next)
                        }) {
                            Icon(
                                when (mode) {
                                    ThemeMode.AUTO -> AutoIcon
                                    ThemeMode.LIGHT -> SunIcon
                                    ThemeMode.NIGHT -> MoonIcon
                                },
                                contentDescription = "表示：${mode.label}（タップで切り替え）",
                            )
                        }
                        IconButton(onClick = { showTanks = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "水槽の設定")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    TABS.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (tab) {
                    0 -> HomeScreen(vm)
                    1 -> LogScreen(vm)
                    2 -> WaterScreen(vm)
                    3 -> LivestockScreen(vm)
                    else -> ReminderScreen(vm)
                }
            }
        }
    }
}
