package com.example.securevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.securevault.ui.*
import com.example.securevault.ui.theme.SecureVaultTheme
import com.example.securevault.viewmodel.AppScreen
import com.example.securevault.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecureVaultTheme {
                VaultApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VaultApp(viewModel: VaultViewModel) {
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeNote by viewModel.activeNote.collectAsState()

    val loginError by viewModel.loginError.collectAsState()
    val resetPasswordError by viewModel.resetPasswordError.collectAsState()
    val setPasswordError by viewModel.setPasswordError.collectAsState()
    val noteLoginError by viewModel.noteLoginError.collectAsState()

    when (currentScreen) {
        // Screen 1: AppIntroductionScreen
        AppScreen.APP_INTRODUCTION -> {
            AppIntroductionScreen(
                isPasswordSet = isPasswordSet,
                onNavigateToSetPassword = { viewModel.navigateTo(AppScreen.SET_PASSWORD) },
                onNavigateToLogin = { viewModel.navigateTo(AppScreen.LOGIN) },
                onNavigateToResetPassword = { viewModel.navigateTo(AppScreen.RESET_PASSWORD) }
            )
        }

        // Screen 2: LoginScreen
        AppScreen.LOGIN -> {
            LoginScreen(
                errorMessage = loginError,
                onLogin = { pass -> viewModel.login(pass) },
                onBack = { viewModel.navigateTo(AppScreen.APP_INTRODUCTION) }
            )
        }

        // Screen 3: ResetPasswordScreen
        AppScreen.RESET_PASSWORD -> {
            ResetPasswordScreen(
                errorMessage = resetPasswordError,
                onVerifyOldPassword = { oldPass -> viewModel.verifyOldPassword(oldPass) },
                onBack = { viewModel.navigateTo(AppScreen.APP_INTRODUCTION) }
            )
        }

        // Screen 4: SetPasswordScreen
        AppScreen.SET_PASSWORD -> {
            SetPasswordScreen(
                errorMessage = setPasswordError,
                onSavePassword = { newPass, confirmPass ->
                    viewModel.setNewPassword(newPass, confirmPass)
                },
                onBack = { viewModel.navigateTo(AppScreen.APP_INTRODUCTION) }
            )
        }

        // Screen 5: MainScreen
        AppScreen.MAIN -> {
            MainScreen(
                notes = notes,
                onAccessNote = { note -> viewModel.accessNote(note) },
                onCreateNote = { viewModel.navigateTo(AppScreen.NOTE_CREATION) },
                onExit = { viewModel.navigateTo(AppScreen.APP_INTRODUCTION) }
            )
        }

        // Screen 6: NoteLoginScreen
        AppScreen.NOTE_LOGIN -> {
            val note = activeNote
            NoteLoginScreen(
                noteTitle = note?.title ?: "Note",
                errorMessage = noteLoginError,
                onUnlock = { passwordCandidate ->
                    viewModel.unlockNote(passwordCandidate)
                },
                onBack = { viewModel.navigateTo(AppScreen.MAIN) }
            )
        }

        // Screen 7: NoteMainScreen
        AppScreen.NOTE_MAIN -> {
            val note = activeNote
            if (note != null) {
                NoteMainScreen(
                    note = note,
                    onBack = { viewModel.navigateTo(AppScreen.MAIN) },
                    onEdit = { viewModel.navigateTo(AppScreen.NOTE_EDIT) },
                    onPrivacy = { viewModel.navigateTo(AppScreen.NOTE_PRIVACY) },
                    onDelete = { viewModel.deleteNote(note) }
                )
            } else {
                viewModel.navigateTo(AppScreen.MAIN)
            }
        }

        // Screen 8: NoteEditScreen
        AppScreen.NOTE_EDIT -> {
            val note = activeNote
            if (note != null) {
                NoteEditScreen(
                    initialNote = note,
                    onSave = { newTitle, newContent ->
                        viewModel.updateNoteContent(note, newTitle, newContent)
                    },
                    onBack = { viewModel.navigateTo(AppScreen.NOTE_MAIN) }
                )
            } else {
                viewModel.navigateTo(AppScreen.MAIN)
            }
        }

        // Screen 9: NotePrivacyScreen
        AppScreen.NOTE_PRIVACY -> {
            val note = activeNote
            if (note != null) {
                NotePrivacyScreen(
                    note = note,
                    onSavePrivacy = { passwordInput ->
                        viewModel.saveNotePrivacy(note, passwordInput)
                    },
                    onBack = { viewModel.navigateTo(AppScreen.NOTE_MAIN) }
                )
            } else {
                viewModel.navigateTo(AppScreen.MAIN)
            }
        }

        // Screen 10: NoteCreationScreen
        AppScreen.NOTE_CREATION -> {
            NoteCreationScreen(
                onCreateNote = { title, content, isPrivate, password ->
                    viewModel.createNote(title, content, isPrivate, password)
                },
                onBack = { viewModel.navigateTo(AppScreen.MAIN) }
            )
        }
    }
}
