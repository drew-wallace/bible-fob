package com.example.biblefob.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DataSourcePathsTest {
    @Test
    fun `builds versioned paths for NET`() {
        assertEquals("bible/NET_bible.json", DataSourcePaths.wholeBibleJson("NET"))
        assertEquals("bible/NET_books", DataSourcePaths.perBookJsonDir("NET"))
        assertEquals("database/NET_bible.db", DataSourcePaths.sqliteDbAsset("NET"))
        assertEquals("database/NET_bible.sql", DataSourcePaths.sqlDumpAsset("NET"))
    }

    @Test
    fun `resolves canonical and slug book filenames`() {
        assertEquals("1 Peter", resolveBookFileName("1 Peter", BookFileNameStyle.CANONICAL))
        assertEquals("1_peter", resolveBookFileName("1 Peter", BookFileNameStyle.SLUG))
    }
}
