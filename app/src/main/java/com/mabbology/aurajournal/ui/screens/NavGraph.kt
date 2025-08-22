package com.mabbology.aurajournal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mabbology.aurajournal.ui.viewmodel.AuthViewModel

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    // If the user is authenticated, start on the journal list, otherwise start on login.
    val startDestination = if (authState.isAuthenticated) "journalList" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        composable("register") {
            RegistrationScreen(navController = navController, viewModel = authViewModel)
        }
        composable("journalList") {
            JournalListScreen(navController = navController)
        }
        composable("journalEditor") {
            JournalEditorScreen(navController = navController)
        }
    }
}
