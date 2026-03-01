package com.example.biblefob.data

import kotlinx.coroutines.flow.Flow

interface VersionCatalogRepository {
    val versionEntries: Flow<List<VersionEntry>>

    suspend fun upsertUserEntry(entry: VersionEntry)

    companion object {
        val builtInEntries: List<VersionEntry> = listOf(
            VersionEntry(
                id = "KJV",
                displayName = "King James Version",
                sqliteDbAssetPath = DataSourcePaths.sqliteDbAsset("KJV"),
                sqlDumpAssetPath = DataSourcePaths.sqlDumpAsset("KJV")
            ),
            VersionEntry(
                id = "ASV",
                displayName = "American Standard Version",
                sqliteDbAssetPath = DataSourcePaths.sqliteDbAsset("ASV"),
                sqlDumpAssetPath = DataSourcePaths.sqlDumpAsset("ASV")
            ),
            VersionEntry(
                id = "WEB",
                displayName = "World English Bible",
                sqliteDbAssetPath = DataSourcePaths.sqliteDbAsset("WEB"),
                sqlDumpAssetPath = DataSourcePaths.sqlDumpAsset("WEB")
            ),
            VersionEntry(
                id = "YLT",
                displayName = "Young's Literal Translation",
                sqliteDbAssetPath = DataSourcePaths.sqliteDbAsset("YLT"),
                sqlDumpAssetPath = DataSourcePaths.sqlDumpAsset("YLT")
            )
        )

        const val defaultVersionId: String = "ASV"
    }
}
