package com.arcuscomputing.dictionary.io;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.RawRes;

import com.arcuscomputing.dictionarypro.ads.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okio.BufferedSink;
import okio.Okio;
import okio.Sink;
import okio.Source;
import timber.log.Timber;

import static com.arcuscomputing.dictionary.DictionaryConstants.BUFFER_4096;

public class DataFileManager {

    private final static String INDEX_HASH = "336996dd";
    private final static String DEFINITIONS_HASH = "cb10b5de";

    private final File DICTIONARY_DATA_DIR;
    private final File INDEX_FILE;
    private final File DATA_FILE;


    public DataFileManager(Context context) {
        DICTIONARY_DATA_DIR = context.getExternalFilesDir(null);

        if (DICTIONARY_DATA_DIR == null) {
            throw new IllegalStateException("Couldn't get a reference to external data directory.");
        }

        INDEX_FILE = new File(DICTIONARY_DATA_DIR, "index.dat");
        DATA_FILE = new File(DICTIONARY_DATA_DIR, "wdefs_all.dat");

    }

    File getIndexFile() {
        return INDEX_FILE;
    }

    File getDataFile() {
        return DATA_FILE;
    }

    boolean indexFileExists() {
        return INDEX_FILE.exists();
    }

    boolean dataFileExists() {
        return DATA_FILE.exists();
    }

    boolean extractRequiredFiles(Context context) {

        boolean indexOk = copyFile(context.getResources().openRawResource(R.raw.index), DICTIONARY_DATA_DIR, "index.dat");
        boolean defsOk = copyFile(context.getResources().openRawResource(R.raw.wdefs_all), DICTIONARY_DATA_DIR, "wdefs_all.dat");

        return indexOk && defsOk;
    }

    boolean hashesAreOk() {

        boolean hashesAreOk = false;

        String indexHash = computeCRC32(INDEX_FILE);
        String dataHash = computeCRC32(DATA_FILE);

        if (indexHash != null && dataHash != null) {
            if (indexHash.equals(INDEX_HASH) && dataHash.equals(DEFINITIONS_HASH)) {
                hashesAreOk = true;
            } else {
                Timber.e("Hashes are not as expected");
            }
        } else {
            Timber.e("Hashes are null");
        }

        return hashesAreOk;
    }

    private String computeCRC32(File file) {
        String hash = null;
        InputStream fis = null;
        CRC32 crc = new CRC32();

        try {
            fis = new FileInputStream(file);

            byte[] buffer = new byte[BUFFER_4096 * 2];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                crc.update(buffer, 0, bytesRead);
            }

            hash = Long.toHexString(crc.getValue());

        } catch (Exception e) {

            return null;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                Timber.e(e, "Unexpected errors in computeCRC32");
            }
        }

        return hash;
    }

    private boolean copyFile(InputStream inputStream, File directory, String fileName) {

        Source indexSource = Okio.buffer(Okio.source(inputStream));
        BufferedSink sink = null;
        File destination = new File(directory, fileName);

        try {
            sink = Okio.buffer(Okio.sink(destination));
            sink.writeAll(indexSource);
            sink.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (sink != null) {
            try {
                sink.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;

    }
}
