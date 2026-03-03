package com.example.biblefob.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VersionEntryTest {

    @Test
    fun fromJsonOrNull_returnsNormalizedEntry() {
        val parsed = VersionEntry.fromJsonOrNull(
            """
            {
              "id": " web ",
              "displayName": "World English Bible",
              "sqliteDbAssetPath": "database/WEB_bible.db",
              "sqlDumpAssetPath": "database/WEB_bible.sql"
            }
            """.trimIndent()
        )

        assertNotNull(parsed)
        assertEquals("WEB", parsed?.id)
        assertEquals("World English Bible", parsed?.displayName)
        assertEquals(VersionManagementPolicy.USER_IMPORTED, parsed?.policy)
        assertEquals(VersionDataSourceType.LOCAL_FILE, parsed?.dataSourceType)
    }

    @Test
    fun fromJsonOrNull_returnsNullWhenMissingRequiredFields() {
        val parsed = VersionEntry.fromJsonOrNull(
            """
            {
              "id": "NET",
              "displayName": "",
              "sqliteDbAssetPath": "database/NET_bible.db",
              "sqlDumpAssetPath": "database/NET_bible.sql"
            }
            """.trimIndent()
        )

        assertNull(parsed)
    }
}
