package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.ui.viewmodel.JournalViewModel
import com.mabbology.aurajournal.ui.viewmodel.PartnersViewModel
import com.mabbology.aurajournal.ui.viewmodel.ProfileState
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalListScreen(
    navController: NavController,
    viewModel: JournalViewModel,
    profileState: ProfileState,
    partnersViewModel: PartnersViewModel
) {
    val journalListState by viewModel.journalListState.collectAsState()
    val partnersState by partnersViewModel.state.collectAsState()

    val submissivePartners = partnersState.partners.filter { it.dominantId == profileState.userId }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (submissivePartners.isNotEmpty()) {
                    val submissiveToAssign = submissivePartners.first()
                    FloatingActionButton(
                        onClick = { navController.navigate("createAssignment/${submissiveToAssign.submissiveId}") },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        // Icon for assigning journal
                    }
                }
                FloatingActionButton(onClick = { navController.navigate("journalEditor") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Journal Entry")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (journalListState.journals.isEmpty() && !journalListState.isLoading) {
                EmptyJournalView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = journalListState.journals,
                        key = { journal -> journal.id }
                    ) { journal ->
                        JournalCard(
                            journal = journal,
                            onClick = {
                                navController.navigate("journalView/${journal.id}")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            if (journalListState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun EmptyJournalView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Book,
            contentDescription = "Empty Journal",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No journal entries yet.\nTap the '+' button to create one!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalCard(journal: Journal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = journal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimestampList(journal.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (journal.type == "shared") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Shared",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            journal.mood?.let { mood ->
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = mood, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

private fun formatTimestampList(isoString: String): String {
    return try {
        val odt = OffsetDateTime.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
        odt.format(formatter)
    } catch (e: Exception) {
        "Invalid date"
    }
}
