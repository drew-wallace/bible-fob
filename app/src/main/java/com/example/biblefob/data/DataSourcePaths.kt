package com.example.biblefob.data

import java.io.File

/**
 * Canonical on-disk locations for Bible content.
 *
 * Asset paths are relative to `app/src/main/assets/`.
 */
object DataSourcePaths {
    const val WHOLE_BIBLE_JSON = "bible/whole_bible.json"
    const val PER_BOOK_JSON_DIR = "bible/books"

    const val SQLITE_DB_ASSET = "database/bible.db"
    const val SQLITE_SCHEMA_ASSET = "database/schema.sql"

    private const val IMPORTED_VERSIONS_DIR = "imported_versions"

    fun wholeBibleJson(version: String): String = "bible/${version}_bible.json"

    fun perBookJsonDir(version: String): String = "bible/${version}_books"

    fun sqliteDbAsset(version: String): String = "database/${version}_bible.db"

    fun sqlDumpAsset(version: String): String = "database/${version}_bible.sql"

    fun localSqliteDbFile(appFilesDir: File, version: String): File {
        return File(appFilesDir, "$IMPORTED_VERSIONS_DIR/${version}_bible.db")
    }

    fun localSqlDumpFile(appFilesDir: File, version: String): File {
        return File(appFilesDir, "$IMPORTED_VERSIONS_DIR/${version}_bible.sql")
    }
}
