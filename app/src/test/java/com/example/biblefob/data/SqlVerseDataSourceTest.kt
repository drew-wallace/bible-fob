package com.example.biblefob.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SqlVerseDataSourceTest {
    private val matthew = Book(id = 40, name = "Matthew")

    @Test
    fun `buildRangeSelection constrains single chapter range to verse bounds`() {
        val range = PassageRange(
            startBook = matthew,
            startChapter = 1,
            startVerse = 1,
            endBook = matthew,
            endChapter = 1,
            endVerse = 7
        )

        val (selection, args) = buildRangeSelection(range)

        assertEquals("book_id = ? AND chapter = ? AND verse BETWEEN ? AND ?", selection)
        assertArrayEquals(arrayOf("40", "1", "1", "7"), args)
    }

    @Test
    fun `buildRangeSelection spans multiple chapters correctly`() {
        val range = PassageRange(
            startBook = matthew,
            startChapter = 1,
            startVerse = 20,
            endBook = matthew,
            endChapter = 2,
            endVerse = 5
        )

        val (selection, args) = buildRangeSelection(range)

        assertEquals(
            "book_id = ? AND ((chapter = ? AND verse >= ?) OR (chapter > ? AND chapter < ?) OR (chapter = ? AND verse <= ?))",
            selection
        )
        assertArrayEquals(arrayOf("40", "1", "20", "1", "2", "2", "5"), args)
    }
}
