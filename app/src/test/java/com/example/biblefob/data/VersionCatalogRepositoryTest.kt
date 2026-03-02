package com.example.biblefob.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCatalogRepositoryTest {

    @Test
    fun builtInEntries_containsDefaultVersion() {
        val hasDefault = VersionCatalogRepository.builtInEntries
            .any { it.id == VersionCatalogRepository.defaultVersionId }

        assertTrue(hasDefault)
    }

    @Test
    fun mergeVersionEntries_userEntryOverridesBuiltInForSameId() {
        val builtIn = listOf(
            VersionEntry(
                id = "ASV",
                displayName = "American Standard Version",
                sqliteDbAssetPath = "database/ASV_bible.db",
                sqlDumpAssetPath = "database/ASV_bible.sql",
                policy = VersionManagementPolicy.BUNDLED
            )
        )
        val userEntries = listOf(
            VersionEntry(
                id = "ASV",
                displayName = "ASV (Custom)",
                sqliteDbAssetPath = "database/ASV_custom.db",
                sqlDumpAssetPath = "database/ASV_custom.sql",
                policy = VersionManagementPolicy.USER_IMPORTED
            )
        )

        val merged = mergeVersionEntries(
            builtInEntries = builtIn,
            userEntries = userEntries
        )

        assertEquals(1, merged.size)
        assertEquals("ASV (Custom)", merged.first().displayName)
        assertEquals("database/ASV_custom.db", merged.first().sqliteDbAssetPath)
        assertEquals("database/ASV_custom.sql", merged.first().sqlDumpAssetPath)
    }
}
