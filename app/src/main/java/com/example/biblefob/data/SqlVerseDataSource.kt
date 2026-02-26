package com.example.biblefob.data

import android.database.sqlite.SQLiteOpenHelper

interface SqlVerseDataSource {
    fun getVerses(range: PassageRange): List<Verse>
}

class SQLiteVerseDataSource(
    private val helper: SQLiteOpenHelper,
    private val tableName: String = "verses"
) : SqlVerseDataSource {
    override fun getVerses(range: PassageRange): List<Verse> {
        if (range.startBook.id != range.endBook.id) {
            return emptyList()
        }

        val db = helper.readableDatabase
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
}
