package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mabbology.aurajournal.ui.viewmodel.UserListViewModel

@Composable
fun UserListScreen(
    viewModel: UserListViewModel = hiltViewModel()
) {
    val userListState by viewModel.userListState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchUserProfiles(it)
            },
            label = { Text("Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (userListState.isLoading) {
            CircularProgressIndicator()
        }

        userListState.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn {
            items(userListState.users) { user ->
                // Display user profile
            }
        }
    }
}
