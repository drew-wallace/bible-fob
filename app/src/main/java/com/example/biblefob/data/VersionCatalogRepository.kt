package com.example.biblefob.data

import kotlinx.coroutines.flow.Flow

interface VersionCatalogRepository {
    val versionEntries: Flow<List<VersionEntry>>

    suspend fun upsertUserEntry(entry: VersionEntry)

    suspend fun renameUserEntry(id: String, displayName: String): Boolean

    suspend fun deleteUserEntry(id: String): Boolean

    companion object {
        val builtInEntries: List<VersionEntry> = listOf(
            VersionEntry(
                id = "KJV",
                displayName = "King James Version",
                sqliteDbAssetPath = DataSourcePaths.Bundled.sqliteDbAsset("KJV"),
                sqlDumpAssetPath = DataSourcePaths.Bundled.sqlDumpAsset("KJV"),
                policy = VersionManagementPolicy.BUNDLED,
                dataSourceType = VersionDataSourceType.BUNDLED_ASSET
            ),
            VersionEntry(
                id = "ASV",
                displayName = "American Standard Version",
                sqliteDbAssetPath = DataSourcePaths.Bundled.sqliteDbAsset("ASV"),
                sqlDumpAssetPath = DataSourcePaths.Bundled.sqlDumpAsset("ASV"),
                policy = VersionManagementPolicy.BUNDLED,
                dataSourceType = VersionDataSourceType.BUNDLED_ASSET
            ),
            VersionEntry(
                id = "WEB",
                displayName = "World English Bible",
                sqliteDbAssetPath = DataSourcePaths.Bundled.sqliteDbAsset("WEB"),
                sqlDumpAssetPath = DataSourcePaths.Bundled.sqlDumpAsset("WEB"),
                policy = VersionManagementPolicy.BUNDLED,
                dataSourceType = VersionDataSourceType.BUNDLED_ASSET
            ),
            VersionEntry(
                id = "YLT",
                displayName = "Young's Literal Translation",
                sqliteDbAssetPath = DataSourcePaths.Bundled.sqliteDbAsset("YLT"),
                sqlDumpAssetPath = DataSourcePaths.Bundled.sqlDumpAsset("YLT"),
                policy = VersionManagementPolicy.BUNDLED,
                dataSourceType = VersionDataSourceType.BUNDLED_ASSET
            )
        )

        const val defaultVersionId: String = "ASV"
    }
}
