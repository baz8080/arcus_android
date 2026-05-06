package com.arcuscomputing.dictionary.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MappedFileTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun mappedFileOf(bytes: ByteArray): MappedFile {
        val file = tempFolder.newFile()
        file.writeBytes(bytes)
        return MappedFile(file)
    }

    private fun mappedFileOf(content: String) =
        mappedFileOf(content.toByteArray(Charsets.ISO_8859_1))

    @Test fun `readLine returns null on empty file`() {
        val mf = mappedFileOf(ByteArray(0))
        assertNull(mf.readLine())
    }

    @Test fun `readLine reads line without trailing newline`() {
        val mf = mappedFileOf("hello")
        assertEquals("hello", mf.readLine())
        assertNull(mf.readLine())
    }

    @Test fun `readLine reads line with trailing newline`() {
        val mf = mappedFileOf("hello\n")
        assertEquals("hello", mf.readLine())
        assertNull(mf.readLine())
    }

    @Test fun `readLine strips carriage return before newline`() {
        val mf = mappedFileOf("hello\r\n")
        assertEquals("hello", mf.readLine())
    }

    @Test fun `readLine returns empty string for blank line`() {
        val mf = mappedFileOf("\nhello\n")
        assertEquals("", mf.readLine())
        assertEquals("hello", mf.readLine())
    }

    @Test fun `readLine reads multiple lines sequentially`() {
        val mf = mappedFileOf("line1\nline2\nline3\n")
        assertEquals("line1", mf.readLine())
        assertEquals("line2", mf.readLine())
        assertEquals("line3", mf.readLine())
        assertNull(mf.readLine())
    }

    @Test fun `seek then readLine reads from the correct position`() {
        val mf = mappedFileOf("abcde\nfghij\n")
        mf.seek(6)
        assertEquals("fghij", mf.readLine())
    }

    @Test fun `position advances after readLine`() {
        val mf = mappedFileOf("abc\n")
        assertEquals(0L, mf.position)
        mf.readLine()
        assertEquals(4L, mf.position)
    }

    @Test fun `length returns total file size in bytes`() {
        val mf = mappedFileOf("hello\n")
        assertEquals(6L, mf.length)
    }

    @Test fun `readLine decodes ISO-8859-1 bytes correctly`() {
        // 0xE9 = 'é' in ISO-8859-1
        val bytes = byteArrayOf(0x68, 0x65, 0x6C, 0x6C, 0xE9.toByte(), 0x0A)
        val mf = mappedFileOf(bytes)
        assertEquals("hellé", mf.readLine())
    }

    @Test fun `readByte returns correct byte and advances position`() {
        val mf = mappedFileOf("AB")
        assertEquals('A'.code.toByte(), mf.readByte())
        assertEquals(1L, mf.position)
        assertEquals('B'.code.toByte(), mf.readByte())
    }

    @Test fun `seek and readByte read from the seeked position`() {
        val mf = mappedFileOf("ABCDE")
        mf.seek(3)
        assertEquals('D'.code.toByte(), mf.readByte())
    }
}
