package com.workouttracker

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workouttracker.ui.navigation.AppNavigation
import com.workouttracker.ui.navigation.Routes
import com.workouttracker.ui.screens.OnboardingScreen
import com.workouttracker.ui.theme.WorkoutTrackerTheme
import com.workouttracker.ui.viewmodel.WorkoutViewModel

data class BottomNavItem(
    val label: String, val route: String,
    val selectedIcon: ImageVector, val unselectedIcon: ImageVector
)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WorkoutTrackerApp() }
    }
}

@Composable
fun WorkoutTrackerApp() {
    val appViewModel: WorkoutViewModel = viewModel()
    val isDarkTheme by appViewModel.isDarkTheme.collectAsStateWithLifecycle()
    val useLbs      by appViewModel.useLbs.collectAsStateWithLifecycle()
    val activeTimer by appViewModel.activeTimerSeconds.collectAsStateWithLifecycle()

    WorkoutTrackerTheme(darkTheme = isDarkTheme) {
        val context = LocalContext.current
        
        // Check if onboarding has been completed (stored in SharedPreferences)
        val prefs = remember { context.getSharedPreferences("wt_prefs", Context.MODE_PRIVATE) }
        var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding_done", false)) }

        // Show onboarding on first ever launch
        if (!onboardingDone) {
            OnboardingScreen(
                onFinish = {
                    prefs.edit().putBoolean("onboarding_done", true).apply()
                    onboardingDone = true
                }
            )
            return@WorkoutTrackerTheme
        }

        // Main app
        val navController = rememberNavController()
        val navBackStack  by navController.currentBackStackEntryAsState()
        val currentRoute  = navBackStack?.destination?.route

        // Global error handler — listens to AppErrorBus and shows a snackbar
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by com.workouttracker.ui.util.AppErrorBus.error.collectAsStateWithLifecycle()
        
        LaunchedEffect(errorMessage) {
            errorMessage?.let {
                snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
                com.workouttracker.ui.util.AppErrorBus.clear()
            }
        }

        val bottomNavItems = listOf(
            BottomNavItem(context.getString(R.string.nav_calendar), Routes.CALENDAR, Icons.Filled.CalendarMonth,  Icons.Outlined.CalendarMonth),
            BottomNavItem(context.getString(R.string.nav_cardio),   Routes.CARDIO,   Icons.Filled.DirectionsRun,  Icons.Outlined.DirectionsRun),
            BottomNavItem(context.getString(R.string.nav_programs), Routes.PROGRAMS, Icons.Filled.FitnessCenter,  Icons.Outlined.FitnessCenter),
            BottomNavItem(context.getString(R.string.nav_stats),    Routes.HISTORY,  Icons.Filled.BarChart,       Icons.Outlined.BarChart),
            BottomNavItem(context.getString(R.string.nav_tools),    Routes.TOOLS,    Icons.Filled.Construction,   Icons.Outlined.Construction)
        )

        val detailPrefixes = listOf(
            "workout/", "program/", "custom_program/", "exercise_history/",
            "bodyweight", "plate_calc", "templates", "exercise_history_list",
            "new_custom_program", "custom_programs", "account"
        )
        val showBottomBar = currentRoute != null &&
                detailPrefixes.none { currentRoute.startsWith(it) }

        Scaffold(
            modifier       = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost   = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    // Floating Rest Timer Overlay
                    AnimatedVisibility(
                        visible = activeTimer != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        activeTimer?.let { seconds ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { appViewModel.stopTimer() },
                                color = MaterialTheme.colorScheme.primaryContainer,
                                tonalElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    val minutes = seconds / 60
                                    val remainingSeconds = seconds % 60
                                    Text(
                                        "Rest Timer: $minutes:${if (remainingSeconds < 10) "0$remainingSeconds" else remainingSeconds}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "Stop",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    if (showBottomBar) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            bottomNavItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick  = {
                                        if (!selected) navController.navigate(item.route) {
                                            popUpTo(Routes.CALENDAR) { saveState = true }
                                            launchSingleTop = true
                                            restoreState    = true
                                        }
                                    },
                                    icon  = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                                    label = { Text(item.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor   = MaterialTheme.colorScheme.primary,
                                        selectedTextColor   = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppNavigation(
                navController = navController,
                isDarkTheme   = isDarkTheme,
                onToggleTheme = { appViewModel.toggleTheme() },
                useLbs        = useLbs,
                onToggleUnit  = { appViewModel.toggleUnit() },
                modifier      = Modifier.padding(innerPadding)
            )
        }
    }
}
