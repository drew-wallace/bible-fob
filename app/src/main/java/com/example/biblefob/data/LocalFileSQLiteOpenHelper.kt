package com.example.biblefob.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

/**
 * Opens a SQLite database file in app-private storage.
 */
class LocalFileSQLiteOpenHelper(
    context: Context,
    private val databaseFilePath: String,
    version: Int = 1
) : SQLiteOpenHelper(context, databaseFilePath, null, version) {
    override fun onCreate(db: SQLiteDatabase) = Unit

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

object LocalFileDatabaseFactory {
    fun createOrNull(
        context: Context,
        entry: VersionEntry,
        schemaVersion: Int
    ): SQLiteOpenHelper? {
        val localDbFile = File(entry.sqliteDbAssetPath)
        if (localDbFile.exists()) {
            return LocalFileSQLiteOpenHelper(
                context = context,
                databaseFilePath = localDbFile.absolutePath,
                version = schemaVersion
            )
        }

        val sqlDumpFile = File(entry.sqlDumpAssetPath)
        if (sqlDumpFile.exists()) {
            return AssetSqlDumpSQLiteOpenHelper(
                context = context,
                sqlDumpAssetPath = sqlDumpFile.absolutePath,
                databaseName = entry.sqliteDbAssetPath,
                version = schemaVersion
            )
        }

        return null
    }
}
