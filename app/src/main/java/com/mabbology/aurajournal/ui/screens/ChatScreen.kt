package com.mabbology.aurajournal.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mabbology.aurajournal.core.util.ExecuteOnKeyboardOpen
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.Message
import com.mabbology.aurajournal.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ChatScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d(TAG, "File selected: $uri")
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.tmp")
                        Log.d(TAG, "Copying file to: ${file.absolutePath}")
                        inputStream?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.d(TAG, "File copied successfully. Size: ${file.length()} bytes")
                        val mediaType = context.contentResolver.getType(uri)
                        Log.d(TAG, "Media type: $mediaType. Sending message...")
                        viewModel.sendMessage(" ", file, mediaType)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling file selection", e)
                    }
                }
            }
        }
    }

    LaunchedEffect(state.messages, listState) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Scroll to the bottom when the keyboard opens
    ExecuteOnKeyboardOpen {
        coroutineScope.launch {
            if (state.messages.isNotEmpty()) {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.partnerName) },
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
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromCurrentUser = message.senderId == state.currentUserId
                    )
                }
            }
            MessageInput(
                message = messageText,
                onMessageChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                onAttach = {
                    filePickerLauncher.launch("*/*")
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message, isFromCurrentUser: Boolean) {
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isFromCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            if (message.mediaUrl != null && message.mediaType != null) {
                MessageMedia(message = message)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (message.content.isNotBlank()) {
                Text(text = message.content)
            }

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (message.status == "sending") {
                    Text(
                        text = "sending...",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (message.status == "failed") {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed to send",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(message.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun MessageMedia(message: Message) {
    val model = if (message.id.startsWith("local_")) {
        message.mediaUrl?.let { File(it) }
    } else {
        message.mediaUrl?.let { "${AppwriteConstants.ENDPOINT}/storage/buckets/${AppwriteConstants.STORAGE_BUCKET_ID}/files/$it/view?project=${AppwriteConstants.PROJECT_ID}" }
    }

    if (model == null) return

    when {
        message.mediaType?.startsWith("image/") == true -> {
            AsyncImage(
                model = model,
                contentDescription = "Attached image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        }
        message.mediaType?.startsWith("video/") == true -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Videocam, contentDescription = "Video attachment")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Video", style = MaterialTheme.typography.bodyMedium)
            }
        }
        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "File attachment")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attachment", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun MessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach File")
            }
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
