package com.mabbology.aurajournal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.mabbology.aurajournal.ui.viewmodel.AuthViewModel
import com.mabbology.aurajournal.ui.viewmodel.JournalViewModel

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated && !authState.isLoading) {
            navController.navigate("auth_flow") {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }

    val startDestination = if (authState.isAuthenticated) "journal_flow" else "auth_flow"

    NavHost(navController = navController, startDestination = startDestination) {
        navigation(startDestination = "login", route = "auth_flow") {
            composable("login") {
                LoginScreen(navController = navController, viewModel = authViewModel)
            }
            composable("register") {
                RegistrationScreen(navController = navController, viewModel = authViewModel)
            }
        }

        navigation(startDestination = "journalList", route = "journal_flow") {
            composable("journalList") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("journal_flow")
                }
                val journalViewModel: JournalViewModel = hiltViewModel(parentEntry)
                JournalListScreen(
                    navController = navController,
                    viewModel = journalViewModel,
                    authViewModel = authViewModel
                )
            }
            // Route for creating a NEW entry
            composable("journalEditor") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("journal_flow")
                }
                val journalViewModel: JournalViewModel = hiltViewModel(parentEntry)
                JournalEditorScreen(
                    navController = navController,
                    viewModel = journalViewModel,
                    journalId = null
                )
            }
            // Route for EDITING an existing entry
            composable(
                route = "journalEditor/{journalId}",
                arguments = listOf(navArgument("journalId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("journal_flow")
                }
                val journalViewModel: JournalViewModel = hiltViewModel(parentEntry)
                JournalEditorScreen(
                    navController = navController,
                    viewModel = journalViewModel,
                    journalId = backStackEntry.arguments?.getString("journalId")
                )
            }
            composable(
                route = "journalView/{journalId}",
                arguments = listOf(navArgument("journalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("journal_flow")
                }
                val journalViewModel: JournalViewModel = hiltViewModel(parentEntry)
                JournalViewScreen(
                    navController = navController,
                    viewModel = journalViewModel,
                    journalId = backStackEntry.arguments?.getString("journalId")
                )
            }
            composable("connectionRequests") {
                ConnectionRequestsScreen(navController = navController)
            }
            composable("userList") {
                UserListScreen(navController = navController)
            }
            composable("profile") {
                ProfileScreen(navController = navController)
            }
        }
    }
}
