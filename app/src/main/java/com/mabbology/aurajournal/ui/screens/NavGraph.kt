package com.mabbology.aurajournal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mabbology.aurajournal.ui.viewmodel.AuthViewModel
import com.mabbology.aurajournal.ui.viewmodel.NoteViewModel

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated && !authState.isLoading) {
            navController.navigate("auth_flow") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    val startDestination = if (authState.isAuthenticated) "main" else "auth_flow"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth_flow") {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        composable("register") {
            RegistrationScreen(navController = navController, viewModel = authViewModel)
        }

        composable("main") {
            MainScreen(navController = navController, authViewModel = authViewModel)
        }

        // --- Journal Routes ---
        composable(
            "journalView/{journalId}",
            arguments = listOf(navArgument("journalId") { type = NavType.StringType })
        ) { backStackEntry ->
            JournalViewScreen(
                navController = navController,
                journalId = backStackEntry.arguments?.getString("journalId")
            )
        }
        composable(
            route = "journalEditor?journalId={journalId}&assignmentId={assignmentId}&prompt={prompt}",
            arguments = listOf(
                navArgument("journalId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("assignmentId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("prompt") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            JournalEditorScreen(
                navController = navController,
                journalId = backStackEntry.arguments?.getString("journalId"),
                assignmentId = backStackEntry.arguments?.getString("assignmentId"),
                prompt = backStackEntry.arguments?.getString("prompt")
            )
        }

        // --- Note Routes ---
        composable(
            "noteView/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val viewModel: NoteViewModel = hiltViewModel()
            NoteViewScreen(
                navController = navController,
                viewModel = viewModel,
                noteId = backStackEntry.arguments?.getString("noteId")
            )
        }
        composable(
            "noteEditor?noteId={noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            val viewModel: NoteViewModel = hiltViewModel()
            NoteEditorScreen(
                navController = navController,
                viewModel = viewModel,
                noteId = backStackEntry.arguments?.getString("noteId")
            )
        }

        // --- Other Routes ---
        composable("partners") {
            PartnersScreen(navController = navController)
        }
        composable("userList") {
            UserListScreen(navController = navController)
        }
        composable("profile") {
            ProfileScreen(navController = navController)
        }

        // --- Assignment Routes ---
        composable("assignmentList") {
            AssignmentListScreen(navController = navController)
        }
        composable(
            "createAssignment/{submissiveId}",
            arguments = listOf(navArgument("submissiveId") { type = NavType.StringType })
        ) { backStackEntry ->
            CreateAssignmentScreen(
                navController = navController,
                submissiveId = backStackEntry.arguments?.getString("submissiveId")
            )
        }

        // --- Chat Route ---
        composable(
            "chat/{partnershipId}/{partnerId}",
            arguments = listOf(
                navArgument("partnershipId") { type = NavType.StringType },
                navArgument("partnerId") { type = NavType.StringType }
            )
        ) {
            ChatScreen(navController = navController)
        }
    }
}
