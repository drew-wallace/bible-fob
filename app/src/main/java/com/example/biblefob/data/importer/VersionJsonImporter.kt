package com.example.biblefob.data.importer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.biblefob.data.CanonicalBooks
import com.example.biblefob.data.DataSourcePaths
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

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
        val verseRows = parseBibleJsonToVerseRows(rawJson)
        if (verseRows.isEmpty()) {
            throw ImportException("JSON did not contain any verses.")
        }

        val outputFile = sqlDumpFileForVersion(normalizedVersionId)
        val sqlDump = buildSqlDump(verseRows)
        if (outputFile.exists()) {
            val existingDigest = outputFile.inputStream().use(::sha256)
            val newDigest = sha256(sqlDump.byteInputStream())
            if (existingDigest == newDigest) {
                throw DuplicateImportException(normalizedVersionId)
            }
            throw ConflictingImportException(normalizedVersionId)
        }

        outputFile.parentFile?.mkdirs()
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        tempFile.writeText(sqlDump)
        if (!tempFile.renameTo(outputFile)) {
            tempFile.delete()
            throw ImportException("Failed to store imported version in app-private storage.")
        }

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
        return DataSourcePaths.Imported.sqlDumpFile(context.filesDir, versionId)
    }

    private fun escapeSql(value: String): String = value.replace("'", "''")

    private fun sha256(input: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read = input.read(buffer)
        while (read >= 0) {
            if (read > 0) digest.update(buffer, 0, read)
            read = input.read(buffer)
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }
}

data class ImportOutput(
    val versionId: String,
    val sqlDumpFile: File,
    val verseCount: Int
)

open class ImportException(message: String) : IllegalArgumentException(message)

class DuplicateImportException(versionId: String) :
    ImportException("Version $versionId has already been imported.")

class ConflictingImportException(versionId: String) :
    ImportException("Version $versionId already exists with different content.")

internal data class VerseRow(
    val bookId: Int,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

private val bookAliases = mapOf(
    "Psalm" to "Psalms",
    "Song Of Solomon" to "Song of Songs"
)

internal fun parseBibleJsonToVerseRows(rawJson: String): List<VerseRow> {
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
        val canonicalBook = canonicalBookFor(bookName)
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

                val rawText = verseObject.opt(verseKey)
                if (rawText !is String) {
                    throw ImportException("$bookName $chapter:$verse must contain string verse text.")
                }

                val text = rawText.trim()

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

private fun canonicalBookFor(name: String) =
    CanonicalBooks.byName(name) ?: bookAliases[name]?.let(CanonicalBooks::byName)
