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

    // Determine the user's role based on the current scope
    val (isDominantInScope, currentPartner) = remember(scope, partnersState.partners, profileState.userId) {
        val partnerInScope = (scope as? Scope.PartnerScope)?.partner
        val isDominant = partnerInScope?.dominantId == profileState.userId
        (isDominant to partnerInScope)
    }

    val hasPartner = partnersState.partners.isNotEmpty()

    // Filter assignments based on the user's role in the current scope
    val (pendingAssignments, completedAssignments) = remember(state.pendingAssignments, state.completedAssignments, profileState.userId, currentPartner, isDominantInScope) {
        val userId = profileState.userId
        if (currentPartner == null) {
            // Personal scope: user is always the submissive in this context
            state.pendingAssignments.filter { it.submissiveId == userId } to state.completedAssignments.filter { it.submissiveId == userId }
        } else {
            if (isDominantInScope) {
                // Dominant's view: show assignments they created for the partner
                val partnerId = currentPartner.submissiveId
                state.pendingAssignments.filter { it.dominantId == userId && it.submissiveId == partnerId } to
                        state.completedAssignments.filter { it.dominantId == userId && it.submissiveId == partnerId }
            } else {
                // Submissive's view: show assignments they received from the partner
                val partnerId = currentPartner.dominantId
                state.pendingAssignments.filter { it.dominantId == partnerId && it.submissiveId == userId } to
                        state.completedAssignments.filter { it.dominantId == partnerId && it.submissiveId == userId }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isDominantInScope) {
                FloatingActionButton(onClick = {
                    currentPartner?.submissiveId?.let { subId ->
                        navController.navigate("createAssignment/$subId")
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Assignment")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!hasPartner && scope is Scope.Personal) {
                Text("You need a partner to use assignments.", modifier = Modifier.align(Alignment.Center))
            } else if (pendingAssignments.isEmpty() && completedAssignments.isEmpty() && !state.isLoading) {
                Text("No assignments to show.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pendingAssignments.isNotEmpty()) {
                        item {
                            Text("Pending", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(pendingAssignments) { assignment ->
                            AssignmentCard(
                                assignment = assignment,
                                onClick = {
                                    // A submissive clicks to complete the assignment
                                    if (assignment.submissiveId == profileState.userId) {
                                        val encodedPrompt = URLEncoder.encode(assignment.prompt, StandardCharsets.UTF_8.toString())
                                        navController.navigate("journalEditor?assignmentId=${assignment.id}&prompt=${encodedPrompt}")
                                    }
                                    // A dominant clicking a pending assignment does nothing for now
                                }
                            )
                        }
                    }

                    if (completedAssignments.isNotEmpty()) {
                        item {
                            if (pendingAssignments.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            }
                            Text("Completed", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(completedAssignments) { assignment ->
                            AssignmentCard(
                                assignment = assignment,
                                onClick = {
                                    // Navigate to the journal entry for the completed assignment
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
