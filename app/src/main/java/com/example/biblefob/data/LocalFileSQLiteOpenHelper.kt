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
        metadata: VersionDataSourceMetadata,
        schemaVersion: Int
    ): SQLiteOpenHelper? {
        if (metadata.type != VersionDataSourceType.LOCAL_FILE) return null

        val localDbFile = File(metadata.sqliteDbPath)
        if (localDbFile.exists()) {
            return LocalFileSQLiteOpenHelper(
                context = context,
                databaseFilePath = localDbFile.absolutePath,
                version = schemaVersion
            )
        }

        val sqlDumpFile = File(metadata.sqlDumpPath)
        if (sqlDumpFile.exists()) {
            return LocalSqlDumpSQLiteOpenHelper(
                context = context,
                sqlDumpFilePath = sqlDumpFile.absolutePath,
                databaseFilePath = metadata.sqliteDbPath,
                version = schemaVersion
            )
        }

        return null
    }
}

class LocalSqlDumpSQLiteOpenHelper(
    context: Context,
    private val sqlDumpFilePath: String,
    private val databaseFilePath: String,
    version: Int = 1
) : SQLiteOpenHelper(context, databaseFilePath, null, version) {

    override fun onCreate(db: SQLiteDatabase) {
        val script = File(sqlDumpFilePath)
            .takeIf(File::exists)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: return

        db.beginTransaction()
        try {
            splitSqlStatements(script).forEach(db::execSQL)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        File(databaseFilePath).parentFile?.mkdirs()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}
