package com.example.securevault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.securevault.model.Note
import com.example.securevault.model.PrivateNote

/**
 * Screen 6: NoteLoginScreen
 *
 * Requirements:
 * "The 6th category (NoteLoginScreen) will have 1 textbox for the password insert.
 * On match, It redirects to that Note's NoteMainScreen. On fail, it prints a message.
 * Also has a back button."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteLoginScreen(
    noteTitle: String,
    errorMessage: String?,
    onUnlock: (password: String) -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock Note") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Protected Note: \"$noteTitle\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Note Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onUnlock(password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Unlock")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Back")
            }
        }
    }
}

/**
 * Screen 7: NoteMainScreen
 *
 * Requirements:
 * "The 7th category (NoteMainScreen) will show the Note's title at the top and the text below it.
 * The page has 3 buttons: a back button (back to MainScreen), an edit button (redirects to NoteEditScreen),
 * a privacy button (redirects to NotePrivacyScreen),a delete button."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteMainScreen(
    note: Note,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPrivacy: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Note") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Note's title at the top
            Text(
                text = note.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Privacy indicator
            Text(
                text = if (note.isPrivate()) "Status: Private Note (Protected)" else "Status: Public Note",
                style = MaterialTheme.typography.labelMedium,
                color = if (note.isPrivate()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Note's text below it
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Back, Edit, Privacy, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPrivacy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Privacy")
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

/**
 * Screen 8: NoteEditScreen
 *
 * Requirements:
 * "The 8th category (NoteEditScreen) will allow the edit of the title and the content
 * (check the example, I do not remember how I did it then)."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    initialNote: Note,
    onSave: (newTitle: String, newContent: String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(initialNote.title) }
    var content by remember { mutableStateOf(initialNote.content) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Note") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = { onSave(title, content) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/**
 * Screen 9: NotePrivacyScreen
 *
 * Requirements:
 * "The 9th category (NotePrivacyScreen) will show the current privacy options for the note
 * (at the moment, it prints a password textbox with the password and the possibility to change it).
 * If the password textbox is left empty, the Note's privacy status is changed to public.
 * If a password is chnaged, it will be updated in the Model (from MVC) and, if needed, the Note's privacy status
 * is changed to public. These changes happen only when pressing the save button. Also a back button."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePrivacyScreen(
    note: Note,
    onSavePrivacy: (passwordInput: String) -> Unit,
    onBack: () -> Unit
) {
    // Current password is shown if note is currently PrivateNote; empty if PublicNote
    val currentPassword = (note as? PrivateNote)?.password ?: ""
    var passwordInput by remember { mutableStateOf(currentPassword) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Privacy Settings") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Privacy for: \"${note.title}\"",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (note.isPrivate()) {
                    "Currently: Private Note (Password Protected)"
                } else {
                    "Currently: Public Note (Unprotected)"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (note.isPrivate()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Note Password") },
                supportingText = {
                    Text("Leave empty to make this note Public. Enter a password to make/keep it Private.")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onSavePrivacy(passwordInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save Privacy Settings")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Back")
            }
        }
    }
}

/**
 * Screen 10: NoteCreationScreen
 *
 * Requirements:
 * "The 10th category (NoteCreationScreen) is basically identical to the NoteEditScreen,
 * only it creates a Note object and adds it to the DB (including the NotePrivacyScreen link)."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCreationScreen(
    onCreateNote: (title: String, content: String, isPrivate: Boolean, password: String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    val isPrivate = password.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Note") },
                actions = {
                    // Link to privacy options
                    TextButton(onClick = { showPrivacyDialog = true }) {
                        Text(if (isPrivate) "Privacy: Private" else "Privacy: Public")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy configuration link bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPrivate) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPrivate) "Privacy: Private (Password set)" else "Privacy: Public (No password)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { showPrivacyDialog = true }) {
                        Text("Configure Privacy")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onCreateNote(title, content, isPrivate, password)
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text("Create Note")
                }
            }
        }
    }

    // Modal Privacy configuration matching NotePrivacyScreen requirements
    if (showPrivacyDialog) {
        var tempPassword by remember { mutableStateOf(password) }
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Note Privacy Options") },
            text = {
                Column {
                    Text(
                        text = "Set a password to create a PrivateNote. Leave empty for PublicNote.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    password = tempPassword
                    showPrivacyDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPrivacyDialog = false }) {
                    Text("Back")
                }
            }
        )
    }
}
