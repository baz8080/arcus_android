package com.arcuscomputing.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.arcuscomputing.models.WordModel;

import java.util.LinkedList;
import java.util.List;

public class FavouritesDbHelper extends SQLiteOpenHelper {

    public final static String OPTION_SORT_ALPHA_ASC = "word  ASC";
    public final static String OPTION_SORT_ALPHA_DESC = "word DESC";
    public final static String OPTION_SORT_DATE_ASC = "date_added ASC";
    public final static String OPTION_SORT_DATE_DESC = "date_added DESC";
    private static final int DATABASE_VERSION = 3;
    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE favourites (word TEXT, definition TEXT, date_added DATETIME default CURRENT_TIMESTAMP)";

    private static final String INSERT_SQL =
            "INSERT INTO favourites (word, definition) values (?,?)";

    private static final String FAVOURITE_QUERY =
            "SELECT * FROM favourites WHERE word = ? AND definition = ?";

    //TODO paramaterise?
    private static final String ALL_FAVOURITES_QUERY =
            "SELECT * FROM favourites ORDER BY ";

    private static final String DELETE_FAVOURITE_QUERY =
            "DELETE FROM favourites WHERE word = ? AND definition = ?";

    private static final String DELETE_ALL_FAVOURITES_QUERY = "DELETE FROM favourites";

    private SQLiteDatabase db;

    private SQLiteStatement insertStatement;

    public FavouritesDbHelper(Context context) {
        super(
                context,
                context.getPackageName().contains("pro")
                        ? "arcusdictionarypro.db"
                        : "arcusdictionary.db",
                null,
                DATABASE_VERSION);

        synchronized (this) {
            this.db = this.getWritableDatabase();
        }

        this.insertStatement = this.db.compileStatement(INSERT_SQL);
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS favourites");
        onCreate(db);
    }

    public synchronized void insertfavourite(String word, String definition) {

        this.insertStatement.bindString(1, word);
        this.insertStatement.bindString(2, definition);
        this.insertStatement.execute();
    }

    public synchronized boolean isFavourite(String word, String definition) {

        Cursor result = db.rawQuery(FAVOURITE_QUERY, getArgsFromWordAndDef(word, definition));

        int count = result.getCount();
        result.close();

        return count == 1;
    }

    private String[] getArgsFromWordAndDef(String word, String definition) {

        String[] args = new String[2];
        args[0] = word;
        args[1] = definition;

        return args;
    }

    public synchronized boolean deleteFromFavourites(String word, String definition) {
        Cursor cursor = db.rawQuery(DELETE_FAVOURITE_QUERY, getArgsFromWordAndDef(word, definition));
        int count = cursor.getCount();
        cursor.close();


        return count == 1;
    }

    public synchronized List<WordModel> getAllFavourites(String sortMethod) {
        Cursor cursor = db.rawQuery(ALL_FAVOURITES_QUERY + sortMethod, null);
        List<WordModel> results = new LinkedList<WordModel>();

        while (cursor.moveToNext()) {
            WordModel wm = new WordModel();
            wm.setWord(cursor.getString(cursor.getColumnIndex("word")));
            String def = cursor.getString(cursor.getColumnIndex("definition"));
            wm.setDefinition(def);
            results.add(wm);
        }

        cursor.close();
        return results;
    }

    public synchronized void deleteAllFavourites() {
        Cursor cursor = db.rawQuery(DELETE_ALL_FAVOURITES_QUERY, null);
        int count = cursor.getCount();
        cursor.close();
    }

}	
