package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    LaunchedEffect(journalId) {
        if (journalId != null) {
            viewModel.getJournalById(journalId)
        }
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

// Helper function to format the ISO 8601 timestamp from Appwrite
private fun formatTimestamp(isoString: String): String {
    return try {
        val odt = OffsetDateTime.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")
        odt.format(formatter)
    } catch (e: Exception) {
        "Invalid date"
    }
}
