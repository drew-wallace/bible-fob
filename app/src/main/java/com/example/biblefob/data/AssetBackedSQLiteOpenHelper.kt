package com.example.biblefob.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

/**
 * Copies a prebuilt SQLite DB from assets (default: [DataSourcePaths.SQLITE_DB_ASSET]) into
 * app-internal storage and exposes it through [SQLiteOpenHelper].
 */
class AssetBackedSQLiteOpenHelper(
    context: Context,
    private val assetPath: String = DataSourcePaths.SQLITE_DB_ASSET,
    databaseName: String = "bible.db",
    version: Int = 1
) : SQLiteOpenHelper(context, databaseName, null, version) {

    private val appContext = context.applicationContext
    private val dbName = databaseName

    override fun onCreate(db: SQLiteDatabase) {
        // Prebuilt DB path is handled in [ensureDatabaseCopied].
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun getReadableDatabase(): SQLiteDatabase {
        ensureDatabaseCopied()
        return super.getReadableDatabase()
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        ensureDatabaseCopied()
        return super.getWritableDatabase()
    }

    private fun ensureDatabaseCopied() {
        val databaseFile = appContext.getDatabasePath(dbName)
        if (databaseFile.exists()) {
            return
        }

        databaseFile.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }

        copyAssetDatabase(databaseFile)
    }

    private fun copyAssetDatabase(destination: File) {
        appContext.assets.open(assetPath).use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    companion object {
        fun forVersion(
            context: Context,
            version: String,
            databaseName: String = "${version}_bible.db",
            schemaVersion: Int = 1
        ): AssetBackedSQLiteOpenHelper {
            return AssetBackedSQLiteOpenHelper(
                context = context,
                assetPath = DataSourcePaths.sqliteDbAsset(version),
                databaseName = databaseName,
                version = schemaVersion
            )
        }
    }
}
