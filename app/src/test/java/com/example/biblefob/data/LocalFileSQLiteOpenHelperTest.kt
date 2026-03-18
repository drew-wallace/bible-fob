package com.example.biblefob.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalFileSQLiteOpenHelperTest {

    @Test
    fun `splitSqlStatements keeps semicolons inside quoted text`() {
        val script = """
            CREATE TABLE verses(text TEXT);
            INSERT INTO verses(text) VALUES ('In the beginning; God created.');
        """.trimIndent()

        val statements = splitSqlStatements(script)

        assertEquals(2, statements.size)
        assertEquals("CREATE TABLE verses(text TEXT)", statements[0])
        assertEquals(
            "INSERT INTO verses(text) VALUES ('In the beginning; God created.')",
            statements[1]
        )
    }

    @Test
    fun `splitSqlStatements handles escaped quotes and no trailing semicolon`() {
        val script = """
            INSERT INTO verses(text) VALUES ('It''s done; it is finished');
            UPDATE verses SET text='No separator here'
        """.trimIndent()

        val statements = splitSqlStatements(script)

        assertEquals(2, statements.size)
        assertEquals("INSERT INTO verses(text) VALUES ('It''s done; it is finished')", statements[0])
        assertEquals("UPDATE verses SET text='No separator here'", statements[1])
    }
}
