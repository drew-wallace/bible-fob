package com.example.biblefob.data

import org.json.JSONObject

class JsonBibleRepository(
    wholeBibleJson: String? = null,
    private val perBookJsonLoader: ((Book) -> String?)? = null,
    private val sqlDataSource: SqlVerseDataSource? = null
) : BibleRepository {
    private val wholeBibleRoot = wholeBibleJson
        ?.takeIf { it.isNotBlank() }
        ?.let(::JSONObject)

    private val perBookCache = mutableMapOf<String, JSONObject?>()

    override fun getVerses(normalizedReference: String): List<Verse> {
        val range = NormalizedReferenceParser.parse(normalizedReference) ?: return emptyList()
        return getVerses(range)
    }

    override fun getVerses(range: PassageRange): List<Verse> {
        if (range.startBook.id != range.endBook.id) {
            return emptyList()
        }

        runCatching { sqlDataSource?.getVerses(range).orEmpty() }
            .getOrDefault(emptyList())
            .takeIf { it.isNotEmpty() }
            ?.let { return sortCanonically(it) }

        val verses = mutableListOf<Verse>()
        for (chapter in range.startChapter..range.endChapter) {
            val verseStart = if (chapter == range.startChapter) range.startVerse else 1
            val verseEnd = if (chapter == range.endChapter) range.endVerse else Int.MAX_VALUE

            if (verseEnd < verseStart) {
                continue
            }

            val verseNumbers = getVerseNumbers(range.startBook, chapter)
                .filter { it in verseStart..verseEnd }

            verseNumbers.forEach { verseNumber ->
                val text = getVerseText(
                    book = range.startBook,
                    chapter = chapter,
                    verse = verseNumber
                ) ?: return@forEach
                verses += Verse(
                    book = range.startBook,
                    chapter = chapter,
                    number = verseNumber,
                    text = text
                )
            }
        }

        return sortCanonically(verses)
    }

    private fun getVerseNumbers(book: Book, chapter: Int): List<Int> {
        val chapterJson = getChapterJson(book, chapter) ?: return emptyList()
        return chapterJson.keys()
            .asSequence()
            .mapNotNull { it.toIntOrNull() }
            .sorted()
            .toList()
    }

    private fun getVerseText(book: Book, chapter: Int, verse: Int): String? {
        val chapterJson = getChapterJson(book, chapter) ?: return null
        val key = verse.toString()
        return chapterJson.optString(key).takeIf { it.isNotBlank() }
    }

    private fun getChapterJson(book: Book, chapter: Int): JSONObject? {
        val chapterKey = chapter.toString()

        val wholeBibleBook = wholeBibleRoot?.optJSONObject(book.name)
        val fromWholeBible = wholeBibleBook?.optJSONObject(chapterKey)
        if (fromWholeBible != null) {
            return fromWholeBible
        }

        val perBookJson = perBookCache.getOrPut(book.name) {
            val loaded = perBookJsonLoader?.invoke(book) ?: return@getOrPut null
            runCatching { JSONObject(loaded) }.getOrNull()
        }

        return perBookJson?.optJSONObject(chapterKey)
    }

    private fun sortCanonically(verses: List<Verse>): List<Verse> {
        return verses.sortedWith(
            compareBy<Verse> { it.book.id }
                .thenBy { it.chapter }
                .thenBy { it.number }
        )
    }
}

object NormalizedReferenceParser {
    private val SINGLE_CHAPTER_REGEX = Regex("^(.+)\\s+(\\d+)$")
    private val CHAPTER_VERSE_REGEX = Regex("^(.+)\\s+(\\d+):(\\d+)(?:-(\\d+))?$")
    private val CROSS_CHAPTER_REGEX = Regex("^(.+)\\s+(\\d+):(\\d+)-(\\d+):(\\d+)$")

    fun parse(normalizedReference: String): PassageRange? {
        val reference = normalizedReference.trim()
        if (reference.isEmpty()) {
            return null
        }

        CROSS_CHAPTER_REGEX.matchEntire(reference)?.let { match ->
            val book = CanonicalBooks.byName(match.groupValues[1]) ?: return null
            val startChapter = match.groupValues[2].toIntOrNull() ?: return null
            val startVerse = match.groupValues[3].toIntOrNull() ?: return null
            val endChapter = match.groupValues[4].toIntOrNull() ?: return null
            val endVerse = match.groupValues[5].toIntOrNull() ?: return null
            if (!isPositive(startChapter, startVerse, endChapter, endVerse)) {
                return null
            }
            if (endChapter < startChapter || (endChapter == startChapter && endVerse < startVerse)) {
                return null
            }
            return PassageRange(
                startBook = book,
                startChapter = startChapter,
                startVerse = startVerse,
                endBook = book,
                endChapter = endChapter,
                endVerse = endVerse
            )
        }

        CHAPTER_VERSE_REGEX.matchEntire(reference)?.let { match ->
            val book = CanonicalBooks.byName(match.groupValues[1]) ?: return null
            val chapter = match.groupValues[2].toIntOrNull() ?: return null
            val startVerse = match.groupValues[3].toIntOrNull() ?: return null
            val endVerse = match.groupValues[4].toIntOrNull() ?: startVerse
            if (!isPositive(chapter, startVerse, endVerse)) {
                return null
            }
            if (endVerse < startVerse) {
                return null
            }
            return PassageRange(
                startBook = book,
                startChapter = chapter,
                startVerse = startVerse,
                endBook = book,
                endChapter = chapter,
                endVerse = endVerse
            )
        }

        SINGLE_CHAPTER_REGEX.matchEntire(reference)?.let { match ->
            val book = CanonicalBooks.byName(match.groupValues[1]) ?: return null
            val chapter = match.groupValues[2].toIntOrNull() ?: return null
            if (chapter <= 0) {
                return null
            }
            val chapterLength = CanonicalBooks.lastVerseInChapter(book, chapter) ?: Int.MAX_VALUE
            return PassageRange(
                startBook = book,
                startChapter = chapter,
                startVerse = 1,
                endBook = book,
                endChapter = chapter,
                endVerse = chapterLength
            )
        }

        return null
    }

    private fun isPositive(vararg numbers: Int): Boolean = numbers.all { it > 0 }
}

object CanonicalBooks {
    private val books = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel", "1 Kings", "2 Kings",
        "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah", "Esther", "Job", "Psalms",
        "Proverbs", "Ecclesiastes", "Song of Songs", "Isaiah", "Jeremiah", "Lamentations",
        "Ezekiel", "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah", "Nahum",
        "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi", "Matthew", "Mark", "Luke",
        "John", "Acts", "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
        "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy",
        "Titus", "Philemon", "Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John",
        "3 John", "Jude", "Revelation"
    )

    private val verseCountsByBook: Map<String, IntArray> = mapOf(
        "John" to intArrayOf(51, 25, 36, 54, 47, 71, 53, 59, 41, 42, 57, 50, 38, 31, 27, 33, 26, 40, 42, 31, 25),
        "Jude" to intArrayOf(25),
        "3 John" to intArrayOf(15)
    )

    private val byName = books.mapIndexed { index, name -> name to Book(index + 1, name) }.toMap()

    fun byName(name: String): Book? = byName[name.trim()]

    fun lastVerseInChapter(book: Book, chapter: Int): Int? {
        val chapterCounts = verseCountsByBook[book.name] ?: return null
        if (chapter !in 1..chapterCounts.size) {
            return null
        }
        return chapterCounts[chapter - 1]
    }
}
