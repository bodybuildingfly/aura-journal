package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.JournalViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalViewScreen(
    navController: NavController,
    viewModel: JournalViewModel,
    journalId: String?
) {
    val selectedJournalState by viewModel.selectedJournalState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // This effect now starts observing the journal from the local database.
    // The UI will load instantly and then update if any changes occur.
    LaunchedEffect(journalId) {
        if (journalId != null) {
            viewModel.observeJournalById(journalId)
        }
    }

    LaunchedEffect(selectedJournalState.isDeleted) {
        if (selectedJournalState.isDeleted) {
            navController.popBackStack()
            viewModel.onDeletionHandled()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Journal Entry") },
            text = { Text("Are you sure you want to permanently delete this entry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        journalId?.let { viewModel.deleteJournalEntry(it) }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedJournalState.journal?.title ?: "Journal Entry") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        journalId?.let { id ->
                            navController.navigate("journalEditor?journalId=$id")
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
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
                .verticalScroll(rememberScrollState())
        ) {
            selectedJournalState.journal?.let { journal ->
                Text(
                    text = formatTimestamp(journal.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = journal.content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun formatTimestamp(isoString: String): String {
    return try {
        val odt = OffsetDateTime.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")
        odt.format(formatter)
    } catch (e: Exception) {
        "Invalid date"
    }
}
