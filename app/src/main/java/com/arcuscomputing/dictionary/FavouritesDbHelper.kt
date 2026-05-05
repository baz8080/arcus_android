package com.arcuscomputing.dictionary

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FavouritesDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    if (context.packageName.contains("pro")) "arcusdictionarypro.db" else "arcusdictionary.db",
    null,
    DATABASE_VERSION
) {

    enum class SortOrder(internal val sql: String) {
        ALPHA_ASC("word ASC"),
        ALPHA_DESC("word DESC"),
        DATE_ASC("date_added ASC"),
        DATE_DESC("date_added DESC"),
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DICTIONARY_TABLE_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favourites")
        onCreate(db)
    }

    fun insertFavourite(word: String, definition: String) {
        writableDatabase.insert("favourites", null, ContentValues().apply {
            put("word", word)
            put("definition", definition)
        })
    }

    fun deleteFromFavourites(word: String, definition: String) {
        writableDatabase.delete("favourites", "word = ? AND definition = ?", arrayOf(word, definition))
    }

    fun deleteAllFavourites() {
        writableDatabase.execSQL("DELETE FROM favourites")
    }

    fun getAllFavourites(sortOrder: SortOrder): List<WordModel> =
        readableDatabase.rawQuery(
            "SELECT word, definition FROM favourites ORDER BY ${sortOrder.sql}",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(WordModel(word = cursor.getString(0), definition = cursor.getString(1)))
                }
            }
        }

    fun getFavouriteKeys(): Set<Pair<String, String>> =
        readableDatabase.rawQuery("SELECT word, definition FROM favourites", null).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to cursor.getString(1))
                }
            }
        }

    companion object {
        private const val DATABASE_VERSION = 3
        private const val DICTIONARY_TABLE_CREATE =
            "CREATE TABLE favourites (word TEXT, definition TEXT, date_added DATETIME default CURRENT_TIMESTAMP)"
    }
}
