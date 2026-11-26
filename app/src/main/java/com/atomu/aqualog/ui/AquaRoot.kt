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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel

private data class TabDef(val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabDef("ホーム", Icons.Filled.Home),
    TabDef("作業", Icons.Filled.Build),
    TabDef("水質", Icons.Filled.Info),
    TabDef("生体", Icons.Filled.Star),
    TabDef("リマインド", Icons.Filled.Notifications),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquaRoot(vm: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showTanks by rememberSaveable { mutableStateOf(false) }

    if (showTanks) {
        BackHandler { showTanks = false }
        TankScreen(vm, onBack = { showTanks = false })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (tab == 0) "水槽ログ" else TABS[tab].label) },
                    actions = {
                        IconButton(onClick = { showTanks = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "水槽の設定")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
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
