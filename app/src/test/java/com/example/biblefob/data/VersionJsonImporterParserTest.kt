package com.example.biblefob.data

import com.example.biblefob.data.importer.ImportException
import com.example.biblefob.data.importer.parseBibleJsonToVerseRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionJsonImporterParserTest {

    @Test
    fun `accepts Psalm alias and normalizes to Psalms`() {
        val rawJson = """
            {
              "Psalm": {
                "1": {
                  "1": "Blessed is the man"
                }
              }
            }
        """.trimIndent()

        val rows = parseBibleJsonToVerseRows(rawJson)

        assertEquals(1, rows.size)
        assertEquals("Psalms", rows.first().book)
    }


    @Test
    fun `accepts blank verse text`() {
        val rawJson = """
            {
              "Matthew": {
                "17": {
                  "21": "   "
                }
              }
            }
        """.trimIndent()

        val rows = parseBibleJsonToVerseRows(rawJson)

        assertEquals(1, rows.size)
        assertEquals("", rows.first().text)
    }

    @Test
    fun `rejects non string verse text`() {
        val rawJson = """
            {
              "John": {
                "1": {
                  "1": 42
                }
              }
            }
        """.trimIndent()

        val error = runCatching { parseBibleJsonToVerseRows(rawJson) }.exceptionOrNull()

        assertTrue(error is ImportException)
        assertTrue(error?.message?.contains("must contain string verse text") == true)
    }

    @Test
    fun `rejects malformed root structure`() {
        val rawJson = """
            [
              {"Genesis": {"1": {"1": "Text"}}}
            ]
        """.trimIndent()

        val error = runCatching { parseBibleJsonToVerseRows(rawJson) }.exceptionOrNull()

        assertTrue(error is ImportException)
        assertTrue(error?.message?.contains("Malformed JSON") == true)
    }
}
