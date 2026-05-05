package com.arcuscomputing.dictionary.io

import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Read-only memory-mapped wrapper used for binary search over the dictionary
 * files. Pages are paged in on demand by the OS and reclaimed under memory
 * pressure, so RSS is bounded by the working set actually touched (a handful
 * of pages per query) rather than the file size.
 */
class MappedFile(file: File) {

    private val buffer: MappedByteBuffer = RandomAccessFile(file, "r").use { raf ->
        raf.channel.map(FileChannel.MapMode.READ_ONLY, 0L, raf.length())
    }

    val length: Long get() = buffer.limit().toLong()
    val position: Long get() = buffer.position().toLong()

    fun seek(pos: Long) {
        buffer.position(pos.toInt())
    }

    fun readByte(): Byte = buffer.get()

    /**
     * Reads bytes up to (and consumes) the next `\n`, or to EOF. Strips a
     * trailing `\r`. Decodes as ISO-8859-1 (one byte = one char). Returns
     * `null` at EOF.
     */
    fun readLine(): String? {
        if (!buffer.hasRemaining()) return null
        val start = buffer.position()
        val limit = buffer.limit()
        var end = start
        while (end < limit && buffer.get(end) != NEWLINE) end++
        val terminated = end < limit
        val strEnd = if (end > start && buffer.get(end - 1) == CARRIAGE_RETURN) end - 1 else end
        val bytes = ByteArray(strEnd - start)
        buffer.position(start)
        buffer.get(bytes)
        buffer.position(if (terminated) end + 1 else end)
        return String(bytes, Charsets.ISO_8859_1)
    }

    companion object {
        private const val NEWLINE: Byte = '\n'.code.toByte()
        private const val CARRIAGE_RETURN: Byte = '\r'.code.toByte()
    }
}
