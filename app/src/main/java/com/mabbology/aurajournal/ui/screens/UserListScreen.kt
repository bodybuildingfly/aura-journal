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
import com.mabbology.aurajournal.ui.viewmodel.UserListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    navController: NavController,
    viewModel: UserListViewModel = hiltViewModel()
) {
    val userListState by viewModel.userListState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userListState.requestSentMessage) {
        userListState.requestSentMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Users") },
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
                    searchQuery = it
                    viewModel.searchUserProfiles(it)
                },
                label = { Text("Search by name") },
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
                        UserCard(user = user, onSendRequest = { viewModel.sendConnectionRequest(user.userId) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: UserProfile, onSendRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = user.displayName, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onSendRequest) {
            Text("Send Request")
        }
    }
}
