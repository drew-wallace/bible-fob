package com.example.biblefob.navigation

import com.example.biblefob.data.VersionEntry

internal fun normalizeVersionOrNull(version: String, supportedVersions: List<VersionEntry>): String? {
    val normalizedVersion = version.toVersionLookupKey()
    if (normalizedVersion.isBlank()) return null

    return supportedVersions
        .firstOrNull { entry -> entry.id.toVersionLookupKey() == normalizedVersion }
        ?.id
}

private fun String.toVersionLookupKey(): String {
    return trim()
        .uppercase()
        .replace(Regex("[^A-Z0-9]+"), "")
}
