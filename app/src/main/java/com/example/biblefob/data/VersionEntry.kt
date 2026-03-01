package com.example.biblefob.data

import org.json.JSONObject

enum class VersionManagementPolicy {
    BUNDLED,
    USER_IMPORTED;

    val canRename: Boolean
        get() = this == USER_IMPORTED

    val canDelete: Boolean
        get() = this == USER_IMPORTED
}

data class VersionEntry(
    val id: String,
    val displayName: String,
    val sqliteDbAssetPath: String,
    val sqlDumpAssetPath: String,
    val policy: VersionManagementPolicy
) {
    companion object {
        fun fromJsonOrNull(rawJson: String): VersionEntry? = runCatching {
            val jsonObject = JSONObject(rawJson)
            fromJsonObject(jsonObject)
        }.getOrNull()

        fun fromJsonObject(jsonObject: JSONObject): VersionEntry? {
            val id = jsonObject.optString(JSON_ID).trim().uppercase()
            val displayName = jsonObject.optString(JSON_DISPLAY_NAME).trim()
            val sqliteDbAssetPath = jsonObject.optString(JSON_SQLITE_DB_ASSET_PATH).trim()
            val sqlDumpAssetPath = jsonObject.optString(JSON_SQL_DUMP_ASSET_PATH).trim()
            val policy = jsonObject
                .optString(JSON_POLICY)
                .trim()
                .uppercase()
                .takeIf(String::isNotBlank)
                ?.let { rawPolicy ->
                    VersionManagementPolicy.entries.firstOrNull { candidate -> candidate.name == rawPolicy }
                }
                ?: VersionManagementPolicy.USER_IMPORTED

            return VersionEntry(
                id = id,
                displayName = displayName,
                sqliteDbAssetPath = sqliteDbAssetPath,
                sqlDumpAssetPath = sqlDumpAssetPath,
                policy = policy
            ).takeIf { entry ->
                entry.id.isNotEmpty() &&
                    entry.displayName.isNotEmpty() &&
                    entry.sqliteDbAssetPath.isNotEmpty() &&
                    entry.sqlDumpAssetPath.isNotEmpty()
            }
        }
    }

    fun toJsonObject(): JSONObject = JSONObject()
        .put(JSON_ID, id)
        .put(JSON_DISPLAY_NAME, displayName)
        .put(JSON_SQLITE_DB_ASSET_PATH, sqliteDbAssetPath)
        .put(JSON_SQL_DUMP_ASSET_PATH, sqlDumpAssetPath)
        .put(JSON_POLICY, policy.name)
}

private const val JSON_ID = "id"
private const val JSON_DISPLAY_NAME = "displayName"
private const val JSON_SQLITE_DB_ASSET_PATH = "sqliteDbAssetPath"
private const val JSON_SQL_DUMP_ASSET_PATH = "sqlDumpAssetPath"
private const val JSON_POLICY = "policy"
