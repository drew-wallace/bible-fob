package com.example.biblefob.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionDataSourceMetadataTest {
    @Test
    fun `toDataSourceMetadata_maps_entry_paths_and_type`() {
        val entry = VersionEntry(
            id = "NET",
            displayName = "NET",
            sqliteDbAssetPath = "/tmp/imported_versions/NET_bible.db",
            sqlDumpAssetPath = "/tmp/imported_versions/NET_bible.sql",
            policy = VersionManagementPolicy.USER_IMPORTED,
            dataSourceType = VersionDataSourceType.LOCAL_FILE
        )

        val metadata = entry.toDataSourceMetadata()

        assertEquals(VersionDataSourceType.LOCAL_FILE, metadata.type)
        assertEquals(entry.sqliteDbAssetPath, metadata.sqliteDbPath)
        assertEquals(entry.sqlDumpAssetPath, metadata.sqlDumpPath)
    }
}
