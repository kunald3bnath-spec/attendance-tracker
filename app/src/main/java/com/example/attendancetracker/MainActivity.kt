package com.example.attendancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attendancetracker.data.AttendanceDatabase
import com.example.attendancetracker.data.AttendanceRepository
import com.example.attendancetracker.ui.screens.HistoryScreen
import com.example.attendancetracker.ui.screens.MarkAttendanceScreen
import com.example.attendancetracker.ui.screens.OverviewScreen
import com.example.attendancetracker.ui.screens.ProjectsScreen
import com.example.attendancetracker.ui.theme.AttendanceTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AttendanceDatabase.getInstance(applicationContext)
        val repository = AttendanceRepository(
            database.projectDao(),
            database.memberDao(),
            database.attendanceRecordDao()
        )
        setContent {
            AttendanceTrackerApp(repository)
        }
    }
}

// ── Bottom Nav Destinations ───────────────────────────────────────────────────

private enum class BottomTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    PROJECTS("Projects", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen),
    MARK("Mark", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    OVERVIEW("Overview", Icons.Filled.BarChart, Icons.Outlined.BarChart),
}

// ── Root App ─────────────────────────────────────────────────────────────────

@Composable
fun AttendanceTrackerApp(repository: AttendanceRepository) {
    val viewModel: AttendanceViewModel = viewModel(
        factory = AttendanceViewModelFactory(repository)
    )
    val isDark by viewModel.isDarkMode.collectAsState()

    AttendanceTrackerTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Outer nav: projects list → project detail shell
            val outerNav = rememberNavController()

            NavHost(outerNav, startDestination = "projects_list") {

                // Projects list — no bottom bar here
                composable("projects_list") {
                    ProjectsScreen(
                        viewModel = viewModel,
                        onProjectClick = { projectId ->
                            viewModel.selectProject(projectId)
                            outerNav.navigate("project_shell/$projectId")
                        }
                    )
                }

                // Project shell — has bottom nav
                composable("project_shell/{projectId}") {
                    ProjectShell(
                        viewModel = viewModel,
                        onBackToProjects = {
                            viewModel.clearSelectedProject()
                            outerNav.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

// ── Project Shell (bottom nav) ────────────────────────────────────────────────

@Composable
fun ProjectShell(
    viewModel: AttendanceViewModel,
    onBackToProjects: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.MARK) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab == BottomTab.PROJECTS) {
                                onBackToProjects()
                            } else {
                                selectedTab = tab
                            }
                        },
                        icon = {
                            Icon(
                                if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                BottomTab.PROJECTS -> { /* handled by onClick above */ }
                BottomTab.MARK -> MarkAttendanceScreen(viewModel = viewModel)
                BottomTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                BottomTab.OVERVIEW -> OverviewScreen(viewModel = viewModel)
            }
        }
    }
}
