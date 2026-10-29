package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CardinalTab
import com.example.ui.MainViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MissionStudioScreen
import com.example.ui.screens.PerceptionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsCatalogScreen
import com.example.ui.theme.AetherealCardinalTheme
import com.example.ui.theme.CardinalCrimson
import com.example.ui.theme.CardinalScarlet
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AetherealCardinalTheme {
                val viewModel: MainViewModel = viewModel()
                val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
                val activeMission by viewModel.activeMission.collectAsStateWithLifecycle()
                val logs by viewModel.telemetryLogs.collectAsStateWithLifecycle()
                val recentMissions by viewModel.recentMissions.collectAsStateWithLifecycle()
                val llmConfig by viewModel.llmConfig.collectAsStateWithLifecycle()
                val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsStateWithLifecycle()
                val livePerception by viewModel.livePerception.collectAsStateWithLifecycle()
                val toolResult by viewModel.singleToolExecutionResult.collectAsStateWithLifecycle()

                // Back handling: return to Mission Studio if in sub-screen
                BackHandler(enabled = selectedTab != CardinalTab.MISSION) {
                    viewModel.selectTab(CardinalTab.MISSION)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ObsidianDeep,
                    bottomBar = {
                        CardinalBottomNav(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        CardinalTab.MISSION -> {
                            MissionStudioScreen(
                                activeMission = activeMission,
                                logs = logs,
                                isAccessibilityActive = isAccessibilityActive,
                                onDispatch = { prompt -> viewModel.dispatchMission(prompt) },
                                onPause = { viewModel.pauseMission() },
                                onResume = { viewModel.resumeMission() },
                                onAbort = { viewModel.abortMission() },
                                onOpenAccessibilitySettings = { openAccessibilitySettings() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        CardinalTab.TOOLS -> {
                            ToolsCatalogScreen(
                                tools = viewModel.allTools,
                                isAccessibilityActive = isAccessibilityActive,
                                executionResult = toolResult,
                                onExecuteTool = { id, params -> viewModel.executeSingleTool(id, params) },
                                onClearResult = { viewModel.clearToolResult() },
                                onOpenAccessibilitySettings = { openAccessibilitySettings() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        CardinalTab.PERCEPTION -> {
                            PerceptionScreen(
                                perception = livePerception,
                                isAccessibilityActive = isAccessibilityActive,
                                onRefresh = { viewModel.refreshPerception() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        CardinalTab.HISTORY -> {
                            HistoryScreen(
                                missions = recentMissions,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        CardinalTab.SETTINGS -> {
                            SettingsScreen(
                                currentConfig = llmConfig,
                                onSaveConfig = { viewModel.updateLLMConfig(it) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}

@Composable
fun CardinalBottomNav(
    selectedTab: CardinalTab,
    onTabSelected: (CardinalTab) -> Unit
) {
    NavigationBar(
        containerColor = ObsidianSurface,
        modifier = Modifier.testTag("cardinal_bottom_navigation")
    ) {
        val items = listOf(
            Triple(CardinalTab.MISSION, "Mission", Icons.Default.PlayArrow),
            Triple(CardinalTab.TOOLS, "Tools", Icons.Default.Build),
            Triple(CardinalTab.PERCEPTION, "Vision", Icons.Default.Visibility),
            Triple(CardinalTab.HISTORY, "History", Icons.Default.History),
            Triple(CardinalTab.SETTINGS, "Config", Icons.Default.Settings)
        )

        items.forEach { (tab, label, icon) ->
            val isSelected = selectedTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimary,
                    selectedTextColor = CardinalScarlet,
                    indicatorColor = CardinalCrimson,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}
