package com.arcuscomputing.dictionary

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavouritesDbHelperTest {

    private lateinit var helper: FavouritesDbHelper

    @Before
    fun setUp() {
        helper = FavouritesDbHelper(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        helper.close()
    }

    @Test fun `insertFavourite and getFavouriteKeys round-trip`() {
        helper.insertFavourite("apple", "a round fruit")
        assertTrue(helper.getFavouriteKeys().contains("apple" to "a round fruit"))
    }

    @Test fun `getFavouriteKeys returns empty set for empty database`() {
        assertTrue(helper.getFavouriteKeys().isEmpty())
    }

    @Test fun `getFavouriteKeys returns correct pairs for multiple entries`() {
        helper.insertFavourite("cat", "a feline")
        helper.insertFavourite("dog", "a canine")
        assertEquals(
            setOf("cat" to "a feline", "dog" to "a canine"),
            helper.getFavouriteKeys()
        )
    }

    @Test fun `deleteFromFavourites removes only the matched entry`() {
        helper.insertFavourite("apple", "a round fruit")
        helper.insertFavourite("banana", "a yellow fruit")
        helper.deleteFromFavourites("apple", "a round fruit")
        val keys = helper.getFavouriteKeys()
        assertFalse(keys.contains("apple" to "a round fruit"))
        assertTrue(keys.contains("banana" to "a yellow fruit"))
    }

    @Test fun `deleteFromFavourites does nothing when entry does not exist`() {
        helper.insertFavourite("apple", "a round fruit")
        helper.deleteFromFavourites("apple", "wrong definition")
        assertTrue(helper.getFavouriteKeys().contains("apple" to "a round fruit"))
    }

    @Test fun `deleteAllFavourites empties the database`() {
        helper.insertFavourite("apple", "a round fruit")
        helper.insertFavourite("banana", "a yellow fruit")
        helper.deleteAllFavourites()
        assertTrue(helper.getFavouriteKeys().isEmpty())
    }

    @Test fun `getAllFavourites returns empty list for empty database`() {
        assertTrue(helper.getAllFavourites(FavouritesDbHelper.SortOrder.ALPHA_ASC).isEmpty())
    }

    @Test fun `getAllFavourites ALPHA_ASC returns alphabetically ascending order`() {
        helper.insertFavourite("zebra", "striped animal")
        helper.insertFavourite("apple", "red fruit")
        helper.insertFavourite("mango", "tropical fruit")
        val words = helper.getAllFavourites(FavouritesDbHelper.SortOrder.ALPHA_ASC).map { it.word }
        assertEquals(listOf("apple", "mango", "zebra"), words)
    }

    @Test fun `getAllFavourites ALPHA_DESC returns alphabetically descending order`() {
        helper.insertFavourite("zebra", "striped animal")
        helper.insertFavourite("apple", "red fruit")
        helper.insertFavourite("mango", "tropical fruit")
        val words = helper.getAllFavourites(FavouritesDbHelper.SortOrder.ALPHA_DESC).map { it.word }
        assertEquals(listOf("zebra", "mango", "apple"), words)
    }

    @Test fun `getAllFavourites DATE_ASC returns earliest entry first`() {
        helper.writableDatabase.execSQL(
            "INSERT INTO favourites (word, definition, date_added) VALUES (?, ?, ?)",
            arrayOf("second", "def2", "2024-01-02 10:00:00")
        )
        helper.writableDatabase.execSQL(
            "INSERT INTO favourites (word, definition, date_added) VALUES (?, ?, ?)",
            arrayOf("first", "def1", "2024-01-01 10:00:00")
        )
        val words = helper.getAllFavourites(FavouritesDbHelper.SortOrder.DATE_ASC).map { it.word }
        assertEquals(listOf("first", "second"), words)
    }

    @Test fun `getAllFavourites DATE_DESC returns most recently added first`() {
        helper.writableDatabase.execSQL(
            "INSERT INTO favourites (word, definition, date_added) VALUES (?, ?, ?)",
            arrayOf("first", "def1", "2024-01-01 10:00:00")
        )
        helper.writableDatabase.execSQL(
            "INSERT INTO favourites (word, definition, date_added) VALUES (?, ?, ?)",
            arrayOf("second", "def2", "2024-01-02 10:00:00")
        )
        val words = helper.getAllFavourites(FavouritesDbHelper.SortOrder.DATE_DESC).map { it.word }
        assertEquals(listOf("second", "first"), words)
    }

    @Test fun `getAllFavourites maps word and definition fields correctly`() {
        helper.insertFavourite("tiger", "a large striped cat")
        val result = helper.getAllFavourites(FavouritesDbHelper.SortOrder.ALPHA_ASC).single()
        assertEquals("tiger", result.word)
        assertEquals("a large striped cat", result.definition)
    }
}
