package com.example.biblefob.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetSqlDumpSQLiteOpenHelperTest {
    @Test
    fun `splitSqlStatements ignores semicolons in quoted string literals`() {
        val script = """
            create table asv(id int not null, text varchar(1000) not null);
            INSERT INTO asv(id, text) VALUES
            (1, 'A sentence; with punctuation'),
            (2, 'Two apostrophes ''quoted'' and another; semicolon');
        """.trimIndent()

        val statements = splitSqlStatements(script)

        assertEquals(2, statements.size)
        assertEquals(true, statements[0].startsWith("create table asv"))
        assertEquals(true, statements[1].startsWith("INSERT INTO asv"))
    }

    @Test
    fun `splitSqlStatements returns trailing statement without terminator`() {
        val script = "create table asv(id int);\ninsert into asv(id) values (1)"

        val statements = splitSqlStatements(script)

        assertEquals(listOf("create table asv(id int)", "insert into asv(id) values (1)"), statements)
    }
}
