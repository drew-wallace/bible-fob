package com.example.biblefob.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Builds a local SQLite database from a bundled SQL dump asset on first launch.
 *
 * The SQL file is expected to contain standard statements separated by ';'
 * (e.g. CREATE TABLE + INSERT rows for Bible content).
 */
class AssetSqlDumpSQLiteOpenHelper(
    context: Context,
    private val sqlDumpAssetPath: String,
    databaseName: String,
    version: Int = 1
) : SQLiteOpenHelper(context, databaseName, null, version) {

    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        val script = appContext.assets.open(sqlDumpAssetPath)
            .bufferedReader()
            .use { it.readText() }

        db.beginTransaction()
        try {
            script
                .split(';')
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { statement ->
                    db.execSQL(statement)
                }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    companion object {
        fun forVersion(
            context: Context,
            version: String,
            databaseName: String = "${version}_bible.db",
            schemaVersion: Int = 1
        ): AssetSqlDumpSQLiteOpenHelper {
            return AssetSqlDumpSQLiteOpenHelper(
                context = context,
                sqlDumpAssetPath = DataSourcePaths.sqlDumpAsset(version),
                databaseName = databaseName,
                version = schemaVersion
            )
        }
    }
}
