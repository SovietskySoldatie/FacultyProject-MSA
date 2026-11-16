package com.example.securevault

import com.example.securevault.model.Note
import com.example.securevault.model.PrivateNote
import com.example.securevault.model.PublicNote
import org.junit.Assert.*
import org.junit.Test

class NoteModelTest {

    @Test
    fun testPublicNotePropertiesAndPreview() {
        val note = PublicNote(
            id = 1L,
            title = "Meeting Notes",
            content = "Discuss project architecture and database schemas."
        )

        assertFalse("PublicNote must not be private", note.isPrivate())
        assertEquals(1L, note.id)
        assertEquals("Meeting Notes", note.title)
        assertEquals("Discuss project architecture and database schemas.", note.content)

        val preview = note.getPreview()
        assertNotNull("PublicNote must have preview", preview)
        assertEquals("Discuss project architecture and database schemas.", preview)
    }

    @Test
    fun testPublicNotePreviewTruncation() {
        val longContent = "A".repeat(100)
        val note = PublicNote(id = 2L, title = "Long Note", content = longContent)

        val preview = note.getPreview()
        assertNotNull(preview)
        assertTrue("Preview should be limited in length", preview!!.length <= 65)
        assertTrue("Truncated preview should end with ellipsis", preview.endsWith("..."))
    }

    @Test
    fun testPrivateNotePropertiesAndNoPreview() {
        val privateNote = PrivateNote(
            id = 10L,
            title = "Secret Passwords",
            content = "Server root: admin/supersecret",
            password = "vaultPassword123"
        )

        assertTrue("PrivateNote must be private", privateNote.isPrivate())
        assertEquals(10L, privateNote.id)
        assertEquals("Secret Passwords", privateNote.title)
        assertEquals("Server root: admin/supersecret", privateNote.content)
        assertEquals("vaultPassword123", privateNote.password)

        // Strict requirement: When printing a PrivateNote, preview of content is NOT available
        assertNull("PrivateNote must NOT provide a content preview", privateNote.getPreview())
    }

    @Test
    fun testPrivateNotePasswordVerification() {
        val note = PrivateNote(
            id = 3L,
            title = "Confidential",
            content = "Secret info",
            password = "correct_password"
        )

        assertTrue("Correct password should verify", note.verifyPassword("correct_password"))
        assertFalse("Wrong password should fail verification", note.verifyPassword("wrong_password"))
        assertFalse("Empty password should fail verification", note.verifyPassword(""))
    }

    @Test
    fun testNoteUpdateMethod() {
        val note: Note = PublicNote(id = 5L, title = "Old Title", content = "Old Content")
        note.update("New Title", "New Content")

        assertEquals("New Title", note.title)
        assertEquals("New Content", note.content)
    }

    @Test
    fun testConversionHelpers() {
        val publicNote = PublicNote(id = 1L, title = "Note 1", content = "Content 1")
        val privateConverted = publicNote.toPrivate("newSecret")

        assertTrue(privateConverted.isPrivate())
        assertEquals("Note 1", privateConverted.title)
        assertEquals("Content 1", privateConverted.content)
        assertEquals("newSecret", privateConverted.password)
        assertEquals("Conversion creates a new note with id 0 for DB auto-generation", 0L, privateConverted.id)

        val publicConverted = privateConverted.toPublic()
        assertFalse(publicConverted.isPrivate())
        assertEquals("Note 1", publicConverted.title)
        assertEquals("Content 1", publicConverted.content)
        assertEquals(0L, publicConverted.id)
    }
}
