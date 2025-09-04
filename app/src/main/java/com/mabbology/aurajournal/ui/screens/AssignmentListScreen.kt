package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.domain.model.JournalAssignment
import com.mabbology.aurajournal.ui.viewmodel.Scope
import com.mabbology.aurajournal.ui.viewmodel.JournalAssignmentViewModel
import com.mabbology.aurajournal.ui.viewmodel.PartnersViewModel
import com.mabbology.aurajournal.ui.viewmodel.ProfileViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentListScreen(
    navController: NavController,
    viewModel: JournalAssignmentViewModel = hiltViewModel(),
    partnersViewModel: PartnersViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    scope: Scope
) {
    LaunchedEffect(scope) {
        viewModel.setScope(scope)
    }
    val state by viewModel.state.collectAsState()
    val partnersState by partnersViewModel.state.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

    val isDominant = partnersState.partners.any { it.dominantId == profileState.userId }
    val hasPartner = partnersState.partners.isNotEmpty()

    Scaffold(
        floatingActionButton = {
            if (isDominant) {
                FloatingActionButton(onClick = {
                    val submissiveId = partnersState.partners.first { it.dominantId == profileState.userId }.submissiveId
                    navController.navigate("createAssignment/$submissiveId")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Assignment")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!hasPartner) {
                Text("You need a partner to use assignments.", modifier = Modifier.align(Alignment.Center))
            } else if (state.pendingAssignments.isEmpty() && state.completedAssignments.isEmpty() && !state.isLoading) {
                Text("You have no assignments.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.pendingAssignments.isNotEmpty()) {
                        item {
                            Text("New Assignments", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(state.pendingAssignments) { assignment ->
                            AssignmentCard(
                                assignment = assignment,
                                onClick = {
                                    val encodedPrompt = URLEncoder.encode(assignment.prompt, StandardCharsets.UTF_8.toString())
                                    navController.navigate("journalEditor?assignmentId=${assignment.id}&prompt=${encodedPrompt}")
                                }
                            )
                        }
                    }

                    if (state.completedAssignments.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            Text("Completed Assignments", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(state.completedAssignments) { assignment ->
                            AssignmentCard(
                                assignment = assignment,
                                onClick = {
                                    // Navigate to the journal entry
                                    assignment.journalId?.let {
                                        navController.navigate("journalView/$it")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentCard(assignment: JournalAssignment, onClick: () -> Unit) {
    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(assignment.createdAt)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = formattedDate, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = assignment.prompt, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
