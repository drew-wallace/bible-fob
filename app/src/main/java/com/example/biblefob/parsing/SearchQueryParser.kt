package com.example.biblefob.parsing

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class SearchQueryParser(
    private val passageParserAdapter: PassageParserAdapter = PassageParserAdapter()
) {
    fun parseFromUri(uri: URI?): List<String> {
        if (uri == null) {
            return emptyList()
        }

        return parseSearchValue(extractSearchFromRawQuery(uri.rawQuery))
    }

    fun parseFromUriString(uriString: String?): List<String> {
        if (uriString.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching { URI(uriString) }
            .map(::parseFromUri)
            .getOrDefault(emptyList())
    }

    fun parseFromSearchQuery(searchQuery: String?): List<String> {
        return parseSearchValue(searchQuery)
    }

    private fun extractSearchFromRawQuery(rawQuery: String?): String {
        if (rawQuery.isNullOrBlank()) {
            return ""
        }

        return rawQuery
            .split('&')
            .asSequence()
            .mapNotNull(::toQueryPair)
            .firstOrNull { (key, _) -> key == SEARCH_PARAM }
            ?.second
            ?.let(::urlDecode)
            .orEmpty()
    }

    private fun parseSearchValue(searchQuery: String?): List<String> {
        if (searchQuery.isNullOrBlank()) {
            return emptyList()
        }

        val decodedSearch = searchQuery.trim()

        if (decodedSearch.isBlank()) {
            return emptyList()
        }

        val normalizedReferences = mutableListOf<String>()
        var carryOverContext: ReferenceContext? = null

        TOKEN_REGEX.findAll(decodedSearch).forEach { tokenMatch ->
            val token = tokenMatch.value.trim()
            when (token) {
                ";" -> carryOverContext = null
                "," -> Unit
                else -> {
                    val parsed = parseChunk(token, carryOverContext) ?: return@forEach
                    normalizedReferences += parsed
                    carryOverContext = extractContext(parsed)
                }
            }
        }

        return normalizedReferences
    }

    private fun parseChunk(chunk: String, carryOverContext: ReferenceContext?): String? {
        passageParserAdapter.parse(chunk)?.let { return it }
        val context = carryOverContext ?: return null

        val inferredChunk = when {
            VERSE_ONLY_REGEX.matches(chunk) -> "${context.book} ${context.chapter}:$chunk"
            CHAPTER_VERSE_REGEX.matches(chunk) -> "${context.book} $chunk"
            else -> return null
        }

        return passageParserAdapter.parse(inferredChunk)
    }

    private fun extractContext(parsedReference: String): ReferenceContext? {
        val match = PARSED_CONTEXT_REGEX.matchEntire(parsedReference) ?: return null
        val book = match.groupValues[1]
        val chapter = match.groupValues[2].toIntOrNull() ?: return null
        return ReferenceContext(book = book, chapter = chapter)
    }

    private fun toQueryPair(part: String): Pair<String, String>? {
        val separatorIndex = part.indexOf('=')
        if (separatorIndex <= 0) {
            return null
        }

        val key = urlDecode(part.substring(0, separatorIndex))
        val value = part.substring(separatorIndex + 1)
        return key to value
    }

    private fun urlDecode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8)
    }

    private data class ReferenceContext(
        val book: String,
        val chapter: Int
    )

    private companion object {
        private const val SEARCH_PARAM = "search"
        private val TOKEN_REGEX = Regex("[^,;]+|[,;]")
        private val VERSE_ONLY_REGEX = Regex("^\\d+(?:\\s*-\\s*\\d+)?$")
        private val CHAPTER_VERSE_REGEX = Regex("^\\d+:\\d+(?:\\s*-\\s*\\d+)?$")
        private val PARSED_CONTEXT_REGEX = Regex("^(.+) (\\d+)(?::\\d+(?:-\\d+)?)?$")
    }
}
