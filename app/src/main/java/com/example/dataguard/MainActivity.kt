package com.example.dataguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.dataguard.ui.screens.DashboardScreen
import com.example.dataguard.ui.screens.HistoryScreen
import com.example.dataguard.ui.screens.PlanSetupScreen
import com.example.dataguard.ui.screens.TopAppsScreen
import com.example.dataguard.ui.theme.DataGuardTheme
import com.example.dataguard.ui.viewmodel.DataGuardViewModel
import com.example.dataguard.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {

    private val viewModel: DataGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            DataGuardTheme {
                DataGuardMainScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissionAndLoad()
    }
}

@Composable
fun DataGuardMainScaffold(viewModel: DataGuardViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    // Handle back button to return to Dashboard if on other tabs
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        viewModel.selectTab(ScreenTab.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.DASHBOARD,
                    onClick = { viewModel.selectTab(ScreenTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.TOP_APPS,
                    onClick = { viewModel.selectTab(ScreenTab.TOP_APPS) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Top Apps") },
                    label = { Text("Apps") },
                    modifier = Modifier.testTag("nav_apps")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.HISTORY,
                    onClick = { viewModel.selectTab(ScreenTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.PLAN_SETUP,
                    onClick = { viewModel.selectTab(ScreenTab.PLAN_SETUP) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Plan & Alerts") },
                    label = { Text("Plan") },
                    modifier = Modifier.testTag("nav_plan")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                ScreenTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                ScreenTab.TOP_APPS -> TopAppsScreen(viewModel = viewModel)
                ScreenTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                ScreenTab.PLAN_SETUP -> PlanSetupScreen(viewModel = viewModel)
            }
        }
    }
}
