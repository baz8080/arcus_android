package com.arcuscomputing.dictionary.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static com.arcuscomputing.dictionary.DictionaryConstants.BUFFER_4096;

public class ReadRandom extends RandomAccessFile {

    int buf_end = 0;
    int buf_pos = 0;
    long real_pos = 0;
    private byte[] buffer;

    public ReadRandom(File file, String mode) throws IOException {
        super(file, mode);
        invalidate();
        buffer = new byte[BUFFER_4096];
    }

    public final int read() throws IOException {
        if (buf_pos >= buf_end) {
            if (fillBuffer() < 0) {
                return -1;
            }
        }
        if (buf_end == 0) {
            return -1;
        } else {
            return buffer[buf_pos++];
        }
    }

    private int fillBuffer() throws IOException {
        int n = super.read(buffer, 0, BUFFER_4096);
        if (n >= 0) {
            real_pos += n;
            buf_end = n;
            buf_pos = 0;
        }
        return n;
    }

    private void invalidate() throws IOException {
        buf_end = 0;
        buf_pos = 0;
        real_pos = super.getFilePointer();
    }

    public int read(byte b[], int off, int len) throws IOException {
        int leftover = buf_end - buf_pos;
        if (len <= leftover) {
            System.arraycopy(buffer, buf_pos, b, off, len);
            buf_pos += len;
            return len;
        }
        for (int i = 0; i < len; i++) {
            int c = this.read();
            if (c != -1) {
                b[off + i] = (byte) c;
            } else {
                return i;
            }
        }
        return len;
    }

    public long getFilePointer() throws IOException {
        long l = real_pos;
        return (l - buf_end + buf_pos);
    }

    public void seek(long pos) throws IOException {
        int n = (int) (real_pos - pos);
        if (n >= 0 && n <= buf_end) {
            buf_pos = buf_end - n;
        } else {
            super.seek(pos);
            invalidate();
        }
    }

    /**
     * return a next line in String
     */
    @SuppressWarnings("deprecation")
    public final String getNextLine() throws IOException {
        String str;
        if (buf_end - buf_pos <= 0) {
            if (fillBuffer() < 0) {
                throw new IOException("error in filling buffer!");
            }
        }
        int lineEnd = -1;
        for (int i = buf_pos; i < buf_end; i++) {
            if (buffer[i] == '\n') {
                lineEnd = i;
                break;
            }
        }
        if (lineEnd < 0) {
            StringBuilder input = new StringBuilder(256);
            int c;
            while (((c = read()) != -1) && (c != '\n')) {
                input.append((char) c);
            }
            if ((c == -1) && (input.length() == 0)) {
                return null;
            }
            return input.toString();
        }
        if (lineEnd > 0 && buffer[lineEnd - 1] == '\r') {
            str = new String(buffer, 0, buf_pos, lineEnd - buf_pos - 1);
        } else {
            str = new String(buffer, 0, buf_pos, lineEnd - buf_pos);
        }
        buf_pos = lineEnd + 1;
        return str;
    }

}