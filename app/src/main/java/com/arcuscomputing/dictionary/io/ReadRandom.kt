package com.arcuscomputing.dictionary.io

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

private const val BUFFER_SIZE = 4096

class ReadRandom(file: File, mode: String) : RandomAccessFile(file, mode) {

    private var bufEnd = 0
    private var bufPos = 0
    private var realPos = 0L
    private val buffer = ByteArray(BUFFER_SIZE)

    init {
        invalidate()
    }

    override fun read(): Int {
        if (bufPos >= bufEnd) {
            if (fillBuffer() < 0) return -1
        }
        return if (bufEnd == 0) -1 else buffer[bufPos++].toInt() and 0xFF
    }

    private fun fillBuffer(): Int {
        val n = super.read(buffer, 0, BUFFER_SIZE)
        if (n >= 0) {
            realPos += n
            bufEnd = n
            bufPos = 0
        }
        return n
    }

    private fun invalidate() {
        bufEnd = 0
        bufPos = 0
        realPos = super.getFilePointer()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val leftover = bufEnd - bufPos
        if (len <= leftover) {
            System.arraycopy(buffer, bufPos, b, off, len)
            bufPos += len
            return len
        }
        for (i in 0 until len) {
            val c = read()
            if (c != -1) b[off + i] = c.toByte() else return i
        }
        return len
    }

    override fun getFilePointer(): Long = realPos - bufEnd + bufPos

    override fun seek(pos: Long) {
        val n = (realPos - pos).toInt()
        if (n in 0..bufEnd) {
            bufPos = bufEnd - n
        } else {
            super.seek(pos)
            invalidate()
        }
    }

    fun getNextLine(): String? {
        if (bufEnd - bufPos <= 0) {
            if (fillBuffer() < 0) throw IOException("error in filling buffer!")
        }
        var lineEnd = -1
        for (i in bufPos until bufEnd) {
            if (buffer[i] == '\n'.code.toByte()) {
                lineEnd = i
                break
            }
        }
        if (lineEnd < 0) {
            val input = StringBuilder(256)
            var c: Int
            while (read().also { c = it } != -1 && c != '\n'.code) {
                input.append(c.toChar())
            }
            return if (c == -1 && input.isEmpty()) null else input.toString()
        }
        val str = if (lineEnd > 0 && buffer[lineEnd - 1] == '\r'.code.toByte()) {
            String(buffer, bufPos, lineEnd - bufPos - 1, Charsets.ISO_8859_1)
        } else {
            String(buffer, bufPos, lineEnd - bufPos, Charsets.ISO_8859_1)
        }
        bufPos = lineEnd + 1
        return str
    }
}
