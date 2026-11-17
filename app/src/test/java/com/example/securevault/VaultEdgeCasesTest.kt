package com.example.securevault

import android.app.Application
import com.example.securevault.data.entity.NoteEntity
import com.example.securevault.data.repository.DefaultVaultRepository
import com.example.securevault.model.PrivateNote
import com.example.securevault.model.PublicNote
import com.example.securevault.viewmodel.AppScreen
import com.example.securevault.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
class VaultEdgeCasesTest {

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
    fun testPreviewBoundaryValues() {
        // Empty content - preview must be left empty
        val emptyNote = PublicNote(title = "Empty", content = "")
        assertEquals("", emptyNote.getPreview())
        assertTrue("Preview for note with empty content must be empty", emptyNote.getPreview().isNullOrEmpty())
        assertNotEquals("(Empty content)", emptyNote.getPreview())

        // Whitespace only - preview must be left empty
        val whitespaceNote = PublicNote(title = "Whitespace", content = "   \n\t  ")
        assertEquals("", whitespaceNote.getPreview())
        assertTrue("Preview for whitespace-only content must be empty", whitespaceNote.getPreview().isNullOrEmpty())

        // PrivateNote with empty content - preview must remain null
        val privateEmptyNote = PrivateNote(title = "Private Empty", content = "", password = "pw")
        assertNull("PrivateNote preview must strictly remain null", privateEmptyNote.getPreview())

        // Exactly 60 characters
        val exact60 = "1234567890".repeat(6) // length 60
        val note60 = PublicNote(title = "Exact 60", content = exact60)
        assertEquals(exact60, note60.getPreview())
        assertFalse(note60.getPreview()!!.endsWith("..."))

        // 61 characters
        val len61 = exact60 + "x"
        val note61 = PublicNote(title = "61 chars", content = len61)
        val preview61 = note61.getPreview()
        assertNotNull(preview61)
        assertTrue(preview61!!.endsWith("..."))
        assertEquals(63, preview61.length) // 60 chars + "..."
    }

    @Test
    fun testPreviewTransitionsWhenContentBecomesEmpty() {
        val note = PublicNote(title = "Draft", content = "Initial content")
        assertEquals("Initial content", note.getPreview())

        // Clearing content
        note.update(newTitle = "Draft", newContent = "")
        assertEquals("", note.getPreview())
        assertTrue("Cleared note preview must be empty", note.getPreview()!!.isEmpty())

        // Changing to whitespace
        note.update(newTitle = "Draft", newContent = "   \t ")
        assertEquals("", note.getPreview())
        assertTrue("Whitespace note preview must be empty", note.getPreview()!!.isEmpty())
    }

    @Test
    fun testEntityDomainRoundTrip() {
        // Public Note
        val publicNote = PublicNote(id = 42L, title = "Round Trip Public", content = "Some content")
        val publicEntity = NoteEntity.fromDomain(publicNote)
        assertEquals(42L, publicEntity.id)
        assertEquals("Round Trip Public", publicEntity.title)
        assertEquals("Some content", publicEntity.content)
        assertFalse(publicEntity.isPrivate)
        assertNull(publicEntity.password)

        val restoredPublic = publicEntity.toDomain()
        assertTrue(restoredPublic is PublicNote)
        assertEquals(42L, restoredPublic.id)
        assertEquals("Round Trip Public", restoredPublic.title)
        assertEquals("Some content", restoredPublic.content)
        assertFalse(restoredPublic.isPrivate())

        // Private Note
        val privateNote = PrivateNote(id = 84L, title = "Round Trip Private", content = "Secret", password = "pWord")
        val privateEntity = NoteEntity.fromDomain(privateNote)
        assertEquals(84L, privateEntity.id)
        assertTrue(privateEntity.isPrivate)
        assertEquals("pWord", privateEntity.password)

        val restoredPrivate = privateEntity.toDomain()
        assertTrue(restoredPrivate is PrivateNote)
        assertEquals(84L, restoredPrivate.id)
        assertTrue(restoredPrivate.isPrivate())
        assertEquals("pWord", (restoredPrivate as PrivateNote).password)
    }

    @Test
    fun testMultiplePrivacyTransitions() = runBlocking {
        // Step 1: Start as Public
        var note = repository.getNoteById(repository.insertNote(PublicNote(title = "Cycling Note", content = "Cycle")))!!
        val id1 = note.id
        assertFalse(note.isPrivate())

        // Step 2: Public -> Private
        note = repository.changeNotePrivacy(note, "pass1")
        val id2 = note.id
        assertTrue(note.isPrivate())
        assertNotEquals(id1, id2)
        assertNull("Old note must be deleted", repository.getNoteById(id1))
        assertNotNull("New private note must exist", repository.getNoteById(id2))

        // Step 3: Private -> Public (empty password)
        note = repository.changeNotePrivacy(note, "")
        val id3 = note.id
        assertFalse(note.isPrivate())
        assertNotEquals(id2, id3)
        assertNull("Old private note must be deleted", repository.getNoteById(id2))
        assertNotNull("New public note must exist", repository.getNoteById(id3))

        // Step 4: Public -> Private again
        note = repository.changeNotePrivacy(note, "pass2")
        val id4 = note.id
        assertTrue(note.isPrivate())
        assertNotEquals(id3, id4)
        assertNull("Old public note must be deleted", repository.getNoteById(id3))
        assertNotNull("New private note must exist", repository.getNoteById(id4))
    }

