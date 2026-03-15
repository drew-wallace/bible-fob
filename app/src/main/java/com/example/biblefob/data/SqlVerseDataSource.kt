package com.example.biblefob.data

import android.database.sqlite.SQLiteOpenHelper

interface SqlVerseDataSource {
    fun getVerses(range: PassageRange): List<Verse>
}

class SQLiteVerseDataSource(
    private val helper: SQLiteOpenHelper,
    tableNames: List<String> = listOf("verses")
) : SqlVerseDataSource {
    private val candidateTableNames = tableNames
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    @Volatile
    private var resolvedTableName: String? = null

    override fun getVerses(range: PassageRange): List<Verse> {
        if (range.startBook.id != range.endBook.id) {
            return emptyList()
        }

        val db = helper.readableDatabase
        val tableName = resolveTableName(db) ?: return emptyList()
        val selection = buildString {
            append("book_id = ? AND (")
            append("(chapter > ? AND chapter < ?) OR ")
            append("(chapter = ? AND verse >= ?) OR ")
            append("(chapter = ? AND verse <= ?)")
            append(")")
        }

        val args = arrayOf(
            range.startBook.id.toString(),
            range.startChapter.toString(),
            range.endChapter.toString(),
            range.startChapter.toString(),
            range.startVerse.toString(),
            range.endChapter.toString(),
            range.endVerse.toString()
        )

        val verses = mutableListOf<Verse>()
        db.query(
            tableName,
            arrayOf("book_id", "book", "chapter", "verse", "text"),
            selection,
            args,
            null,
            null,
            "book_id ASC, chapter ASC, verse ASC"
        ).use { cursor ->
            val bookIdIndex = cursor.getColumnIndexOrThrow("book_id")
            val bookNameIndex = cursor.getColumnIndexOrThrow("book")
            val chapterIndex = cursor.getColumnIndexOrThrow("chapter")
            val verseIndex = cursor.getColumnIndexOrThrow("verse")
            val textIndex = cursor.getColumnIndexOrThrow("text")

            while (cursor.moveToNext()) {
                verses += Verse(
                    book = Book(
                        id = cursor.getInt(bookIdIndex),
                        name = cursor.getString(bookNameIndex)
                    ),
                    chapter = cursor.getInt(chapterIndex),
                    number = cursor.getInt(verseIndex),
                    text = cursor.getString(textIndex)
                )
            }
        }

        return verses
    }

    private fun resolveTableName(db: android.database.sqlite.SQLiteDatabase): String? {
        resolvedTableName?.let { return it }

        candidateTableNames.firstOrNull { tableName -> hasVerseSchema(db, tableName) }
            ?.also { resolvedTableName = it }
            ?.let { return it }

        val discoveredTable = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
            null
        ).use { cursor ->
            generateSequence {
                if (cursor.moveToNext()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }.firstOrNull { tableName -> hasVerseSchema(db, tableName) }
        }

        resolvedTableName = discoveredTable
        return discoveredTable
    }


    private fun quoteIdentifier(identifier: String): String {
        return "\"${identifier.replace("\"", "\"\"")}\""
    }

    private fun hasVerseSchema(db: android.database.sqlite.SQLiteDatabase, tableName: String): Boolean {
        val requiredColumns = setOf("book_id", "book", "chapter", "verse", "text")
        val tableColumns = db.rawQuery("PRAGMA table_info(${quoteIdentifier(tableName)})", null).use { cursor ->
            buildSet {
                val columnNameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (columnNameIndex >= 0) {
                        add(cursor.getString(columnNameIndex))
                    }
                }
            }
        }

        return tableColumns.containsAll(requiredColumns)
    }
}
