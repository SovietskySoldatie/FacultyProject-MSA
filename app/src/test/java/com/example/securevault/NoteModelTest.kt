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

    @Test
    fun testEqualsAndHashCodeSymmetryBetweenPublicAndPrivateNotes() {
        val pub = PublicNote(id = 10L, title = "Same Title", content = "Same Content")
        val priv = PrivateNote(id = 10L, title = "Same Title", content = "Same Content", password = "secret")
        val pubClone = PublicNote(id = 10L, title = "Same Title", content = "Same Content")
        val privClone = PrivateNote(id = 10L, title = "Same Title", content = "Same Content", password = "secret")
        val privDifferentPass = PrivateNote(id = 10L, title = "Same Title", content = "Same Content", password = "other")

        // Symmetry contract: pub == priv MUST equal priv == pub (both false)
        assertFalse("PublicNote and PrivateNote must not be equal", pub == priv)
        assertFalse("PrivateNote and PublicNote must not be equal", priv == pub)
        assertEquals(pub == priv, priv == pub)

        // Clones of same type must be equal
        assertTrue("Matching PublicNotes must be equal", pub == pubClone)
        assertEquals(pub.hashCode(), pubClone.hashCode())

        assertTrue("Matching PrivateNotes must be equal", priv == privClone)
        assertEquals(priv.hashCode(), privClone.hashCode())

        // Different passwords must not be equal
        assertFalse("PrivateNotes with different passwords must not be equal", priv == privDifferentPass)
    }

    @Test
    fun testCopyWithMethod() {
        val pub = PublicNote(id = 5L, title = "Original Title", content = "Original Content")
        val pubCopy = pub.copyWith(newTitle = "Updated Title")

        assertEquals(5L, pubCopy.id)
        assertEquals("Updated Title", pubCopy.title)
        assertEquals("Original Content", pubCopy.content)
        assertFalse(pubCopy.isPrivate())
        assertNotSame(pub, pubCopy)

        val priv = PrivateNote(id = 7L, title = "Priv Title", content = "Priv Content", password = "p1")
        val privCopy = priv.copyWith(newContent = "New Body", newPassword = "p2")

        assertEquals(7L, privCopy.id)
        assertEquals("Priv Title", privCopy.title)
        assertEquals("New Body", privCopy.content)
        assertEquals("p2", privCopy.password)
        assertTrue(privCopy.isPrivate())
        assertNotSame(priv, privCopy)
    }
}
