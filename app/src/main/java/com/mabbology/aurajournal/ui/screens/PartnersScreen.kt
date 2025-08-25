package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mabbology.aurajournal.domain.model.Partner
import com.mabbology.aurajournal.domain.model.PartnerRequest
import com.mabbology.aurajournal.ui.viewmodel.PartnerRequestsViewModel
import com.mabbology.aurajournal.ui.viewmodel.PartnersViewModel
import com.mabbology.aurajournal.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnersScreen(
    navController: NavController,
    requestsViewModel: PartnerRequestsViewModel = hiltViewModel(),
    partnersViewModel: PartnersViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val requestsState by requestsViewModel.state.collectAsState()
    val partnersState by partnersViewModel.state.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                partnersViewModel.syncPartners()
                requestsViewModel.syncRequests()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partners") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        partnersViewModel.syncPartners()
                        requestsViewModel.syncRequests()
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "Refresh")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Partners", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (partnersState.partners.isEmpty()) {
                item { Text("You have no partners yet.", modifier = Modifier.padding(bottom = 16.dp)) }
            } else {
                items(partnersState.partners) { partner ->
                    PartnerCard(
                        partner = partner,
                        currentUserId = profileState.userId,
                        onAssignJournal = { submissiveId ->
                            navController.navigate("createAssignment/$submissiveId")
                        }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Incoming Applications", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (requestsState.incomingRequests.isEmpty()) {
                item { Text("No incoming applications.", modifier = Modifier.padding(bottom = 16.dp)) }
            } else {
                items(requestsState.incomingRequests) { request ->
                    IncomingRequestCard(
                        request = request,
                        isApproving = requestsState.approvingRequestId == request.id,
                        onApprove = { requestsViewModel.approveRequest(request) },
                        onReject = { requestsViewModel.rejectRequest(request.id) }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Outgoing Applications", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (requestsState.outgoingRequests.isEmpty()) {
                item { Text("No outgoing applications.") }
            } else {
                items(requestsState.outgoingRequests) { request ->
                    OutgoingRequestCard(request = request)
                }
            }
        }
    }
}

@Composable
fun PartnerCard(
    partner: Partner,
    currentUserId: String,
    onAssignJournal: (String) -> Unit
) {
    val isCurrentUserDominant = partner.dominantId == currentUserId
    val partnerName = if (isCurrentUserDominant) partner.submissiveName else partner.dominantName
    val partnerRole = if (isCurrentUserDominant) "submissive" else "Dominant"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$partnerName ($partnerRole)",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isCurrentUserDominant) {
                Button(onClick = { onAssignJournal(partner.submissiveId) }) {
                    Text("Assign Journal")
                }
            }
        }
    }
}

@Composable
fun IncomingRequestCard(
    request: PartnerRequest,
    isApproving: Boolean,
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
                text = "Application from ${request.counterpartyName}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))

            if (isApproving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Row {
                    Button(onClick = onApprove, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("Approve")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onReject, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
fun OutgoingRequestCard(request: PartnerRequest) {
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
                    text = "Application to ${request.counterpartyName}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Status: ${request.status.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
