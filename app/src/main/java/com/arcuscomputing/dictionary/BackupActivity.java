package com.arcuscomputing.dictionary;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.arcuscomputing.ArcusApplication;
import com.arcuscomputing.dictionary.io.DataFileManager;
import com.arcuscomputing.dictionarypro.parent.R;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;


public class BackupActivity extends AppCompatActivity {

    private final static String DATABASE_NAME = "arcusdictionary.db";
    DataFileManager dfm = ArcusApplication.getDataFileManager();
    private String databaseName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.backup);

        databaseName = getPackageName().contains("pro")
                ? "arcusdictionarypro.db"
                : "arcusdictionary.db";

        Button exportDbToSdButton = (Button) findViewById(R.id.exportdbtosdbutton);

        if (exportDbToSdButton != null) {
            exportDbToSdButton.setOnClickListener(new OnClickListener() {
                public void onClick(final View v) {

                    new AlertDialog.Builder(BackupActivity.this).setMessage(
                            "Are you sure (this will overwrite any existing backup data)?").setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    if (isExternalStorageAvail()) {
                                        new ExportDatabaseTask().execute();
                                        BackupActivity.this.finish();
                                    } else {
                                        Toast.makeText(BackupActivity.this,
                                                "External storage is not available, unable to export data.", Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                        }
                    }).show();
                }
            });
        }

        Button importDbFromSdButton = (Button) findViewById(R.id.importdbfromsdbutton);
        if (importDbFromSdButton != null) {
            importDbFromSdButton.setOnClickListener(new OnClickListener() {
                public void onClick(final View v) {
                    new AlertDialog.Builder(BackupActivity.this).setMessage(
                            "Are you sure (this will overwrite existing current data)?").setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    if (isExternalStorageAvail()) {
                                        new ImportDatabaseTask().execute();
                                        // sleep momentarily so that database reset stuff has time to take place (else Main reloads too fast)

                                        SystemClock.sleep(500);
                                        BackupActivity.this.finish();
                                    } else {
                                        Toast.makeText(BackupActivity.this,
                                                "External storage is not available, unable to export data.", Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                        }
                    }).show();
                }
            });
        }
    }

    private boolean isExternalStorageAvail() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private class ExportDatabaseTask extends AsyncTask<Void, Void, Boolean> {
        private final ProgressDialog dialog = new ProgressDialog(BackupActivity.this);

        // can use UI thread here
        @Override
        protected void onPreExecute() {
            dialog.setMessage("Exporting database...");
            dialog.show();
        }

        // automatically done on worker thread (separate from UI thread)
        @Override
        protected Boolean doInBackground(final Void... args) {



            File dbFile = new File(Environment.getDataDirectory() + "/data/" + getPackageName() + "/databases/" + databaseName);

            File exportDir = new File(Environment.getExternalStorageDirectory(), "arcusbackup");
            if (!exportDir.exists()) {
                boolean mkdirs = exportDir.mkdirs();

                if (!mkdirs) {
                    return false;
                }
            }

            File file = new File(exportDir, DATABASE_NAME);

            try {
                boolean fileCreated = file.createNewFile();

                if (fileCreated) {
                    dfm.copyFile(dbFile, file);
                    return true;
                } else {
                    return false;
                }

            } catch (IOException e) {
                return false;
            }
        }

        // can use UI thread here
        @Override
        protected void onPostExecute(final Boolean success) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (success) {
                Toast.makeText(BackupActivity.this, "Export successful!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(BackupActivity.this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ImportDatabaseTask extends AsyncTask<Void, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(BackupActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Importing database...");
            dialog.show();
        }

        // could pass the params used here in AsyncTask<String, Void, String> - but not being re-used
        @Override
        protected String doInBackground(final Void... args) {

            File dbBackupFile = new File(Environment.getExternalStorageDirectory() + "/arcusbackup/" + DATABASE_NAME);
            if (!dbBackupFile.exists()) {
                return "Database backup file does not exist, cannot import.";
            } else if (!dbBackupFile.canRead()) {
                return "Database backup file exists, but is not readable, cannot import.";
            }

            File dbFile = new File(Environment.getDataDirectory() + "/data/" + getPackageName() + "/databases/" + databaseName);
            if (dbFile.exists()) {
                boolean delete = dbFile.delete();

                if (!delete) {
                    Timber.e("Failed to delete file");
                }
            }

            try {
                boolean create = dbFile.createNewFile();

                if (!create) {
                    Timber.e("Failed to create file");
                    return null;
                }

                dfm.copyFile(dbBackupFile, dbFile);

                Intent resetDatabase = new Intent();
                resetDatabase.putExtra("reset", true);
                setResult(RESULT_OK, resetDatabase);

                return null;
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(final String errMsg) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (errMsg == null) {
                Toast.makeText(BackupActivity.this, "Import successful!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(BackupActivity.this, "Import failed - " + errMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }


}
