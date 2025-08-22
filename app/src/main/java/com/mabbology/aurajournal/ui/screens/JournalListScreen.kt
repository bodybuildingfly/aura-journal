package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalListScreen(
    navController: NavController,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val journalListState by viewModel.journalListState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Journal") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("journalEditor") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Journal Entry")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                journalListState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                journalListState.error != null -> {
                    Text(
                        text = "Error: ${journalListState.error}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                journalListState.journals.isEmpty() -> {
                    Text(
                        text = "No journal entries yet. Tap the '+' button to create one!",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(journalListState.journals) { journal ->
                            ListItem(
                                headlineContent = { Text(journal.title) },
                                supportingContent = {
                                    Text(
                                        text = journal.content,
                                        maxLines = 2
                                    )
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
