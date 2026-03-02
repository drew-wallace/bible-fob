package com.example.biblefob.versionmanagement

import android.content.Context
import com.example.biblefob.data.VersionCatalogRepository
import com.example.biblefob.data.VersionEntry
import kotlinx.coroutines.flow.first
import java.io.File

class VersionManagementService(
    private val context: Context,
    private val versionCatalogRepository: VersionCatalogRepository
) {
    suspend fun renameVersion(versionId: String, newDisplayName: String): RenameVersionResult {
        val normalizedVersionId = versionId.trim().uppercase()
        val displayName = newDisplayName.trim()
        if (displayName.isBlank()) {
            return RenameVersionResult.Failure("Version name cannot be blank.")
        }

        val entries = versionCatalogRepository.versionEntries.first()
        val existingEntry = entries.firstOrNull { it.id == normalizedVersionId }
            ?: return RenameVersionResult.Failure("Version $normalizedVersionId was not found.")

        if (!existingEntry.policy.canRename) {
            return RenameVersionResult.Failure("Version $normalizedVersionId cannot be renamed.")
        }

        val renamedVersionId = displayName.toVersionId().ifBlank { normalizedVersionId }
        val conflict = entries.any { it.id == renamedVersionId && it.id != existingEntry.id }
        if (conflict) {
            return RenameVersionResult.Failure("A version with id $renamedVersionId already exists.")
        }

        val renamedEntry = existingEntry.copy(
            id = renamedVersionId,
            displayName = displayName,
            sqliteDbAssetPath = renameMappedFilePath(
                existingEntry.sqliteDbAssetPath,
                oldVersionId = existingEntry.id,
                newVersionId = renamedVersionId
            ),
            sqlDumpAssetPath = renameMappedFilePath(
                existingEntry.sqlDumpAssetPath,
                oldVersionId = existingEntry.id,
                newVersionId = renamedVersionId
            )
        )

        versionCatalogRepository.upsertUserEntry(renamedEntry)
        if (renamedEntry.id != existingEntry.id) {
            versionCatalogRepository.deleteUserEntry(existingEntry.id)
        }

        return RenameVersionResult.Success(
            oldVersionId = existingEntry.id,
            renamedEntry = renamedEntry
        )
    }

    suspend fun deleteVersion(versionId: String, activeVersionId: String?): DeleteVersionResult {
        val normalizedVersionId = versionId.trim().uppercase()
        val entries = versionCatalogRepository.versionEntries.first()
        val existingEntry = entries.firstOrNull { it.id == normalizedVersionId }
            ?: return DeleteVersionResult.Failure("Version $normalizedVersionId was not found.")

        if (!existingEntry.policy.canDelete) {
            return DeleteVersionResult.Failure("Version $normalizedVersionId cannot be deleted.")
        }

        deleteManagedFileIfPresent(existingEntry.sqliteDbAssetPath)
        deleteManagedFileIfPresent(existingEntry.sqlDumpAssetPath)

        val deleted = versionCatalogRepository.deleteUserEntry(existingEntry.id)
        if (!deleted) {
            return DeleteVersionResult.Failure("Unable to delete $normalizedVersionId.")
        }

        val fallbackSelectedVersion = if (activeVersionId?.trim()?.uppercase() == existingEntry.id) {
            VersionCatalogRepository.defaultVersionId
        } else {
            activeVersionId
        }

        return DeleteVersionResult.Success(
            deletedVersionId = existingEntry.id,
            fallbackSelectedVersion = fallbackSelectedVersion
        )
    }

    private fun renameMappedFilePath(path: String, oldVersionId: String, newVersionId: String): String {
        if (path.startsWith(CONTENT_URI_PREFIX)) return path

        val currentFile = File(path)
        if (!currentFile.exists()) return path
        if (!isManagedFile(currentFile)) return path

        val renamedFileName = currentFile.name.replace(oldVersionId, newVersionId, ignoreCase = true)
        if (renamedFileName == currentFile.name) return path

        val renamedFile = File(currentFile.parentFile, renamedFileName)
        return if (currentFile.renameTo(renamedFile)) renamedFile.absolutePath else path
    }

    private fun deleteManagedFileIfPresent(path: String) {
        if (path.startsWith(CONTENT_URI_PREFIX)) return

        val file = File(path)
        if (!file.exists()) return
        if (!isManagedFile(file)) return

        file.delete()
    }

    private fun isManagedFile(file: File): Boolean {
        val absolutePath = file.absolutePath
        return absolutePath.startsWith(context.filesDir.absolutePath) ||
            absolutePath.startsWith(context.cacheDir.absolutePath)
    }
}

sealed interface RenameVersionResult {
    data class Success(
        val oldVersionId: String,
        val renamedEntry: VersionEntry
    ) : RenameVersionResult

    data class Failure(val message: String) : RenameVersionResult
}

sealed interface DeleteVersionResult {
    data class Success(
        val deletedVersionId: String,
        val fallbackSelectedVersion: String?
    ) : DeleteVersionResult

    data class Failure(val message: String) : DeleteVersionResult
}

private fun String.toVersionId(): String {
    return uppercase()
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
}

private const val CONTENT_URI_PREFIX = "content://"
