package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.JournalViewModel
import com.mabbology.aurajournal.ui.viewmodel.PartnersViewModel
import com.mabbology.aurajournal.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    navController: NavController,
    partnersViewModel: PartnersViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    journalId: String? = null,
    assignmentId: String? = null,
    prompt: String? = null,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val editorState by viewModel.editorState.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val partnersState by partnersViewModel.state.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf(prompt ?: "") }
    var content by remember { mutableStateOf("") }
    var isShared by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf<String?>(null) }

    val firstPartner = partnersState.partners.firstOrNull()
    val isSubmissive = firstPartner?.submissiveId == profileState.userId
    val partnerId = if (isSubmissive) firstPartner?.dominantId else firstPartner?.submissiveId

    LaunchedEffect(journalId) {
        if (journalId != null) {
            viewModel.observeItemById(journalId)
        }
    }

    LaunchedEffect(selectedState.item) {
        selectedState.item?.let {
            title = it.title
            content = it.content
            isShared = it.type == "shared"
            selectedMood = it.mood
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetEditorState()
        }
    }

    LaunchedEffect(editorState.isSaveSuccess) {
        if (editorState.isSaveSuccess) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(editorState.error) {
        editorState.error?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.resetEditorState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (journalId == null) "New Entry" else "Edit Entry") },
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
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            MoodSelector(
                selectedMood = selectedMood,
                onMoodSelected = { selectedMood = it }
            )
            Spacer(modifier = Modifier.height(16.dp))


            if (partnerId != null && journalId == null) {
                ShareWithPartnerRow(
                    isShared = isShared,
                    onCheckedChange = { isShared = it },
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Button(
                onClick = {
                    when {
                        assignmentId != null -> {
                            viewModel.completeAssignment(assignmentId, title, content, partnerId, selectedMood)
                        }
                        journalId != null -> {
                            viewModel.updateJournalEntry(journalId, title, content)
                        }
                        else -> {
                            val type = if (isShared && partnerId != null) "shared" else "personal"
                            val finalPartnerId = if (type == "shared") partnerId else null
                            viewModel.createJournalEntry(title, content, type, finalPartnerId, selectedMood)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !editorState.isSaving && title.isNotBlank()
            ) {
                if (editorState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun MoodSelector(selectedMood: String?, onMoodSelected: (String) -> Unit) {
    val moods = listOf("😊", "😢", "😠", "😍", "😴")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            moods.forEach { mood ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (mood == selectedMood) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onMoodSelected(mood) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = mood, fontSize = 28.sp)
                }
            }
        }
    }
}
