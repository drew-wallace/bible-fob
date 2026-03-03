package com.example.biblefob.data.import

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.biblefob.data.CanonicalBooks
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class VersionJsonImporter(
    private val context: Context,
    private val contentResolver: ContentResolver = context.contentResolver
) {
    fun import(uri: Uri, versionId: String): ImportOutput {
        val normalizedVersionId = versionId.trim().uppercase()
        if (normalizedVersionId.isBlank()) {
            throw ImportException("Version id cannot be blank.")
        }

        val rawJson = readJsonFromUri(uri)
        val verseRows = parseVerseRows(rawJson)
        if (verseRows.isEmpty()) {
            throw ImportException("JSON did not contain any verses.")
        }

        val outputFile = sqlDumpFileForVersion(normalizedVersionId)
        if (outputFile.exists()) {
            throw DuplicateImportException(normalizedVersionId)
        }

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(buildSqlDump(verseRows))

        return ImportOutput(
            versionId = normalizedVersionId,
            sqlDumpFile = outputFile,
            verseCount = verseRows.size
        )
    }

    private fun readJsonFromUri(uri: Uri): String {
        return runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: throw ImportException("Unable to read selected JSON file.")
    }

    private fun parseVerseRows(rawJson: String): List<VerseRow> {
        val root = try {
            JSONObject(rawJson)
        } catch (_: JSONException) {
            throw ImportException("Malformed JSON. Expected object keyed by book names.")
        }

        if (root.length() == 0) {
            throw ImportException("JSON did not contain any books.")
        }

        val rows = mutableListOf<VerseRow>()
        val seenVerseKeys = mutableSetOf<String>()

        root.keys().forEach { bookNameRaw ->
            val bookName = bookNameRaw.trim()
            val canonicalBook = CanonicalBooks.byName(bookName)
                ?: throw ImportException("Unsupported or unknown book '$bookName'.")
            val chapterObject = root.optJSONObject(bookNameRaw)
                ?: throw ImportException("Book '$bookName' must map to an object of chapters.")
            if (chapterObject.length() == 0) {
                throw ImportException("Book '$bookName' did not contain any chapters.")
            }

            chapterObject.keys().forEach { chapterKey ->
                val chapter = chapterKey.toIntOrNull()
                    ?: throw ImportException("Book '$bookName' has non-numeric chapter '$chapterKey'.")
                if (chapter <= 0) {
                    throw ImportException("Book '$bookName' has invalid chapter '$chapterKey'.")
                }

                val verseObject = chapterObject.optJSONObject(chapterKey)
                    ?: throw ImportException("$bookName $chapter must map to an object of verses.")
                if (verseObject.length() == 0) {
                    throw ImportException("$bookName $chapter did not contain any verses.")
                }

                verseObject.keys().forEach { verseKey ->
                    val verse = verseKey.toIntOrNull()
                        ?: throw ImportException("$bookName $chapter has non-numeric verse '$verseKey'.")
                    if (verse <= 0) {
                        throw ImportException("$bookName $chapter has invalid verse '$verseKey'.")
                    }

                    val text = verseObject.optString(verseKey).trim()
                    if (text.isBlank()) {
                        throw ImportException("$bookName $chapter:$verse has blank verse text.")
                    }

                    val deDupKey = "${canonicalBook.id}-$chapter-$verse"
                    if (!seenVerseKeys.add(deDupKey)) {
                        throw ImportException("Duplicate verse detected for $bookName $chapter:$verse.")
                    }

                    rows += VerseRow(
                        bookId = canonicalBook.id,
                        book = canonicalBook.name,
                        chapter = chapter,
                        verse = verse,
                        text = text
                    )
                }
            }
        }

        return rows.sortedWith(compareBy<VerseRow> { it.bookId }.thenBy { it.chapter }.thenBy { it.verse })
    }

    private fun buildSqlDump(rows: List<VerseRow>): String {
        val inserts = rows.joinToString(separator = ",\n") { row ->
            "(${row.bookId},'${escapeSql(row.book)}',${row.chapter},${row.verse},'${escapeSql(row.text)}')"
        }

        return buildString {
            appendLine("CREATE TABLE IF NOT EXISTS verses(")
            appendLine("  book_id INTEGER NOT NULL,")
            appendLine("  book TEXT NOT NULL,")
            appendLine("  chapter INTEGER NOT NULL,")
            appendLine("  verse INTEGER NOT NULL,")
            appendLine("  text TEXT NOT NULL,")
            appendLine("  PRIMARY KEY (book_id, chapter, verse)")
            appendLine(");")
            append("INSERT INTO verses(book_id, book, chapter, verse, text) VALUES\n")
            append(inserts)
            appendLine(";")
        }
    }

    private fun sqlDumpFileForVersion(versionId: String): File {
        return File(context.filesDir, "imported_versions/${versionId}_bible.sql")
    }

    private fun escapeSql(value: String): String = value.replace("'", "''")
}

data class ImportOutput(
    val versionId: String,
    val sqlDumpFile: File,
    val verseCount: Int
)

open class ImportException(message: String) : IllegalArgumentException(message)

class DuplicateImportException(versionId: String) :
    ImportException("Version $versionId has already been imported.")

private data class VerseRow(
    val bookId: Int,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)
