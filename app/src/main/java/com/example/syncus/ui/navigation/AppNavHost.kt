package com.example.syncus.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.syncus.ui.screens.AddTaskScreen
import com.example.syncus.ui.screens.CalendarScreen
import com.example.syncus.ui.screens.HomeScreen
import com.example.syncus.ui.screens.LoginScreen
import com.example.syncus.ui.screens.ProfileScreen
import com.example.syncus.ui.screens.RegisterScreen
import com.example.syncus.ui.screens.SettingsScreen
import com.example.syncus.ui.screens.SplashScreen
import com.example.syncus.ui.screens.TasksScreen
import com.example.syncus.ui.screens.EditTaskScreen



@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar =
        currentRoute == Routes.HOME ||
                currentRoute == Routes.TASKS ||
                currentRoute == Routes.PROFILE

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController)
            }
        }
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onGoHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onGoLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(navController = navController)
            }

            composable(Routes.REGISTER) {
                RegisterScreen(navController = navController)
            }

            composable(Routes.HOME) {
                HomeScreen(navController = navController)
            }

            composable(Routes.TASKS) {
                TasksScreen(navController = navController)
            }

            composable(Routes.CALENDAR) {
                CalendarScreen(navController = navController)
            }

            composable(Routes.PROFILE) {
                ProfileScreen(navController = navController)
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(navController = navController)
            }

            composable(Routes.ADD_TASK) {
                AddTaskScreen(navController = navController)
            }

            composable(Routes.EDIT_TASK) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
                EditTaskScreen(navController = navController, taskId = taskId)
            }
        }
    }
}