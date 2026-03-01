package com.example.biblefob.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.versionDataStore: DataStore<Preferences> by preferencesDataStore(name = "version_catalog")

class VersionCatalogDataStoreRepository(
    private val context: Context
) : VersionCatalogRepository {

    override val versionEntries: Flow<List<VersionEntry>> = context.versionDataStore.data.map { preferences ->
        val rawJson = preferences[USER_ENTRIES_JSON_KEY].orEmpty()
        val userEntries = parseEntries(rawJson)

        mergeVersionEntries(
            builtInEntries = VersionCatalogRepository.builtInEntries,
            userEntries = userEntries
        )
    }

    override suspend fun upsertUserEntry(entry: VersionEntry) {
        context.versionDataStore.edit { preferences ->
            val existingEntries = parseEntries(preferences[USER_ENTRIES_JSON_KEY].orEmpty())
                .filterNot { it.id == entry.id }
            val updatedEntries = (existingEntries + entry)
                .sortedBy(VersionEntry::displayName)

            preferences[USER_ENTRIES_JSON_KEY] = JSONArray()
                .also { jsonArray ->
                    updatedEntries.forEach { item ->
                        jsonArray.put(item.toJsonObject())
                    }
                }
                .toString()
        }
    }

    override suspend fun renameUserEntry(id: String, displayName: String): Boolean {
        if (displayName.isBlank()) return false

        var renamed = false
        context.versionDataStore.edit { preferences ->
            val updatedEntries = parseEntries(preferences[USER_ENTRIES_JSON_KEY].orEmpty())
                .map { entry ->
                    if (entry.id == id) {
                        renamed = true
                        entry.copy(displayName = displayName.trim())
                    } else {
                        entry
                    }
                }
                .sortedBy(VersionEntry::displayName)

            preferences[USER_ENTRIES_JSON_KEY] = JSONArray()
                .also { jsonArray ->
                    updatedEntries.forEach { item ->
                        jsonArray.put(item.toJsonObject())
                    }
                }
                .toString()
        }

        return renamed
    }

    override suspend fun deleteUserEntry(id: String): Boolean {
        var deleted = false

        context.versionDataStore.edit { preferences ->
            val currentEntries = parseEntries(preferences[USER_ENTRIES_JSON_KEY].orEmpty())
            val updatedEntries = currentEntries.filterNot { entry ->
                val shouldDelete = entry.id == id
                if (shouldDelete) {
                    deleted = true
                }
                shouldDelete
            }

            preferences[USER_ENTRIES_JSON_KEY] = JSONArray()
                .also { jsonArray ->
                    updatedEntries.forEach { item ->
                        jsonArray.put(item.toJsonObject())
                    }
                }
                .toString()
        }

        return deleted
    }

    private fun parseEntries(rawJson: String): List<VersionEntry> {
        if (rawJson.isBlank()) return emptyList()

        return runCatching {
            val jsonArray = JSONArray(rawJson)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    VersionEntry.fromJsonObject(item)?.let(::add)
                }
            }
        }.getOrElse { emptyList() }
    }
}

private val USER_ENTRIES_JSON_KEY = stringPreferencesKey("user_version_entries_json")

internal fun mergeVersionEntries(
    builtInEntries: List<VersionEntry>,
    userEntries: List<VersionEntry>
): List<VersionEntry> {
    val mergedById = linkedMapOf<String, VersionEntry>()
    builtInEntries.forEach { entry ->
        mergedById[entry.id] = entry
    }
    userEntries.forEach { entry ->
        mergedById[entry.id] = entry
    }

    return mergedById.values
        .sortedBy(VersionEntry::displayName)
}
