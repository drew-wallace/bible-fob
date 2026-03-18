package com.example.biblefob.navigation

import com.example.biblefob.data.VersionDataSourceType
import com.example.biblefob.data.VersionEntry
import com.example.biblefob.data.VersionManagementPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BibleFobAppVersionNormalizationTest {

    @Test
    fun `normalizeVersionOrNull matches url-safe ids`() {
        val supportedVersions = listOf(
            importedEntry(id = "NET_BIBLE", displayName = "NET Bible"),
            importedEntry(id = "LSB_95", displayName = "Legacy Standard Bible 95")
        )

        assertEquals("NET_BIBLE", normalizeVersionOrNull("net-bible", supportedVersions))
        assertEquals("LSB_95", normalizeVersionOrNull("lsb95", supportedVersions))
    }

    @Test
    fun `normalizeVersionOrNull still matches canonical ids`() {
        val supportedVersions = listOf(importedEntry(id = "NKJV", displayName = "New King James Version"))

        assertEquals("NKJV", normalizeVersionOrNull("nkjv", supportedVersions))
        assertEquals("NKJV", normalizeVersionOrNull("N_K_J_V", supportedVersions))
    }

    @Test
    fun `normalizeVersionOrNull returns null when no match exists`() {
        val supportedVersions = listOf(importedEntry(id = "ESV", displayName = "English Standard Version"))

        assertNull(normalizeVersionOrNull("niv", supportedVersions))
        assertNull(normalizeVersionOrNull("", supportedVersions))
    }

    private fun importedEntry(id: String, displayName: String): VersionEntry {
        return VersionEntry(
            id = id,
            displayName = displayName,
            sqliteDbAssetPath = "/tmp/${id}_bible.db",
            sqlDumpAssetPath = "/tmp/${id}_bible.sql",
            policy = VersionManagementPolicy.USER_IMPORTED,
            dataSourceType = VersionDataSourceType.LOCAL_FILE
        )
    }
}
