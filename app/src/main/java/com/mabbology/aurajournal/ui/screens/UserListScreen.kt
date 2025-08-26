package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.domain.model.UserProfile
import com.mabbology.aurajournal.ui.viewmodel.PartnerRequestsViewModel
import com.mabbology.aurajournal.ui.viewmodel.PartnersViewModel
import com.mabbology.aurajournal.ui.viewmodel.UserListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    navController: NavController,
    viewModel: UserListViewModel = hiltViewModel(),
    partnersViewModel: PartnersViewModel = hiltViewModel(),
    requestsViewModel: PartnerRequestsViewModel = hiltViewModel()
) {
    val userListState by viewModel.userListState.collectAsState()
    val partnersState by partnersViewModel.state.collectAsState()
    val requestsState by requestsViewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userListState.requestSentMessage) {
        userListState.requestSentMessage?.let {
            snackbarHostState.showSnackbar(it)
            // After the message is shown, sync the requests to update the UI
            requestsViewModel.syncRequests()
            viewModel.clearRequestSentMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Potential Dominant") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    viewModel.onSearchQueryChanged(it)
                },
                label = { Text("Search by email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (userListState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(userListState.users) { user ->
                        val isPartner = partnersState.partners.any {
                            it.dominantId == user.userId || it.submissiveId == user.userId
                        }
                        val isPending = requestsState.outgoingRequests.any {
                            it.dominantId == user.userId
                        }

                        val status = when {
                            isPartner -> "Partnered"
                            isPending -> "Pending"
                            else -> null
                        }

                        UserCard(
                            user = user,
                            status = status,
                            onApply = { viewModel.sendApplication(user.userId) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: UserProfile, status: String?, onApply: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = user.displayName, style = MaterialTheme.typography.bodyLarge)

        if (status != null) {
            Text(text = status, style = MaterialTheme.typography.bodyMedium)
        } else {
            Button(onClick = onApply) {
                Text("Apply")
            }
        }
    }
}
