package com.example.securevault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.securevault.model.Note
import com.example.securevault.model.PrivateNote
import com.example.securevault.model.PublicNote

/**
 * Screen 5: MainScreen
 *
 * Requirements:
 * "The 5th category (MainScreen) will print all notes in a list
 * (check example at "C:\Users\Administrator\Desktop\Facultate\An_4_Provisional\SMA\Lab06").
 * This page's back button will redirect to the AppIntroductionScreen and will be called "Exit".
 * When printing a PublicNote, a title AND a preview (short, limited) of the note's content are available
 * and the button to access the note redirects the screen directly to the Note's NoteMainScreen.
 * When printing a PrivateNote, a title AND NOT a preview (short, limited) of the note's content are available
 * and the button to access the note redirects the screen directly to the Note's NoteLoginScreen.
 * One more button is added to create (redirects to "NoteCreationScreen")."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    notes: List<Note>,
    onAccessNote: (Note) -> Unit,
    onCreateNote: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Notes") },
                navigationIcon = {
                    TextButton(onClick = onExit) {
                        Text("Exit", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No notes found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCreateNote) {
                        Text("Create Your First Note")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteListItemCard(
                        note = note,
                        onAccessNote = { onAccessNote(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteListItemCard(
    note: Note,
    onAccessNote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Privacy Badge
                Surface(
                    color = if (note.isPrivate()) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (note.isPrivate()) "Private" else "Public",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (note.isPrivate()) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (note) {
                is PublicNote -> {
                    // Title AND preview (short, limited) available; left empty if note has no content
                    val preview = note.getPreview().orEmpty()
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is PrivateNote -> {
                    // Title AND NOT a preview of note content
                    Text(
                        text = "[Locked - Password Protected]",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onAccessNote) {
                    Text(if (note.isPrivate()) "Unlock Note" else "Open Note")
                }
            }
        }
    }
}
