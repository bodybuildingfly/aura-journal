package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.ui.util.formatDateHeader
import com.mabbology.aurajournal.ui.util.formatTime
import com.mabbology.aurajournal.ui.util.parseDate
import com.mabbology.aurajournal.ui.viewmodel.JournalViewModel
import com.mabbology.aurajournal.ui.viewmodel.Scope
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalListScreen(
    navController: NavController,
    viewModel: JournalViewModel = hiltViewModel(),
    scope: Scope
) {
    LaunchedEffect(scope) {
        viewModel.setScope(scope)
    }
    val listState by viewModel.listState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState.error) {
        listState.error?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.clearListError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            if (listState.items.isEmpty() && !listState.isLoading) {
                EmptyJournalView()
            } else {
                val groupedJournals = remember(listState.items) {
                    listState.items.groupBy { parseDate(it.createdAt) }
                }
                val sortedDates = remember(groupedJournals) {
                    groupedJournals.keys.sortedDescending()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    sortedDates.forEach { date ->
                        val journals = groupedJournals[date].orEmpty()
                        stickyHeader {
                            DateHeader(date = date)
                        }
                        items(
                            items = journals,
                            key = { journal -> journal.id }
                        ) { journal ->
                            JournalCard(
                                journal = journal,
                                onClick = {
                                    if (journal.id.startsWith("local_")) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Entry is still syncing...")
                                        }
                                    } else {
                                        navController.navigate("journalView/${journal.id}")
                                    }
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                isOwnEntry = journal.userId == listState.currentUserId
                            )
                        }
                    }
                }
            }
            if (listState.isLoading) {
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

@Composable
fun DateHeader(date: LocalDate) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Text(
            text = formatDateHeader(date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalCard(
    journal: Journal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOwnEntry: Boolean
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        color = if (isOwnEntry) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = journal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(journal.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            journal.mood?.let { mood ->
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = mood,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    }
}

