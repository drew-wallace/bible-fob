package com.example.biblefob.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

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
        val script = readSqlScript()

        db.beginTransaction()
        try {
            splitSqlStatements(script)
                .forEach { statement ->
                    db.execSQL(statement)
                }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private fun readSqlScript(): String {
        val file = File(sqlDumpAssetPath)
        if (file.exists()) {
            return file.bufferedReader().use { it.readText() }
        }

        return appContext.assets.open(sqlDumpAssetPath)
            .bufferedReader()
            .use { it.readText() }
    }

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

internal fun splitSqlStatements(script: String): List<String> {
    if (script.isBlank()) {
        return emptyList()
    }

    val statements = mutableListOf<String>()
    val current = StringBuilder()
    var index = 0
    var inSingleQuotedString = false

    while (index < script.length) {
        val char = script[index]

        if (char == '\'') {
            current.append(char)
            if (inSingleQuotedString && index + 1 < script.length && script[index + 1] == '\'') {
                current.append(script[index + 1])
                index += 2
                continue
            }
            inSingleQuotedString = !inSingleQuotedString
            index += 1
            continue
        }

        if (char == ';' && !inSingleQuotedString) {
            val statement = current.toString().trim()
            if (statement.isNotBlank()) {
                statements += statement
            }
            current.clear()
            index += 1
            continue
        }

        current.append(char)
        index += 1
    }

    val trailingStatement = current.toString().trim()
    if (trailingStatement.isNotBlank()) {
        statements += trailingStatement
    }

    return statements
}
