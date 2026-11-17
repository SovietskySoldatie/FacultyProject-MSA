package com.example.securevault

import android.app.Application
import com.example.securevault.data.repository.DefaultVaultRepository
import com.example.securevault.model.PrivateNote
import com.example.securevault.model.PublicNote
import com.example.securevault.viewmodel.AppScreen
import com.example.securevault.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthDao: FakeAuthDao
    private lateinit var fakeNoteDao: FakeNoteDao
    private lateinit var repository: DefaultVaultRepository
    private lateinit var viewModel: VaultViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthDao = FakeAuthDao()
        fakeNoteDao = FakeNoteDao()
        repository = DefaultVaultRepository(fakeAuthDao, fakeNoteDao)
        viewModel = VaultViewModel(Application(), repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialScreenIsAppIntroduction() {
        assertEquals(AppScreen.APP_INTRODUCTION, viewModel.currentScreen.value)
    }

    @Test
    fun testScreen1ToScreen4WhenNoPasswordSet() = runTest(testDispatcher) {
        // Password is not set initially
        advanceUntilIdle()
        assertFalse(viewModel.isPasswordSet.value)

        // When password not set, AppIntroduction offers 1 button navigating to SET_PASSWORD
        viewModel.navigateTo(AppScreen.SET_PASSWORD)
        assertEquals(AppScreen.SET_PASSWORD, viewModel.currentScreen.value)
    }

    @Test
    fun testScreen4SetPasswordValidationAndNavigation() = runTest(testDispatcher) {
        viewModel.navigateTo(AppScreen.SET_PASSWORD)

        // Case 1: Empty password
        viewModel.setNewPassword("", "")
        advanceUntilIdle()
        assertEquals("Password cannot be empty.", viewModel.setPasswordError.value)
        assertEquals(AppScreen.SET_PASSWORD, viewModel.currentScreen.value)

        // Case 2: Mismatch
        viewModel.setNewPassword("pass123", "pass456")
        advanceUntilIdle()
        assertEquals("Passwords do not match.", viewModel.setPasswordError.value)
        assertEquals(AppScreen.SET_PASSWORD, viewModel.currentScreen.value)

        // Case 3: Match
        viewModel.setNewPassword("vaultSecret", "vaultSecret")
        advanceUntilIdle()
        assertNull(viewModel.setPasswordError.value)
        assertEquals(AppScreen.APP_INTRODUCTION, viewModel.currentScreen.value)
        assertTrue(viewModel.isPasswordSet.value)
    }

    @Test
    fun testScreen2LoginSuccessAndFailure() = runTest(testDispatcher) {
        repository.setPassword("correctMaster")
        advanceUntilIdle()
        assertTrue(viewModel.isPasswordSet.value)

        viewModel.navigateTo(AppScreen.LOGIN)

        // Incorrect password
        viewModel.login("wrongMaster")
        advanceUntilIdle()
        assertEquals("Incorrect password. Please try again.", viewModel.loginError.value)
        assertEquals(AppScreen.LOGIN, viewModel.currentScreen.value)

        // Correct password
        viewModel.login("correctMaster")
        advanceUntilIdle()
        assertNull(viewModel.loginError.value)
        assertEquals(AppScreen.MAIN, viewModel.currentScreen.value)
    }

    @Test
    fun testScreen3ResetPasswordFlow() = runTest(testDispatcher) {
        repository.setPassword("oldSecret")
        advanceUntilIdle()

        viewModel.navigateTo(AppScreen.RESET_PASSWORD)

        // Incorrect old password
        viewModel.verifyOldPassword("badSecret")
        advanceUntilIdle()
        assertEquals("Incorrect password. Please try again.", viewModel.resetPasswordError.value)
        assertEquals(AppScreen.RESET_PASSWORD, viewModel.currentScreen.value)

        // Correct old password -> redirects to SET_PASSWORD
        viewModel.verifyOldPassword("oldSecret")
        advanceUntilIdle()
        assertNull(viewModel.resetPasswordError.value)
        assertEquals(AppScreen.SET_PASSWORD, viewModel.currentScreen.value)
    }

    @Test
    fun testScreen5AccessPublicNoteRedirectsToNoteMainScreen() = runTest(testDispatcher) {
        val publicNote = PublicNote(id = 1L, title = "Public Topic", content = "Public info")
        viewModel.accessNote(publicNote)

        assertEquals(AppScreen.NOTE_MAIN, viewModel.currentScreen.value)
        assertEquals(publicNote, viewModel.activeNote.value)
    }

    @Test
    fun testScreen5AccessPrivateNoteRedirectsToNoteLoginScreen() = runTest(testDispatcher) {
        val privateNote = PrivateNote(id = 2L, title = "Secret Topic", content = "Secret info", password = "nPass")
        viewModel.accessNote(privateNote)

        assertEquals(AppScreen.NOTE_LOGIN, viewModel.currentScreen.value)
        assertEquals(privateNote, viewModel.activeNote.value)
    }

    @Test
    fun testScreen6NoteLoginUnlockFlow() = runTest(testDispatcher) {
        val privateNote = PrivateNote(id = 2L, title = "Secret Topic", content = "Secret info", password = "nPass")
        viewModel.accessNote(privateNote)
        assertEquals(AppScreen.NOTE_LOGIN, viewModel.currentScreen.value)

        // Wrong password
        viewModel.unlockNote("wrongKey")
        advanceUntilIdle()
        assertEquals("Incorrect note password.", viewModel.noteLoginError.value)
        assertEquals(AppScreen.NOTE_LOGIN, viewModel.currentScreen.value)

        // Correct password
        viewModel.unlockNote("nPass")
        advanceUntilIdle()
        assertNull(viewModel.noteLoginError.value)
        assertEquals(AppScreen.NOTE_MAIN, viewModel.currentScreen.value)
    }

    @Test
    fun testScreen8NoteEditAndSaveContent() = runTest(testDispatcher) {
        val note = PublicNote(id = 5L, title = "Original", content = "Body")
        repository.insertNote(note)
        advanceUntilIdle()

        viewModel.accessNote(note)
        viewModel.navigateTo(AppScreen.NOTE_EDIT)

        viewModel.updateNoteContent(note, "Edited Title", "Edited Body")
        advanceUntilIdle()

        assertEquals(AppScreen.NOTE_MAIN, viewModel.currentScreen.value)
        assertEquals("Edited Title", viewModel.activeNote.value?.title)
        assertEquals("Edited Body", viewModel.activeNote.value?.content)
    }

    @Test
    fun testScreen9NotePrivacyTogglePublicToPrivate() = runTest(testDispatcher) {
        val publicNote = PublicNote(title = "Open Doc", content = "Text")
        val id = repository.insertNote(publicNote)
        advanceUntilIdle()

        viewModel.accessNote(publicNote)
        viewModel.navigateTo(AppScreen.NOTE_PRIVACY)

        // Provide password -> Converts to PrivateNote (creates new note in DB, deletes old)
        viewModel.saveNotePrivacy(publicNote, "vaultProtect")
        advanceUntilIdle()

        assertEquals(AppScreen.NOTE_MAIN, viewModel.currentScreen.value)
        val active = viewModel.activeNote.value
        assertNotNull(active)
        assertTrue(active!!.isPrivate())
        assertTrue(active is PrivateNote)
        assertEquals("vaultProtect", (active as PrivateNote).password)
        assertNotEquals(id, active.id)
    }

    @Test
    fun testScreen9NotePrivacyTogglePrivateToPublicWhenEmpty() = runTest(testDispatcher) {
        val privateNote = PrivateNote(title = "Hidden Doc", content = "Sensitive Text", password = "oldPw")
        val id = repository.insertNote(privateNote)
        advanceUntilIdle()

        viewModel.accessNote(privateNote)
        viewModel.navigateTo(AppScreen.NOTE_PRIVACY)

        // Save with empty string -> Converts to PublicNote
        viewModel.saveNotePrivacy(privateNote, "")
        advanceUntilIdle()

        assertEquals(AppScreen.NOTE_MAIN, viewModel.currentScreen.value)
        val active = viewModel.activeNote.value
        assertNotNull(active)
        assertFalse(active!!.isPrivate())
        assertTrue(active is PublicNote)
        assertNotEquals(id, active.id)
    }

    @Test
    fun testScreen10CreateNotePublicAndPrivate() = runTest(testDispatcher) {
        // Create Public Note
        viewModel.navigateTo(AppScreen.NOTE_CREATION)
        viewModel.createNote("Shopping", "Apples", isPrivate = false, password = "")
        advanceUntilIdle()

        assertEquals(AppScreen.MAIN, viewModel.currentScreen.value)
        val notes = viewModel.notes.value
        assertEquals(1, notes.size)
        assertFalse(notes[0].isPrivate())
        assertEquals("Shopping", notes[0].title)

        // Create Private Note
        viewModel.navigateTo(AppScreen.NOTE_CREATION)
        viewModel.createNote("Passwords", "Wifi: 1234", isPrivate = true, password = "wifiPassword")
        advanceUntilIdle()

        assertEquals(AppScreen.MAIN, viewModel.currentScreen.value)
        val updatedNotes = viewModel.notes.value
        assertEquals(2, updatedNotes.size)
        val privateCreated = updatedNotes.find { it.title == "Passwords" }
        assertNotNull(privateCreated)
        assertTrue(privateCreated!!.isPrivate())
        assertTrue(privateCreated is PrivateNote)
        assertEquals("wifiPassword", (privateCreated as PrivateNote).password)
    }

    @Test
    fun testDeleteNoteFlow() = runTest(testDispatcher) {
        val note = PublicNote(title = "Delete Me", content = "Bye")
        repository.insertNote(note)
        advanceUntilIdle()

        viewModel.accessNote(note)
        viewModel.deleteNote(note)
        advanceUntilIdle()

        assertEquals(AppScreen.MAIN, viewModel.currentScreen.value)
        assertNull(viewModel.activeNote.value)
        assertTrue(viewModel.notes.value.isEmpty())
    }

    @Test
    fun testNoteCreationToPrivacyScreenLinkPreservesDraftAndCreatesPrivateNote() = runTest(testDispatcher) {
        viewModel.openNoteCreation()
        assertEquals(AppScreen.NOTE_CREATION, viewModel.currentScreen.value)

        // User fills in title and content
        viewModel.updateCreationDraft("Classified Plan", "Project alpha details")
        assertEquals("Classified Plan", viewModel.creationDraftTitle.value)
        assertEquals("Project alpha details", viewModel.creationDraftContent.value)

        // User taps NotePrivacyScreen link
        viewModel.openPrivacyFromCreation("Classified Plan", "Project alpha details")
        assertEquals(AppScreen.NOTE_PRIVACY, viewModel.currentScreen.value)
        assertEquals(AppScreen.NOTE_CREATION, viewModel.privacyReturnScreen)

        // User configures privacy password and saves
        viewModel.savePrivacy("planPassword123")
        assertEquals(AppScreen.NOTE_CREATION, viewModel.currentScreen.value)
        assertEquals("planPassword123", viewModel.creationDraftPassword.value)
        assertEquals("Classified Plan", viewModel.creationDraftTitle.value)
        assertEquals("Project alpha details", viewModel.creationDraftContent.value)

        // User finishes creation
        viewModel.createNote(
            title = viewModel.creationDraftTitle.value,
            content = viewModel.creationDraftContent.value,
            isPrivate = viewModel.creationDraftPassword.value.isNotEmpty(),
            password = viewModel.creationDraftPassword.value
        )
        advanceUntilIdle()

        assertEquals(AppScreen.MAIN, viewModel.currentScreen.value)
        val created = viewModel.notes.value.find { it.title == "Classified Plan" }
        assertNotNull(created)
        assertTrue("Created note must be PrivateNote", created!!.isPrivate())
        assertEquals("planPassword123", (created as PrivateNote).password)
    }

    @Test
    fun testNoteCreationCancelPrivacyPreservesDraft() = runTest(testDispatcher) {
        viewModel.openNoteCreation()
        viewModel.updateCreationDraft("Draft Title", "Draft Content")

        // Go to Privacy
        viewModel.openPrivacyFromCreation("Draft Title", "Draft Content")
        assertEquals(AppScreen.NOTE_PRIVACY, viewModel.currentScreen.value)

        // Click Back / Cancel
        viewModel.cancelPrivacy()
        assertEquals(AppScreen.NOTE_CREATION, viewModel.currentScreen.value)
        assertEquals("Draft Title", viewModel.creationDraftTitle.value)
        assertEquals("Draft Content", viewModel.creationDraftContent.value)
        assertEquals("", viewModel.creationDraftPassword.value)
    }

    @Test
    fun testUnlockNoteWithNullActiveNoteDoesNotUnlock() = runTest(testDispatcher) {
        // No note selected
        viewModel.unlockNote("anyPassword")
        advanceUntilIdle()

        assertEquals("No active note selected.", viewModel.noteLoginError.value)
        assertNotEquals(AppScreen.NOTE_MAIN, viewModel.currentScreen.value)
    }

    @Test
    fun testUpdateNoteContentProducesDistinctInstanceForStateFlow() = runTest(testDispatcher) {
        val original = PublicNote(id = 100L, title = "Original Title", content = "Original Content")
        repository.insertNote(original)
        advanceUntilIdle()

        viewModel.accessNote(original)
        val beforeUpdate = viewModel.activeNote.value
        assertSame(original, beforeUpdate)

        viewModel.updateNoteContent(original, "New Title", "New Content")
        advanceUntilIdle()

        val afterUpdate = viewModel.activeNote.value
        assertNotNull(afterUpdate)
        assertEquals("New Title", afterUpdate!!.title)
        assertEquals("New Content", afterUpdate.content)
        assertNotSame("Must produce a new instance for StateFlow emission", beforeUpdate, afterUpdate)
    }
}
