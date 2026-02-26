package com.example.biblefob.parsing

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQueryParserTest {
    private val parser = SearchQueryParser()

    @Test
    fun `parses single passage from search param`() {
        val result = parser.parseFromUriString("https://example.com/open?search=John%203%3A16-19")

        assertEquals(listOf("John 3:16-19"), result)
    }

    @Test
    fun `parses comma separated passages`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=John%203%3A16-19,%20Rom%208%3A1"
        )

        assertEquals(listOf("John 3:16-19", "Romans 8:1"), result)
    }

    @Test
    fun `parses semicolon separated passages`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=John%203%3A16-19;%20Ps%2023"
        )

        assertEquals(listOf("John 3:16-19", "Psalms 23"), result)
    }

    @Test
    fun `parses mixed separators and trims whitespace`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=%20John%203%3A16-19%20,%20Rom%208%3A1;%20%20Ps%2023%20"
        )

        assertEquals(listOf("John 3:16-19", "Romans 8:1", "Psalms 23"), result)
    }

    @Test
    fun `supports numeric and roman-prefixed books`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=1%20Cor%2013%3A4-7;%20II%20John%201%3A5"
        )

        assertEquals(listOf("1 Corinthians 13:4-7", "2 John 1:5"), result)
    }

    @Test
    fun `supports broad abbreviation coverage`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=Gen%201%3A1,%20Rev%2022%3A21,%20Song%202%3A1"
        )

        assertEquals(listOf("Genesis 1:1", "Revelation 22:21", "Song of Songs 2:1"), result)
    }

    @Test
    fun `supports borrowed references from previous chunk`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=Jn%201%3A1-3,6,%202%3A1"
        )

        assertEquals(listOf("John 1:1-3", "John 1:6", "John 2:1"), result)
    }

    @Test
    fun `supports chapter spans`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=Jn%201-2"
        )

        assertEquals(listOf("John 1:1-2:25"), result)
    }

    @Test
    fun `supports chapter spans for single chapter books`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=Jude%201-1"
        )

        assertEquals(listOf("Jude 1:1-1:25"), result)
    }


    @Test
    fun `supports chapter spans for third john`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=3%20Jn%201-1"
        )

        assertEquals(listOf("3 John 1:1-1:15"), result)
    }

    @Test
    fun `rejects chapter spans with invalid start chapter`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=Jn%200-1"
        )

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `rejects chapter spans with invalid end chapter`() {
        val result = parser.parseFromUriString(
            "https://example.com/open?search=Jn%201-22"
        )

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `returns empty list for invalid or missing search param`() {
        assertEquals(emptyList<String>(), parser.parseFromUriString("https://example.com/open"))
        assertEquals(emptyList<String>(), parser.parseFromUriString("https://example.com/open?search="))
        assertEquals(emptyList<String>(), parser.parseFromUriString("not a uri"))
    }
}
