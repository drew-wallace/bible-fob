package com.example.biblefob.data

interface BibleRepository {
    /**
     * Fetches verses for a normalized reference such as `John 3:16-19`.
     */
    fun getVerses(normalizedReference: String): List<Verse>

    /**
     * Fetches verses for a pre-parsed passage range.
     */
    fun getVerses(range: PassageRange): List<Verse>
}
