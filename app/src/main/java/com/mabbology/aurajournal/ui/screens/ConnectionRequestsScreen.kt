package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.domain.model.ConnectionRequest
import com.mabbology.aurajournal.ui.viewmodel.ConnectionRequestsViewModel
import com.mabbology.aurajournal.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionRequestsScreen(
    navController: NavController,
    viewModel: ConnectionRequestsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partners") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("userList") }) {
                Icon(Icons.Default.Search, contentDescription = "Find Users")
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Incoming Requests", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (state.incomingRequests.isEmpty()) {
                    item { Text("No incoming requests.", modifier = Modifier.padding(bottom = 16.dp)) }
                } else {
                    items(state.incomingRequests) { request ->
                        IncomingRequestCard(
                            request = request,
                            onApprove = { viewModel.approveRequest(request) },
                            onReject = { viewModel.rejectRequest(request.id) }
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Outgoing Requests", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (state.outgoingRequests.isEmpty()) {
                    item { Text("No outgoing requests.") }
                } else {
                    items(state.outgoingRequests) { request ->
                        OutgoingRequestCard(
                            request = request,
                            currentUserRole = profileState.role,
                            onAssignJournal = {
                                navController.navigate("createAssignment/${request.recipientId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingRequestCard(
    request: ConnectionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${request.counterpartyName}: ${getOppositeRole(request.counterpartyRole)}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            if (request.status == "pending") {
                Row {
                    Button(onClick = onApprove, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("Approve")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onReject, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("Reject")
                    }
                }
            } else {
                Text(request.status.replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
fun OutgoingRequestCard(
    request: ConnectionRequest,
    currentUserRole: String?,
    onAssignJournal: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${request.counterpartyName}: ${request.counterpartyRole.replaceFirstChar { it.uppercase() }}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = request.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (request.status == "approved" && currentUserRole == "Dominant") {
                Button(onClick = onAssignJournal) {
                    Text("Assign Journal")
                }
            }
        }
    }
}

private fun getOppositeRole(role: String): String {
    return when (role.lowercase()) {
        "dominant" -> "submissive"
        "submissive" -> "Dominant"
        else -> "Partner"
    }.replaceFirstChar { it.uppercase() }
}
