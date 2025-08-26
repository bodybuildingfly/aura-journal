package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.NoteViewModel
import com.mabbology.aurajournal.ui.viewmodel.PartnersViewModel
import com.mabbology.aurajournal.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    navController: NavController,
    viewModel: NoteViewModel,
    partnersViewModel: PartnersViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    noteId: String? = null
) {
    val editorState by viewModel.noteEditorState.collectAsState()
    val selectedNoteState by viewModel.selectedNoteState.collectAsState()
    val partnersState by partnersViewModel.state.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isShared by remember { mutableStateOf(false) }

    val firstPartner = partnersState.partners.firstOrNull()
    val isSubmissive = firstPartner?.submissiveId == profileState.userId
    val partnerId = if (isSubmissive) firstPartner?.dominantId else firstPartner?.submissiveId

    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.observeNoteById(noteId)
        }
    }

    LaunchedEffect(selectedNoteState.note) {
        selectedNoteState.note?.let {
            title = it.title
            content = it.content
            isShared = it.type == "shared"
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                label = { Text("Title") },
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

            if (partnerId != null && noteId == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Switch(checked = isShared, onCheckedChange = { isShared = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share with partner")
                }
            }

            Button(
                onClick = {
                    val type = if (isShared && partnerId != null) "shared" else "personal"
                    val finalPartnerId = if (type == "shared") partnerId else null

                    if (noteId == null) {
                        viewModel.createNote(title, content, type, finalPartnerId)
                    } else {
                        viewModel.updateNote(noteId, title, content)
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
