package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.JournalAssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssignmentScreen(
    navController: NavController,
    submissiveId: String?,
    viewModel: JournalAssignmentViewModel = hiltViewModel()
) {
    var prompt by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    // This effect now triggers navigation immediately when assignmentCreated is true.
    LaunchedEffect(state.assignmentCreated) {
        if (state.assignmentCreated) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Journal Assignment") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Assign a new journal entry to your submissive.")
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Journal Prompt") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (submissiveId != null) {
                        viewModel.createAssignment(submissiveId, prompt)
                    }
                },
                // The button is disabled while the operation is in progress.
                enabled = prompt.isNotBlank() && submissiveId != null && !state.isLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Assign")
                }
            }
        }
    }
}
