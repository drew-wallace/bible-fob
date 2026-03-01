package com.example.biblefob.domain

import android.content.Context
import android.net.Uri
import com.example.biblefob.data.VersionCatalogRepository
import com.example.biblefob.data.VersionEntry
import org.json.JSONArray
import org.json.JSONObject

class ImportVersionFromJsonUseCase(
    private val context: Context,
    private val versionCatalogRepository: VersionCatalogRepository
) {
    suspend operator fun invoke(uri: Uri, defaultDisplayName: String): Result<VersionEntry> {
        val rawJson = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return Result.failure(IllegalArgumentException("Unable to read the selected file."))

        if (!isSupportedBibleJson(rawJson)) {
            return Result.failure(IllegalArgumentException("Selected file is not valid Bible JSON."))
        }

        val entry = runCatching {
            buildEntry(uri = uri, defaultDisplayName = defaultDisplayName)
        }.getOrElse {
            return Result.failure(IllegalArgumentException("Unable to import selected version."))
        }

        versionCatalogRepository.upsertUserEntry(entry)
        return Result.success(entry)
    }

    private fun buildEntry(uri: Uri, defaultDisplayName: String): VersionEntry {
        val displayName = defaultDisplayName.trim()
        val id = displayName.toVersionId().ifBlank { "IMPORTED" }

        require(displayName.isNotBlank())
        require(id.isNotBlank())

        val contentUriPath = uri.toString()

        return VersionEntry(
            id = id,
            displayName = displayName,
            sqliteDbAssetPath = contentUriPath,
            sqlDumpAssetPath = contentUriPath
        )
    }

    private fun isSupportedBibleJson(rawJson: String): Boolean {
        return runCatching { JSONObject(rawJson) }.isSuccess || runCatching { JSONArray(rawJson) }.isSuccess
    }
}

private fun String.toVersionId(): String {
    return uppercase()
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
}
