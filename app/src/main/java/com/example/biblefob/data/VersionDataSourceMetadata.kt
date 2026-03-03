package com.example.biblefob.data

data class VersionDataSourceMetadata(
    val type: VersionDataSourceType,
    val sqliteDbPath: String,
    val sqlDumpPath: String
)

fun VersionEntry.toDataSourceMetadata(): VersionDataSourceMetadata {
    return VersionDataSourceMetadata(
        type = dataSourceType,
        sqliteDbPath = sqliteDbAssetPath,
        sqlDumpPath = sqlDumpAssetPath
    )
}
