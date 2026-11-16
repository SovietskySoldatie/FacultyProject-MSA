package com.example.securevault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.securevault.data.db.AppDatabase
import com.example.securevault.data.repository.DefaultVaultRepository
import com.example.securevault.data.repository.VaultRepository
import com.example.securevault.model.Note
import com.example.securevault.model.PrivateNote
import com.example.securevault.model.PublicNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Screen categories defined in the requirements.
 */
enum class AppScreen {
    APP_INTRODUCTION,   // Screen 1
    LOGIN,              // Screen 2
    RESET_PASSWORD,     // Screen 3
    SET_PASSWORD,       // Screen 4
    MAIN,               // Screen 5
    NOTE_LOGIN,         // Screen 6
    NOTE_MAIN,          // Screen 7
    NOTE_EDIT,          // Screen 8
    NOTE_PRIVACY,       // Screen 9
    NOTE_CREATION       // Screen 10
}

/**
 * ViewModel acting as Controller / Coordinator in the MVC / MVVM architecture.
 */
open class VaultViewModel(
    application: Application,
    private val repository: VaultRepository
) : AndroidViewModel(application) {

    // Secondary constructor used by default Android ViewModelProvider
    constructor(application: Application) : this(
        application,
        DefaultVaultRepository(
            AppDatabase.getDatabase(application).authDao(),
            AppDatabase.getDatabase(application).noteDao()
        )
    )

    // Flow observing whether the login password is set in DB Schema 1
    val isPasswordSet: StateFlow<Boolean> = repository.isPasswordSetFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    // Flow observing all notes in DB Schema 2
    val notes: StateFlow<List<Note>> = repository.getNotesFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Screen navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.APP_INTRODUCTION)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Currently selected note for viewing / editing / privacy / unlocking
    private val _activeNote = MutableStateFlow<Note?>(null)
    val activeNote: StateFlow<Note?> = _activeNote.asStateFlow()

    // UI Feedback messages
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _resetPasswordError = MutableStateFlow<String?>(null)
    val resetPasswordError: StateFlow<String?> = _resetPasswordError.asStateFlow()

    private val _setPasswordError = MutableStateFlow<String?>(null)
    val setPasswordError: StateFlow<String?> = _setPasswordError.asStateFlow()

    private val _noteLoginError = MutableStateFlow<String?>(null)
    val noteLoginError: StateFlow<String?> = _noteLoginError.asStateFlow()

    // Note Creation draft state (preserves inputs when navigating between NoteCreationScreen and NotePrivacyScreen)
    private val _creationDraftTitle = MutableStateFlow("")
    val creationDraftTitle: StateFlow<String> = _creationDraftTitle.asStateFlow()

    private val _creationDraftContent = MutableStateFlow("")
    val creationDraftContent: StateFlow<String> = _creationDraftContent.asStateFlow()

    private val _creationDraftPassword = MutableStateFlow("")
    val creationDraftPassword: StateFlow<String> = _creationDraftPassword.asStateFlow()

    // Tracks navigation source for NotePrivacyScreen (either NOTE_MAIN or NOTE_CREATION)
    private var privacySourceScreen: AppScreen = AppScreen.NOTE_MAIN
    val privacyReturnScreen: AppScreen get() = privacySourceScreen

    fun navigateTo(screen: AppScreen) {
        // Clear screen-specific errors on navigation
        when (screen) {
            AppScreen.LOGIN -> _loginError.value = null
            AppScreen.RESET_PASSWORD -> _resetPasswordError.value = null
            AppScreen.SET_PASSWORD -> _setPasswordError.value = null
            AppScreen.NOTE_LOGIN -> _noteLoginError.value = null
            else -> {}
        }
        _currentScreen.value = screen
    }

    fun selectNote(note: Note) {
        _activeNote.value = note
    }

    // --- Authentication Actions (Screen 1, 2, 3, 4) ---

    fun login(password: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (repository.verifyPassword(password)) {
                _loginError.value = null
                _currentScreen.value = AppScreen.MAIN
                onSuccess()
            } else {
                _loginError.value = "Incorrect password. Please try again."
            }
        }
    }

    fun verifyOldPassword(oldPassword: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (repository.verifyPassword(oldPassword)) {
                _resetPasswordError.value = null
                _currentScreen.value = AppScreen.SET_PASSWORD
                onSuccess()
            } else {
                _resetPasswordError.value = "Incorrect password. Please try again."
            }
        }
    }

    fun setNewPassword(newPass: String, confirmPass: String, onSuccess: () -> Unit = {}) {
        if (newPass.isEmpty()) {
            _setPasswordError.value = "Password cannot be empty."
            return
        }
        if (newPass != confirmPass) {
            // Note: Does NOT delete inserted passwords, preserves them
            _setPasswordError.value = "Passwords do not match."
            return
        }

        viewModelScope.launch {
            repository.setPassword(newPass)
            _setPasswordError.value = null
            _currentScreen.value = AppScreen.APP_INTRODUCTION
            onSuccess()
        }
    }

    // --- Note Access Actions (Screen 5, 6) ---

    fun accessNote(note: Note) {
        _activeNote.value = note
        if (note is PrivateNote) {
            _noteLoginError.value = null
            _currentScreen.value = AppScreen.NOTE_LOGIN
        } else {
            _currentScreen.value = AppScreen.NOTE_MAIN
        }
    }

    fun unlockNote(passwordCandidate: String, onSuccess: () -> Unit = {}) {
        val current = _activeNote.value
        if (current == null) {
            _noteLoginError.value = "No active note selected."
            return
        }

        if (current is PrivateNote) {
            if (current.verifyPassword(passwordCandidate)) {
                _noteLoginError.value = null
                _currentScreen.value = AppScreen.NOTE_MAIN
                onSuccess()
            } else {
                _noteLoginError.value = "Incorrect note password."
            }
        } else {
            _noteLoginError.value = null
            _currentScreen.value = AppScreen.NOTE_MAIN
            onSuccess()
        }
    }

    // --- Note CRUD Actions (Screen 7, 8, 9, 10) ---

    fun openNoteCreation() {
        _creationDraftTitle.value = ""
        _creationDraftContent.value = ""
        _creationDraftPassword.value = ""
        _currentScreen.value = AppScreen.NOTE_CREATION
    }

    fun updateCreationDraft(title: String, content: String) {
        _creationDraftTitle.value = title
        _creationDraftContent.value = content
    }

    fun openPrivacyFromMain(note: Note) {
        _activeNote.value = note
        privacySourceScreen = AppScreen.NOTE_MAIN
        _currentScreen.value = AppScreen.NOTE_PRIVACY
    }

    fun openPrivacyFromCreation(title: String, content: String) {
        _creationDraftTitle.value = title
        _creationDraftContent.value = content
        privacySourceScreen = AppScreen.NOTE_CREATION
        _currentScreen.value = AppScreen.NOTE_PRIVACY
    }

    fun cancelPrivacy() {
        _currentScreen.value = privacySourceScreen
    }

    fun savePrivacy(passwordInput: String) {
        if (privacySourceScreen == AppScreen.NOTE_CREATION) {
            _creationDraftPassword.value = passwordInput.trim()
            _currentScreen.value = AppScreen.NOTE_CREATION
        } else {
            val note = _activeNote.value
            if (note != null) {
                saveNotePrivacy(note, passwordInput)
            } else {
                _currentScreen.value = AppScreen.MAIN
            }
        }
    }

    fun createNote(title: String, content: String, isPrivate: Boolean, password: String = "") {
        viewModelScope.launch {
            val newNote: Note = if (isPrivate && password.isNotBlank()) {
                PrivateNote(
                    id = 0,
                    title = title,
                    content = content,
                    password = password.trim()
                )
            } else {
                PublicNote(
                    id = 0,
                    title = title,
                    content = content
                )
            }

            val generatedId = repository.insertNote(newNote)
            newNote.id = generatedId
            _activeNote.value = newNote
            _creationDraftTitle.value = ""
            _creationDraftContent.value = ""
            _creationDraftPassword.value = ""
            _currentScreen.value = AppScreen.MAIN
        }
    }

    fun updateNoteContent(note: Note, newTitle: String, newContent: String) {
        viewModelScope.launch {
            val updated = note.copyWith(newTitle, newContent)
            repository.updateNote(updated)
            _activeNote.value = updated
            _currentScreen.value = AppScreen.NOTE_MAIN
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _activeNote.value = null
            _currentScreen.value = AppScreen.MAIN
        }
    }

    fun saveNotePrivacy(note: Note, passwordInput: String) {
        viewModelScope.launch {
            val updated = repository.changeNotePrivacy(note, passwordInput)
            _activeNote.value = updated
            _currentScreen.value = AppScreen.NOTE_MAIN
        }
    }
}
