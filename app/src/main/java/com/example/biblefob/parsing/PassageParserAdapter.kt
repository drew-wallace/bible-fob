package com.example.biblefob.parsing

import java.util.Locale

/**
 * Normalizes a single Bible passage reference string.
 *
 * This adapter keeps parser-specific logic isolated so it can be swapped with a JS bridge-backed
 * parser in the future without changing the higher-level query parsing flow.
 */
class PassageParserAdapter {
    fun parse(reference: String): String? {
        val trimmedReference = reference.trim()
        if (trimmedReference.isEmpty()) {
            return null
        }

        val match = REFERENCE_REGEX.matchEntire(trimmedReference) ?: return null
        val normalizedBook = normalizeBook(match.groupValues[1]) ?: return null
        val chapter = match.groupValues[2].toIntOrNull() ?: return null
        val startVerse = match.groupValues[3].toIntOrNull()
        val endVerse = match.groupValues[4].toIntOrNull()

        val chapterSection = when {
            startVerse == null -> "$chapter"
            endVerse == null -> "$chapter:$startVerse"
            else -> "$chapter:$startVerse-$endVerse"
        }

        return "$normalizedBook $chapterSection"
    }

    private fun normalizeBook(bookToken: String): String? {
        val canonicalKey = canonicalizeBookToken(bookToken)
        return BOOK_ALIASES[canonicalKey]
    }

    private fun canonicalizeBookToken(rawToken: String): String {
        val compact = rawToken
            .trim()
            .replace('.', ' ')
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.US)

        val romanNormalized = compact
            .replace(Regex("^iii\\s+"), "3 ")
            .replace(Regex("^ii\\s+"), "2 ")
            .replace(Regex("^i\\s+"), "1 ")

