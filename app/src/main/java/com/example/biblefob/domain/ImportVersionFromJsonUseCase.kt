package com.example.biblefob.domain

import android.content.Context
import android.net.Uri
import com.example.biblefob.data.VersionCatalogRepository
import com.example.biblefob.data.VersionEntry
import org.json.JSONObject

class ImportVersionFromJsonUseCase(
    private val context: Context,
    private val versionCatalogRepository: VersionCatalogRepository
) {
    suspend operator fun invoke(uri: Uri, defaultDisplayName: String): Result<VersionEntry> {
        val rawJson = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return Result.failure(IllegalArgumentException("Unable to read the selected file."))

        val entry = runCatching {
            buildEntry(JSONObject(rawJson), uri = uri, defaultDisplayName = defaultDisplayName)
        }.getOrElse {
            return Result.failure(IllegalArgumentException("Invalid version JSON format."))
        }

        versionCatalogRepository.upsertUserEntry(entry)
        return Result.success(entry)
    }

    private fun buildEntry(jsonObject: JSONObject, uri: Uri, defaultDisplayName: String): VersionEntry {
        val displayName = jsonObject.optString(JSON_DISPLAY_NAME)
            .trim()
            .ifBlank { defaultDisplayName.trim() }
        val id = jsonObject.optString(JSON_ID)
            .trim()
            .ifBlank { displayName.toVersionId() }
            .uppercase()

        require(displayName.isNotBlank())
        require(id.isNotBlank())

        val fallbackPath = uri.toString()

        return VersionEntry(
            id = id,
            displayName = displayName,
            sqliteDbAssetPath = jsonObject.optString(JSON_SQLITE_DB_ASSET_PATH).trim().ifBlank { fallbackPath },
            sqlDumpAssetPath = jsonObject.optString(JSON_SQL_DUMP_ASSET_PATH).trim().ifBlank { fallbackPath }
        )
    }
}

private const val JSON_ID = "id"
private const val JSON_DISPLAY_NAME = "displayName"
private const val JSON_SQLITE_DB_ASSET_PATH = "sqliteDbAssetPath"
private const val JSON_SQL_DUMP_ASSET_PATH = "sqlDumpAssetPath"

private fun String.toVersionId(): String {
    return uppercase()
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
}
