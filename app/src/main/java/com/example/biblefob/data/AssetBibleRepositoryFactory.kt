package com.example.biblefob.data

import android.content.Context
import java.io.File

object AssetBibleRepositoryFactory {
    fun create(
        context: Context,
        wholeBibleAssetPath: String? = DataSourcePaths.WHOLE_BIBLE_JSON,
        perBookAssetFolder: String = DataSourcePaths.PER_BOOK_JSON_DIR,
        bookFileNameStyle: BookFileNameStyle = BookFileNameStyle.SLUG,
        sqlDataSource: SqlVerseDataSource? = null
    ): BibleRepository {
        val wholeBibleJson = wholeBibleAssetPath
            ?.let { path -> runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull() }

        val perBookLoader: (Book) -> String? = { book ->
            val filename = resolveBookFileName(book.name, bookFileNameStyle)
            val path = "$perBookAssetFolder/$filename.json"
            runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull()
        }

        return JsonBibleRepository(
            wholeBibleJson = wholeBibleJson,
            perBookJsonLoader = perBookLoader,
            sqlDataSource = sqlDataSource
        )
    }

    fun createForVersion(
        context: Context,
        version: String,
        preferWholeBibleFile: Boolean = true,
        bookFileNameStyle: BookFileNameStyle = BookFileNameStyle.CANONICAL
    ): BibleRepository {
        val wholeBiblePath = if (preferWholeBibleFile) DataSourcePaths.wholeBibleJson(version) else null
        return create(
            context = context,
            wholeBibleAssetPath = wholeBiblePath,
            perBookAssetFolder = DataSourcePaths.perBookJsonDir(version),
            bookFileNameStyle = bookFileNameStyle
        )
    }

    fun createForVersionEntry(
        context: Context,
        entry: VersionEntry,
        schemaVersion: Int = 1,
        fallbackBookFileNameStyle: BookFileNameStyle = BookFileNameStyle.CANONICAL
    ): BibleRepository {
        val helper = createSqliteHelperOrNull(
            context = context,
            entry = entry,
            schemaVersion = schemaVersion
        )

        val sqlDataSource = helper?.let { openHelper ->
            SQLiteVerseDataSource(
                helper = openHelper,
                tableNames = listOf(
                    "verses",
                    entry.id.lowercase()
                )
            )
        }

        return create(
            context = context,
            wholeBibleAssetPath = null,
            perBookAssetFolder = DataSourcePaths.perBookJsonDir(entry.id),
            bookFileNameStyle = fallbackBookFileNameStyle,
            sqlDataSource = sqlDataSource
        )
    }

    private fun createSqliteHelperOrNull(
        context: Context,
        entry: VersionEntry,
        schemaVersion: Int
    ): android.database.sqlite.SQLiteOpenHelper? {
        val metadata = entry.toDataSourceMetadata()

        return when (metadata.type) {
            VersionDataSourceType.BUNDLED_ASSET -> createBundledAssetHelperOrNull(
                context = context,
                metadata = metadata,
                schemaVersion = schemaVersion
            )

            VersionDataSourceType.LOCAL_FILE -> LocalFileDatabaseFactory.createOrNull(
                context = context,
                metadata = metadata,
                schemaVersion = schemaVersion
            )
        }
    }

    private fun createBundledAssetHelperOrNull(
        context: Context,
        metadata: VersionDataSourceMetadata,
        schemaVersion: Int
    ): android.database.sqlite.SQLiteOpenHelper? {
        val databaseName = File(metadata.sqliteDbPath).name

        if (assetExists(context, metadata.sqliteDbPath)) {
            return AssetBackedSQLiteOpenHelper(
                context = context,
                assetPath = metadata.sqliteDbPath,
                databaseName = databaseName,
                version = schemaVersion
            )
        }

        if (assetExists(context, metadata.sqlDumpPath)) {
            return AssetSqlDumpSQLiteOpenHelper(
                context = context,
                sqlDumpAssetPath = metadata.sqlDumpPath,
                databaseName = databaseName,
                version = schemaVersion
            )
        }

        return null
    }

    private fun assetExists(context: Context, path: String): Boolean {
        return runCatching {
            context.assets.open(path).use { }
            true
        }.getOrDefault(false)
    }

}

enum class BookFileNameStyle {
    CANONICAL,
    SLUG
}

internal fun resolveBookFileName(bookName: String, style: BookFileNameStyle): String {
    return when (style) {
        BookFileNameStyle.CANONICAL -> bookName
        BookFileNameStyle.SLUG -> bookName.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]+"), "")
    }
}
