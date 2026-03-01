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

        (VersionCatalogRepository.builtInEntries + userEntries)
            .distinctBy(VersionEntry::id)
            .sortedBy { entry -> entry.displayName }
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
