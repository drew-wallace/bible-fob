package com.example.biblefob.domain

import android.content.Context
import android.net.Uri
import com.example.biblefob.data.VersionCatalogRepository
import com.example.biblefob.data.VersionEntry
import com.example.biblefob.data.VersionManagementPolicy
import com.example.biblefob.data.importer.ConflictingImportException
import com.example.biblefob.data.importer.DuplicateImportException
import com.example.biblefob.data.importer.ImportException
import com.example.biblefob.data.importer.VersionJsonImporter
import kotlinx.coroutines.flow.first

class ImportVersionFromJsonUseCase(
    private val context: Context,
    private val versionCatalogRepository: VersionCatalogRepository,
    private val importer: VersionJsonImporter = VersionJsonImporter(context)
) {
    suspend operator fun invoke(uri: Uri, defaultDisplayName: String): Result<VersionEntry> {
        val entry = runCatching { buildEntry(defaultDisplayName = defaultDisplayName) }
            .getOrElse { return Result.failure(IllegalArgumentException("Unable to import selected version.")) }

        val existingEntries = versionCatalogRepository.versionEntries.first()
        val conflictingEntry = existingEntries.firstOrNull { it.id == entry.id }
        if (conflictingEntry != null) {
            return Result.failure(
                IllegalArgumentException(
                    "A version with id ${entry.id} already exists. Rename the imported version before trying again."
                )
            )
        }

        val importResult = try {
            importer.import(uri = uri, versionId = entry.id)
        } catch (error: DuplicateImportException) {
            return Result.failure(IllegalArgumentException(error.message ?: "Version already imported."))
        } catch (error: ConflictingImportException) {
            return Result.failure(IllegalArgumentException(error.message ?: "Conflicting version import."))
        } catch (error: ImportException) {
            return Result.failure(IllegalArgumentException(error.message ?: "Unable to parse imported JSON."))
        }

        val importedEntry = entry.copy(
            sqliteDbAssetPath = importResult.sqlDumpFile.absolutePath,
            sqlDumpAssetPath = importResult.sqlDumpFile.absolutePath
        )

        versionCatalogRepository.upsertUserEntry(importedEntry)
        return Result.success(importedEntry)
    }

    private fun buildEntry(defaultDisplayName: String): VersionEntry {
        val displayName = defaultDisplayName.trim()
        val id = displayName.toVersionId().ifBlank { "IMPORTED" }

        require(displayName.isNotBlank())
        require(id.isNotBlank())

        return VersionEntry(
            id = id,
            displayName = displayName,
            sqliteDbAssetPath = id,
            sqlDumpAssetPath = id,
            policy = VersionManagementPolicy.USER_IMPORTED
        )
    }
}

private fun String.toVersionId(): String {
    return uppercase()
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
}
