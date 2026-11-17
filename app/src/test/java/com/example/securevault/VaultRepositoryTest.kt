package com.example.securevault

import com.example.securevault.data.dao.AuthDao
import com.example.securevault.data.dao.NoteDao
import com.example.securevault.data.entity.AuthEntity
import com.example.securevault.data.entity.NoteEntity
import com.example.securevault.data.repository.DefaultVaultRepository
import com.example.securevault.model.PrivateNote
import com.example.securevault.model.PublicNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * In-memory test implementation of AuthDao and NoteDao to simulate DB operations.
 */
class FakeAuthDao : AuthDao {
    private val authState = MutableStateFlow<AuthEntity?>(null)

    override fun getAuthFlow(): Flow<AuthEntity?> = authState.asStateFlow()

    override suspend fun getAuth(): AuthEntity? = authState.value

    override suspend fun setAuth(auth: AuthEntity) {
        authState.value = auth
    }

    override suspend fun clearAuth() {
        authState.value = null
    }
}

class FakeNoteDao : NoteDao {
    private var currentId = 1L
    private val notesList = mutableListOf<NoteEntity>()
    private val notesFlow = MutableStateFlow<List<NoteEntity>>(emptyList())

    override fun getAllNotesFlow(): Flow<List<NoteEntity>> = notesFlow.asStateFlow()

    override suspend fun getNoteById(id: Long): NoteEntity? {
        return notesList.find { it.id == id }
    }

    override suspend fun insertNote(note: NoteEntity): Long {
        val generatedId = if (note.id == 0L) currentId++ else note.id
        val entityWithId = note.copy(id = generatedId)
        notesList.removeIf { it.id == generatedId }
        notesList.add(0, entityWithId) // newest first
        notesFlow.value = notesList.toList()
        return generatedId
    }

    override suspend fun updateNote(note: NoteEntity) {
        val index = notesList.indexOfFirst { it.id == note.id }
        if (index != -1) {
            notesList[index] = note
            notesFlow.value = notesList.toList()
        }
    }

    override suspend fun deleteNote(note: NoteEntity) {
        deleteNoteById(note.id)
    }

    override suspend fun deleteNoteById(id: Long) {
        notesList.removeIf { it.id == id }
        notesFlow.value = notesList.toList()
    }
}

class VaultRepositoryTest {

    private lateinit var fakeAuthDao: FakeAuthDao
    private lateinit var fakeNoteDao: FakeNoteDao
    private lateinit var repository: DefaultVaultRepository

    @Before
    fun setUp() {
        fakeAuthDao = FakeAuthDao()
        fakeNoteDao = FakeNoteDao()
        repository = DefaultVaultRepository(fakeAuthDao, fakeNoteDao)
    }

    @Test
    fun testInitialAuthStatusNotSet() = runBlocking {
        assertFalse("Initially, no password should be set", repository.isPasswordSet())
    }

    @Test
    fun testSetAndVerifyPassword() = runBlocking {
        repository.setPassword("supersecret")
        assertTrue("Password should now be set", repository.isPasswordSet())

        assertTrue("Verification with correct password should pass", repository.verifyPassword("supersecret"))
        assertFalse("Verification with wrong password should fail", repository.verifyPassword("wrongpass"))
    }

    @Test
    fun testInsertAndRetrieveNotes() = runBlocking {
        val publicNote = PublicNote(title = "Shopping List", content = "Apples, Milk")
        val generatedId = repository.insertNote(publicNote)

        assertTrue("DB engine should generate a positive ID", generatedId > 0)
        assertEquals(generatedId, publicNote.id)

        val retrieved = repository.getNoteById(generatedId)
        assertNotNull(retrieved)
        assertEquals("Shopping List", retrieved!!.title)
        assertEquals("Apples, Milk", retrieved.content)
        assertFalse(retrieved.isPrivate())
    }

    @Test
    fun testUpdateNote() = runBlocking {
        val note = PublicNote(title = "Original Title", content = "Original Content")
        val id = repository.insertNote(note)

        note.update("Edited Title", "Edited Content")
        repository.updateNote(note)

        val updated = repository.getNoteById(id)
        assertNotNull(updated)
        assertEquals("Edited Title", updated!!.title)
        assertEquals("Edited Content", updated.content)
    }

    @Test
    fun testDeleteNote() = runBlocking {
        val note = PublicNote(title = "To Delete", content = "Content")
        val id = repository.insertNote(note)
        assertNotNull(repository.getNoteById(id))

        repository.deleteNote(note)
        assertNull("Note should be removed from database", repository.getNoteById(id))
    }

    @Test
    fun testChangePrivacyFromPublicToPrivateCreatesNewNoteThenDeletesOld() = runBlocking {
        // Create initial PublicNote
        val publicNote = PublicNote(title = "Public Plan", content = "Public Roadmap")
        val originalId = repository.insertNote(publicNote)
        assertEquals(1L, originalId)

        // Change privacy to private by supplying a password
        val convertedNote = repository.changeNotePrivacy(publicNote, "newVaultPass")

        assertTrue("Converted note must be a PrivateNote", convertedNote.isPrivate())
        assertTrue("Converted note must be instance of PrivateNote", convertedNote is PrivateNote)
        assertEquals("newVaultPass", (convertedNote as PrivateNote).password)

        // Strict Requirement:
        // "When changing the privacy of a Note, a new Note is created first, and then the old Note is deleted.
        // Let DB IDs be generated by the DB engine."
        assertNotEquals("New note must have a new DB engine generated ID", originalId, convertedNote.id)
        assertNull("Old note with original ID must have been deleted", repository.getNoteById(originalId))
        assertNotNull("New note must exist in database", repository.getNoteById(convertedNote.id))
    }

    @Test
    fun testChangePrivacyFromPrivateToPublicWhenPasswordLeftEmpty() = runBlocking {
        // Create initial PrivateNote
        val privateNote = PrivateNote(title = "Private Diary", content = "Personal thoughts", password = "secret")
        val originalId = repository.insertNote(privateNote)

        // Leave password empty -> changes to PublicNote
        val convertedNote = repository.changeNotePrivacy(privateNote, "")

        assertFalse("Converted note must be public", convertedNote.isPrivate())
        assertTrue("Converted note must be instance of PublicNote", convertedNote is PublicNote)

        // DB ID generated by DB engine for new note, old note deleted
        assertNotEquals("New note must have a distinct generated ID", originalId, convertedNote.id)
        assertNull("Old private note must be deleted", repository.getNoteById(originalId))
        assertNotNull("New public note must exist in DB", repository.getNoteById(convertedNote.id))
    }

    @Test
    fun testChangePasswordOnPrivateNoteKeepsSameNoteId() = runBlocking {
        val privateNote = PrivateNote(title = "Vault Secret", content = "Keys", password = "oldPass")
        val id = repository.insertNote(privateNote)

        // Staying private with updated password
        val updatedNote = repository.changeNotePrivacy(privateNote, "newUpdatedPass")

        assertTrue(updatedNote.isPrivate())
        assertEquals(id, updatedNote.id)
        assertEquals("newUpdatedPass", (updatedNote as PrivateNote).password)
    }
}
