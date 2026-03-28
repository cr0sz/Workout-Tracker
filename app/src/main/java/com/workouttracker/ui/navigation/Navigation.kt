package com.workouttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.workouttracker.ui.screens.*
import com.workouttracker.ui.viewmodel.SyncViewModel
import com.workouttracker.ui.viewmodel.WorkoutViewModel

object Routes {
    const val CALENDAR              = "calendar"
    const val CARDIO                = "cardio"
    const val PROGRAMS              = "programs"
    const val CUSTOM_PROGRAMS       = "custom_programs"
    const val NEW_CUSTOM_PROGRAM    = "new_custom_program"
    const val HISTORY               = "history"
    const val TOOLS                 = "tools"
    const val ACCOUNT               = "account"
    const val BODYWEIGHT            = "bodyweight"
    const val PLATE_CALC            = "plate_calc"
    const val TEMPLATES             = "templates"
    const val EXERCISE_HISTORY_LIST = "exercise_history_list"
    const val WORKOUT               = "workout/{date}"
    const val PROGRAM_DETAIL        = "program/{programId}"
    const val CUSTOM_PROGRAM_DETAIL = "custom_program/{programId}"
    const val EXERCISE_HISTORY      = "exercise_history/{exerciseName}"
    fun workout(date: String)           = "workout/$date"
    fun programDetail(id: String)       = "program/$id"
    fun customProgramDetail(id: Long)   = "custom_program/$id"
    fun exerciseHistory(name: String)   = "exercise_history/${name.replace("/","_")}"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    useLbs: Boolean,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: WorkoutViewModel = viewModel()
    val syncViewModel: SyncViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.CALENDAR, modifier = modifier) {

        composable(Routes.CALENDAR) {
            CalendarScreen(viewModel = viewModel,
                onDayClick = { navController.navigate(Routes.workout(it)) })
        }

        composable(Routes.WORKOUT,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { back ->
            val date = back.arguments?.getString("date") ?: return@composable
            WorkoutDetailScreen(viewModel = viewModel, date = date,
                onBack = { navController.popBackStack() })
        }

        composable(Routes.CARDIO) { CardioScreen(viewModel = viewModel) }

        composable(Routes.PROGRAMS) {
            ProgramsScreen(viewModel = viewModel,
                onProgramClick = { navController.navigate(Routes.programDetail(it)) })
        }

        composable(Routes.PROGRAM_DETAIL,
            arguments = listOf(navArgument("programId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("programId") ?: return@composable
            ProgramDetailScreen(programId = id, viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLoadedToWorkout = { date ->
                    navController.navigate(Routes.workout(date)) {
                        popUpTo(Routes.PROGRAMS) { inclusive = false }
                    }
                })
        }

        composable(Routes.CUSTOM_PROGRAMS) {
            CustomProgramsScreen(viewModel = viewModel,
                onCreateNew    = { navController.navigate(Routes.NEW_CUSTOM_PROGRAM) },
                onOpenProgram  = { navController.navigate(Routes.customProgramDetail(it)) })
        }

        composable(Routes.NEW_CUSTOM_PROGRAM) {
            NewCustomProgramScreen(viewModel = viewModel,
                onBack    = { navController.popBackStack() },
                onCreated = { id ->
                    navController.navigate(Routes.customProgramDetail(id)) {
                        popUpTo(Routes.CUSTOM_PROGRAMS) { inclusive = false }
                    }
                })
        }

        composable(Routes.CUSTOM_PROGRAM_DETAIL,
            arguments = listOf(navArgument("programId") { type = NavType.LongType })
        ) { back ->
            val id = back.arguments?.getLong("programId") ?: return@composable
            CustomProgramDetailScreen(programId = id, viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLoadedToWorkout = { date ->
                    navController.navigate(Routes.workout(date)) {
                        popUpTo(Routes.CUSTOM_PROGRAMS) { inclusive = false }
                    }
                })
        }

        composable(Routes.HISTORY) { HistoryScreen(viewModel = viewModel) }

        composable(Routes.TOOLS) {
            ToolsScreen(viewModel = viewModel,
                onNavigatePlateCalc       = { navController.navigate(Routes.PLATE_CALC) },
                onNavigateBodyweight      = { navController.navigate(Routes.BODYWEIGHT) },
                onNavigateTemplates       = { navController.navigate(Routes.TEMPLATES) },
                onNavigateExerciseHistory = { navController.navigate(Routes.EXERCISE_HISTORY_LIST) },
                onNavigateAccount         = { navController.navigate(Routes.ACCOUNT) },
                isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme,
                useLbs = useLbs, onToggleUnit = onToggleUnit)
        }

        composable(Routes.ACCOUNT) { AccountScreen(syncViewModel = syncViewModel) }
        composable(Routes.BODYWEIGHT) { BodyweightScreen(viewModel = viewModel) }
        composable(Routes.PLATE_CALC) { PlateCalculatorScreen(viewModel = viewModel) }
        composable(Routes.TEMPLATES) { WorkoutTemplatesScreen(viewModel = viewModel) }

        composable(Routes.EXERCISE_HISTORY_LIST) {
            ExerciseHistoryListScreen(viewModel = viewModel,
                onSelectExercise = { navController.navigate(Routes.exerciseHistory(it)) })
        }

        composable(Routes.EXERCISE_HISTORY,
            arguments = listOf(navArgument("exerciseName") { type = NavType.StringType })
        ) { back ->
            val name = back.arguments?.getString("exerciseName") ?: return@composable
            ExerciseHistoryScreen(exerciseName = name, viewModel = viewModel,
                onBack = { navController.popBackStack() })
        }
    }
}
