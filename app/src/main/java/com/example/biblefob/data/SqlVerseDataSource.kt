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

        val versesByBookId = queryVerses(
            db = db,
            tableName = tableName,
            selectionWithArgs = buildRangeSelection(range)
        )
        if (versesByBookId.isNotEmpty()) {
            return versesByBookId
        }

        val versesByBookName = queryVerses(
            db = db,
            tableName = tableName,
            selectionWithArgs = buildRangeSelectionByBookName(range)
        )

        return versesByBookName
    }

    private fun queryVerses(
        db: android.database.sqlite.SQLiteDatabase,
        tableName: String,
        selectionWithArgs: Pair<String, Array<String>>
    ): List<Verse> {
        val (selection, args) = selectionWithArgs

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

internal fun buildRangeSelectionByBookName(range: PassageRange): Pair<String, Array<String>> {
    val canonicalBookName = range.startBook.name
    val aliasNames = canonicalBookName.aliasBookNames()
    val placeholders = List(aliasNames.size) { "?" }.joinToString(", ")
    val bookNameClause = "LOWER(book) IN ($placeholders)"
    val bookNameArgs = aliasNames.map { it.lowercase() }

    return if (range.startChapter == range.endChapter) {
        "$bookNameClause AND chapter = ? AND verse BETWEEN ? AND ?" to
            (bookNameArgs + listOf(
                range.startChapter.toString(),
                range.startVerse.toString(),
                range.endVerse.toString()
            )).toTypedArray()
    } else {
        buildString {
            append(bookNameClause)
            append(" AND (")
            append("(chapter = ? AND verse >= ?) OR ")
            append("(chapter > ? AND chapter < ?) OR ")
            append("(chapter = ? AND verse <= ?)")
            append(")")
        } to (bookNameArgs + listOf(
            range.startChapter.toString(),
            range.startVerse.toString(),
            range.startChapter.toString(),
            range.endChapter.toString(),
            range.endChapter.toString(),
            range.endVerse.toString()
        )).toTypedArray()
    }
}

private fun String.aliasBookNames(): List<String> {
    val aliases = when (this) {
        "Psalms" -> listOf("Psalm")
        "Song of Songs" -> listOf("Song of Solomon")
        else -> emptyList()
    }

    return listOf(this) + aliases
}

internal fun buildRangeSelection(range: PassageRange): Pair<String, Array<String>> {
    return if (range.startChapter == range.endChapter) {
        "book_id = ? AND chapter = ? AND verse BETWEEN ? AND ?" to arrayOf(
            range.startBook.id.toString(),
            range.startChapter.toString(),
            range.startVerse.toString(),
            range.endVerse.toString()
        )
    } else {
        buildString {
            append("book_id = ? AND (")
            append("(chapter = ? AND verse >= ?) OR ")
            append("(chapter > ? AND chapter < ?) OR ")
            append("(chapter = ? AND verse <= ?)")
            append(")")
        } to arrayOf(
            range.startBook.id.toString(),
            range.startChapter.toString(),
            range.startVerse.toString(),
            range.startChapter.toString(),
            range.endChapter.toString(),
            range.endChapter.toString(),
            range.endVerse.toString()
        )
    }
}
