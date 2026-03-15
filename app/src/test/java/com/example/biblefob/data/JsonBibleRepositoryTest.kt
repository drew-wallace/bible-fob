package com.example.biblefob.data

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonBibleRepositoryTest {
    @Test
    fun `expands single chapter verse range`() {
        val repository = JsonBibleRepository(
            wholeBibleJson = """
            {
              "John": {
                "3": {
                  "16": "For God so loved the world",
                  "17": "For God did not send his Son",
                  "18": "Whoever believes in him",
                  "19": "This is the verdict"
                }
              }
            }
            """.trimIndent()
        )

        val verses = repository.getVerses("John 3:16-19")

        assertEquals(listOf(16, 17, 18, 19), verses.map { it.number })
    }

    @Test
    fun `expands cross chapter range in order`() {
        val repository = JsonBibleRepository(
            wholeBibleJson = """
            {
              "John": {
                "1": {
                  "50": "You shall see greater things",
                  "51": "You shall see heaven open"
                },
                "2": {
                  "1": "On the third day",
                  "2": "and Jesus and his disciples"
                }
              }
            }
            """.trimIndent()
        )

        val verses = repository.getVerses("John 1:50-2:2")

        assertEquals(
            listOf("1:50", "1:51", "2:1", "2:2"),
            verses.map { "${it.chapter}:${it.number}" }
        )
    }

    @Test
    fun `expands chapter-only reference from available chapter data`() {
        val repository = JsonBibleRepository(
            wholeBibleJson = """
            {
              "Romans": {
                "8": {
                  "2": "because through Christ Jesus",
                  "1": "Therefore, there is now no condemnation"
                }
              }
            }
            """.trimIndent()
        )

        val verses = repository.getVerses("Romans 8")

        assertEquals(listOf("8:1", "8:2"), verses.map { "${it.chapter}:${it.number}" })
    }

    @Test
    fun `rejects descending ranges`() {
        assertEquals(null, NormalizedReferenceParser.parse("John 3:19-16"))
        assertEquals(null, NormalizedReferenceParser.parse("John 3:5-2:10"))
    }



    @Test
    fun `falls back to json when sql data source throws`() {
        val repository = JsonBibleRepository(
            wholeBibleJson = """
            {
              "John": {
                "3": {
                  "16": "For God so loved the world"
                }
              }
            }
            """.trimIndent(),
            sqlDataSource = object : SqlVerseDataSource {
                override fun getVerses(range: PassageRange): List<Verse> {
                    throw IllegalStateException("database unavailable")
                }
            }
        )

        val verses = repository.getVerses("John 3:16")

        assertEquals(1, verses.size)
        assertEquals("For God so loved the world", verses.first().text)
    }

    @Test
    fun `falls back to per book loader when whole bible json is absent`() {
        val repository = JsonBibleRepository(
            perBookJsonLoader = { book ->
                if (book.name == "John") {
                    """
                    {
                      "3": {
                        "16": "For God so loved the world"
                      }
                    }
                    """.trimIndent()
                } else {
                    null
                }
            }
        )

        val verses = repository.getVerses("John 3:16")

        assertEquals(1, verses.size)
        assertEquals("For God so loved the world", verses.first().text)
    }
}
