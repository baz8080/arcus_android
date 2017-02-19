package com.arcuscomputing.dictionary.io;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import timber.log.Timber;

import static com.arcuscomputing.dictionary.DictionaryConstants.BUFFER_4096;

public class DataFileManager {

    private static final String PACKAGE_PREFIX = "res/raw/";

    private final static String INDEX_HASH = "b23ae5ff";
    private final static String DEFINITIONS_HASH = "52dbf22";

    private final static long INDEX_SIZE = 3069343L;
    private final static long DEFINITIONS_SIZE = 17883031L;

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


    /**
     * Version 1 ========= b798e8397956c5a8800223bdcb776e67 defintions-all.txt
     * a3a2bca3fa31ccc901cab17dd19d485c index.txt
     * <p/>
     * Version 2 (Synonyms) ==================== d1bcfee43d9ea9287030bc7d44c540ab
     * wdefs_all.dat 4b4f507a8b62b86f9240aee32f83a0c1 index.dat
     * <p/>
     * Version 3 (Enhancements) ========================
     * c0be2c90bbd198bae620ada287acc317 index.dat feb22783c329d547d56f6dea029a0687
     * wdefs_all.dat
     * <p/>
     * Version 3 (as above but CRC32) ============================== index.dat
     * d45311eb 3058701 wdefs_all.dat 38fd6d17 15933702
     * <p/>
     * Version 4 (more defs, usgae etc) ================================ index.dat
     * b23ae5ff 3069343(bytes) wdefs_all.dat 052dbf22 17883031(bytes)
     * <p/>
     * 3,069,343
     */

    public File getIndexFile() {
        return INDEX_FILE;
    }

    public File getDataFile() {
        return DATA_FILE;
    }

    public boolean indexFileExists() {
        return INDEX_FILE.exists();
    }

    public boolean dataFileExists() {
        return DATA_FILE.exists();
    }

    public boolean extractRequiredFiles(String packagePath) {

        boolean wasExtracted = false;

        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(new File(packagePath)),
                    BUFFER_4096));

            ZipEntry entry;
            byte[] data = new byte[BUFFER_4096];

            final String INDEX_PACKAGE_NAME = PACKAGE_PREFIX + INDEX_FILE.getName();
            final String DATA_PACKAGE_NAME = PACKAGE_PREFIX + DATA_FILE.getName();

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.getName().equals(INDEX_PACKAGE_NAME) || entry.getName().equals(DATA_PACKAGE_NAME)) {

                    FileOutputStream fos = new FileOutputStream(DICTIONARY_DATA_DIR.getPath()
                            + entry.getName().substring(entry.getName().lastIndexOf("/")));
                    BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_4096);
                    int count;

                    while ((count = zis.read(data, 0, BUFFER_4096)) != -1) {
                        dest.write(data, 0, count);
                    }

                    dest.flush();
                    dest.close();
                }
            }

            wasExtracted = true; // TODO need to actually check here...
            zis.close();

        } catch (IOException e) {
            Timber.e(e, "Unexpected error in extractRequiredFiles");
        }

        return wasExtracted;
    }

    public boolean hashesAreOk() {

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

    public boolean quickCheck() {
        return INDEX_FILE.length() == INDEX_SIZE && DATA_FILE.length() == DEFINITIONS_SIZE;

    }

    public void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            outChannel.close();
        }
    }
}
