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