        return romanNormalized.replace(" ", "")
    }

    private companion object {
        val REFERENCE_REGEX = Regex(
            pattern = "^([1-3]?[A-Za-z. ]+?)\\s+(\\d+)(?::(\\d+)(?:\\s*-\\s*(\\d+))?)?$"
        )

        val BOOK_ALIASES = buildMap {
            // Pentateuch
            putAll(bookAliases("Genesis", "gen", "ge", "gn"))
            putAll(bookAliases("Exodus", "exodus", "exo", "ex", "exod"))
            putAll(bookAliases("Leviticus", "leviticus", "lev", "le", "lv"))
            putAll(bookAliases("Numbers", "numbers", "num", "nu", "nm", "nb"))
            putAll(bookAliases("Deuteronomy", "deuteronomy", "deut", "deu", "dt"))

            // History
            putAll(bookAliases("Joshua", "joshua", "josh", "jos", "jsh"))
            putAll(bookAliases("Judges", "judges", "judg", "jdg", "jg", "jdgs"))
            putAll(bookAliases("Ruth", "ruth", "rth", "ru"))
            putAll(numberedBookAliases(1, "Samuel", "samuel", "sam", "sa", "sm"))
            putAll(numberedBookAliases(2, "Samuel", "samuel", "sam", "sa", "sm"))
            putAll(numberedBookAliases(1, "Kings", "kings", "king", "kgs", "kg", "ki"))
            putAll(numberedBookAliases(2, "Kings", "kings", "king", "kgs", "kg", "ki"))
            putAll(numberedBookAliases(1, "Chronicles", "chronicles", "chron", "chr", "ch"))
            putAll(numberedBookAliases(2, "Chronicles", "chronicles", "chron", "chr", "ch"))
            putAll(bookAliases("Ezra", "ezra", "ezr", "ez"))
            putAll(bookAliases("Nehemiah", "nehemiah", "neh", "ne"))
            putAll(bookAliases("Esther", "esther", "est", "es"))

            // Wisdom
            putAll(bookAliases("Job", "job", "jb"))
            putAll(bookAliases("Psalms", "psalm", "psalms", "ps", "psa", "psm", "pss"))
            putAll(bookAliases("Proverbs", "proverbs", "prov", "pro", "prv", "pr"))
            putAll(bookAliases("Ecclesiastes", "ecclesiastes", "eccles", "eccle", "ecc", "ec", "qoh"))
            putAll(bookAliases("Song of Songs", "songofsongs", "songofsolomon", "song", "songs", "sos", "so", "canticleofcanticles"))

            // Major prophets
            putAll(bookAliases("Isaiah", "isaiah", "isa", "is"))
            putAll(bookAliases("Jeremiah", "jeremiah", "jer", "je", "jr"))
            putAll(bookAliases("Lamentations", "lamentations", "lam", "la"))
            putAll(bookAliases("Ezekiel", "ezekiel", "ezek", "eze", "ezk"))
            putAll(bookAliases("Daniel", "daniel", "dan", "da", "dn"))

            // Minor prophets
            putAll(bookAliases("Hosea", "hosea", "hos", "ho"))
            putAll(bookAliases("Joel", "joel", "joe", "jl"))
            putAll(bookAliases("Amos", "amos", "amo", "am"))
            putAll(bookAliases("Obadiah", "obadiah", "obad", "oba", "ob"))
            putAll(bookAliases("Jonah", "jonah", "jon", "jnh"))
            putAll(bookAliases("Micah", "micah", "mic", "mc"))
            putAll(bookAliases("Nahum", "nahum", "nah", "na"))
            putAll(bookAliases("Habakkuk", "habakkuk", "hab", "hb"))
            putAll(bookAliases("Zephaniah", "zephaniah", "zeph", "zep", "zp"))
            putAll(bookAliases("Haggai", "haggai", "hag", "hg"))
            putAll(bookAliases("Zechariah", "zechariah", "zech", "zec", "zc"))
            putAll(bookAliases("Malachi", "malachi", "mal", "ml"))

            // Gospels + Acts
            putAll(bookAliases("Matthew", "matthew", "matt", "mat", "mt"))
            putAll(bookAliases("Mark", "mark", "mrk", "mar", "mk", "mr"))
            putAll(bookAliases("Luke", "luke", "luk", "lk"))
            putAll(bookAliases("John", "john", "jhn", "jn", "joh"))
            putAll(bookAliases("Acts", "acts", "act", "ac"))

            // Pauline epistles
            putAll(bookAliases("Romans", "romans", "rom", "ro", "rm"))
            putAll(numberedBookAliases(1, "Corinthians", "corinthians", "cor", "co"))
            putAll(numberedBookAliases(2, "Corinthians", "corinthians", "cor", "co"))
            putAll(bookAliases("Galatians", "galatians", "gal", "ga"))
            putAll(bookAliases("Ephesians", "ephesians", "eph", "ep"))
            putAll(bookAliases("Philippians", "philippians", "phil", "php", "pp"))
            putAll(bookAliases("Colossians", "colossians", "col", "co"))
            putAll(numberedBookAliases(1, "Thessalonians", "thessalonians", "thess", "thes", "th"))
            putAll(numberedBookAliases(2, "Thessalonians", "thessalonians", "thess", "thes", "th"))
            putAll(numberedBookAliases(1, "Timothy", "timothy", "tim", "ti", "tm"))
            putAll(numberedBookAliases(2, "Timothy", "timothy", "tim", "ti", "tm"))
            putAll(bookAliases("Titus", "titus", "tit", "ti"))
            putAll(bookAliases("Philemon", "philemon", "philem", "phm", "pm"))

            // General epistles
            putAll(bookAliases("Hebrews", "hebrews", "heb", "he"))
            putAll(bookAliases("James", "james", "jas", "jam", "jm"))
            putAll(numberedBookAliases(1, "Peter", "peter", "pet", "pe", "pt"))
            putAll(numberedBookAliases(2, "Peter", "peter", "pet", "pe", "pt"))
            putAll(numberedBookAliases(1, "John", "john", "jn", "jhn"))
            putAll(numberedBookAliases(2, "John", "john", "jn", "jhn"))
            putAll(numberedBookAliases(3, "John", "john", "jn", "jhn"))
            putAll(bookAliases("Jude", "jude", "jud"))

            // Apocalypse
            putAll(bookAliases("Revelation", "revelation", "rev", "re", "theapocalypse"))
        }

        fun bookAliases(canonical: String, vararg aliases: String): Map<String, String> {
            val all = buildSet {
                add(canonical)
                addAll(aliases)
            }

            return all.associate { raw ->
                raw
                    .lowercase(Locale.US)
                    .replace('.', ' ')
                    .replace(Regex("\\s+"), "") to canonical
            }
        }

        fun numberedBookAliases(number: Int, baseCanonical: String, vararg aliases: String): Map<String, String> {
            val canonical = "$number $baseCanonical"
            val prefixedAliases = buildSet {
                add(canonical)
                add("$number$baseCanonical")
                aliases.forEach { alias ->
                    add("$number $alias")
                    add("$number$alias")
                }
            }

            return prefixedAliases.associate { raw ->
                raw
                    .lowercase(Locale.US)
                    .replace('.', ' ')
                    .replace(Regex("\\s+"), "") to canonical
            }
        }
    }
}
