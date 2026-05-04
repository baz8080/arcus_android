package com.arcuscomputing

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement

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

    private val db: SQLiteDatabase
    private val insertStatement: SQLiteStatement

    init {
        db = writableDatabase
        insertStatement = db.compileStatement(INSERT_SQL)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DICTIONARY_TABLE_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favourites")
        onCreate(db)
    }

    @Synchronized
    fun insertFavourite(word: String, definition: String) {
        insertStatement.bindString(1, word)
        insertStatement.bindString(2, definition)
        insertStatement.execute()
    }

    @Synchronized
    fun isFavourite(word: String, definition: String): Boolean {
        val cursor = db.rawQuery(COUNT_QUERY, arrayOf(word, definition))
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count > 0
    }

    @Synchronized
    fun deleteFromFavourites(word: String, definition: String): Boolean {
        val rowsAffected = db.delete("favourites", "word = ? AND definition = ?", arrayOf(word, definition))
        return rowsAffected == 1
    }

    @Synchronized
    fun getAllFavourites(sortOrder: SortOrder): List<WordModel> {
        val cursor = db.rawQuery("SELECT * FROM favourites ORDER BY ${sortOrder.sql}", null)
        val results = mutableListOf<WordModel>()
        while (cursor.moveToNext()) {
            results.add(WordModel(
                word = cursor.getString(cursor.getColumnIndexOrThrow("word")),
                definition = cursor.getString(cursor.getColumnIndexOrThrow("definition"))
            ))
        }
        cursor.close()
        return results
    }

    @Synchronized
    fun deleteAllFavourites() {
        db.execSQL("DELETE FROM favourites")
    }

    companion object {
        private const val DATABASE_VERSION = 3
        private const val DICTIONARY_TABLE_CREATE =
            "CREATE TABLE favourites (word TEXT, definition TEXT, date_added DATETIME default CURRENT_TIMESTAMP)"
        private const val INSERT_SQL =
            "INSERT INTO favourites (word, definition) values (?,?)"
        private const val COUNT_QUERY =
            "SELECT COUNT(*) FROM favourites WHERE word = ? AND definition = ?"
    }
}