    @Test
    fun testPrivacyChangeWithWhitespaceOnlyPasswordTreatsAsPublic() = runBlocking {
        val privateNote = PrivateNote(title = "Secret", content = "Text", password = "key")
        repository.insertNote(privateNote)

        // Password with spaces only
        val resultNote = repository.changeNotePrivacy(privateNote, "   ")
        assertFalse("Whitespace-only password should treat note as public", resultNote.isPrivate())
        assertTrue(resultNote is PublicNote)
    }

    @Test
    fun testDeletingSpecificNotePreservesOtherNotes() = runBlocking {
        val note1 = PublicNote(title = "Note 1", content = "Content 1")
        val note2 = PublicNote(title = "Note 2", content = "Content 2")
        val note3 = PrivateNote(title = "Note 3", content = "Content 3", password = "pw")

        val id1 = repository.insertNote(note1)
        val id2 = repository.insertNote(note2)
        val id3 = repository.insertNote(note3)

        // Delete note2
        repository.deleteNote(note2)

        assertNotNull(repository.getNoteById(id1))
        assertNull("Note 2 should be deleted", repository.getNoteById(id2))
        assertNotNull(repository.getNoteById(id3))
    }

    @Test
    fun testResetPasswordWhenPasswordNotSetFailsGracefully() = runTest(testDispatcher) {
        viewModel.navigateTo(AppScreen.RESET_PASSWORD)
        viewModel.verifyOldPassword("anyPassword")
        advanceUntilIdle()

        assertEquals("Incorrect password. Please try again.", viewModel.resetPasswordError.value)
        assertEquals(AppScreen.RESET_PASSWORD, viewModel.currentScreen.value)
    }

    @Test
    fun testNavigationBackPaths() = runTest(testDispatcher) {
        // Back from Login -> AppIntroduction
        viewModel.navigateTo(AppScreen.LOGIN)
        viewModel.navigateTo(AppScreen.APP_INTRODUCTION)
        assertEquals(AppScreen.APP_INTRODUCTION, viewModel.currentScreen.value)

        // Back from ResetPassword -> AppIntroduction
        viewModel.navigateTo(AppScreen.RESET_PASSWORD)
        viewModel.navigateTo(AppScreen.APP_INTRODUCTION)
        assertEquals(AppScreen.APP_INTRODUCTION, viewModel.currentScreen.value)

        // Back from SetPassword -> AppIntroduction
        viewModel.navigateTo(AppScreen.SET_PASSWORD)
        viewModel.navigateTo(AppScreen.APP_INTRODUCTION)
        assertEquals(AppScreen.APP_INTRODUCTION, viewModel.currentScreen.value)

        // Exit from Main -> AppIntroduction
        viewModel.navigateTo(AppScreen.MAIN)
        viewModel.navigateTo(AppScreen.APP_INTRODUCTION)
        assertEquals(AppScreen.APP_INTRODUCTION, viewModel.currentScreen.value)

        // Back from NoteLogin -> Main
        viewModel.navigateTo(AppScreen.NOTE_LOGIN)
        viewModel.navigateTo(AppScreen.MAIN)
        assertEquals(AppScreen.MAIN, viewModel.currentScreen.value)

        // Back from NoteCreation -> Main
        viewModel.navigateTo(AppScreen.NOTE_CREATION)
        viewModel.navigateTo(AppScreen.MAIN)
        assertEquals(AppScreen.MAIN, viewModel.currentScreen.value)

        // Privacy cancel back to NoteMain
        val note = PublicNote(id = 1L, title = "Title", content = "Content")
        viewModel.openPrivacyFromMain(note)
        assertEquals(AppScreen.NOTE_PRIVACY, viewModel.currentScreen.value)
        viewModel.cancelPrivacy()
        assertEquals(AppScreen.NOTE_MAIN, viewModel.currentScreen.value)

        // Privacy cancel back to NoteCreation
        viewModel.openPrivacyFromCreation("Draft", "Text")
        assertEquals(AppScreen.NOTE_PRIVACY, viewModel.currentScreen.value)
        viewModel.cancelPrivacy()
        assertEquals(AppScreen.NOTE_CREATION, viewModel.currentScreen.value)
    }

    @Test
    fun testReplaceNoteWithNewPrivacyDefaultDaoImplementation() = runBlocking {
        val originalEntity = NoteEntity(id = 0, title = "Doc", content = "Text", isPrivate = false)
        val originalId = fakeNoteDao.insertNote(originalEntity)
        assertEquals(1L, originalId)

        val newEntity = NoteEntity(id = 0, title = "Doc", content = "Text", isPrivate = true, password = "secret")
        val newId = fakeNoteDao.replaceNoteWithNewPrivacy(newEntity, originalId)

        assertEquals("New ID generated", 2L, newId)
        assertNull("Original ID deleted", fakeNoteDao.getNoteById(originalId))
        assertNotNull("New entity exists", fakeNoteDao.getNoteById(newId))
    }
}
