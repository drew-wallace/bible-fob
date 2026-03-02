package com.example.biblefob.navigation

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.biblefob.data.AssetBibleRepositoryFactory
import com.example.biblefob.data.BibleRepository
import com.example.biblefob.data.JsonBibleRepository
import com.example.biblefob.data.VersionCatalogDataStoreRepository
import com.example.biblefob.data.VersionCatalogRepository
import com.example.biblefob.data.VersionEntry
import com.example.biblefob.domain.ImportVersionFromJsonUseCase
import com.example.biblefob.parsing.SearchQueryParser
import com.example.biblefob.ui.HomeScreen
import com.example.biblefob.ui.HomeScreenUiState
import com.example.biblefob.ui.ReferenceChunkUiModel
import com.example.biblefob.ui.VersionOptionUiModel
import com.example.biblefob.ui.VerseUiModel
import com.example.biblefob.versionmanagement.DeleteVersionResult
import com.example.biblefob.versionmanagement.RenameVersionResult
import com.example.biblefob.versionmanagement.VersionManagementService
import kotlinx.coroutines.launch

@Composable
fun BibleFobApp(
    deepLinkUriString: String? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val versionCatalogRepository = remember(context) { VersionCatalogDataStoreRepository(context) }
    val importVersionFromJson = remember(context, versionCatalogRepository) {
        ImportVersionFromJsonUseCase(
            context = context,
            versionCatalogRepository = versionCatalogRepository
        )
    }
    val versionManagementService = remember(context, versionCatalogRepository) {
        VersionManagementService(
            context = context,
            versionCatalogRepository = versionCatalogRepository
        )
    }
    var versionActionMessage by remember { mutableStateOf<String?>(null) }
    var isVersionActionError by remember { mutableStateOf(false) }
    val versionEntries by versionCatalogRepository.versionEntries.collectAsState(
        initial = VersionCatalogRepository.builtInEntries
    )

    val parsedReferences = remember(deepLinkUriString) {
        SearchQueryParser().parseFromUriString(deepLinkUriString)
    }

    val requestedVersion = remember(deepLinkUriString) {
        deepLinkUriString
            ?.let(Uri::parse)
            ?.getQueryParameter(VERSION_PARAM)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: VersionCatalogRepository.defaultVersionId
    }

    var selectedVersion by remember(requestedVersion) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(requestedVersion, versionEntries) {
        if (selectedVersion == null) {
            selectedVersion = normalizeVersionOrNull(
                version = requestedVersion,
                supportedVersions = versionEntries
            )
        }
    }

    val hasSearchQuery = remember(deepLinkUriString) {
        deepLinkUriString
            ?.let(Uri::parse)
            ?.getQueryParameter(SEARCH_PARAM)
            ?.isNotBlank()
            ?: false
    }

    val supportedVersionOptions = remember(versionEntries) {
        versionEntries.map { entry ->
            VersionOptionUiModel(
                id = entry.id,
                displayName = entry.displayName,
                canRename = entry.policy.canRename,
                canDelete = entry.policy.canDelete
            )
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri == null) {
            versionActionMessage = "Import canceled."
            isVersionActionError = true
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val defaultDisplayName = deriveDefaultDisplayName(context = context, uri = selectedUri)
            val importResult = importVersionFromJson(
                uri = selectedUri,
                defaultDisplayName = defaultDisplayName
            )

            importResult
                .onSuccess { entry ->
                    selectedVersion = entry.id
                    versionActionMessage = "Imported ${entry.displayName}."
                    isVersionActionError = false
                }
                .onFailure { throwable ->
                    versionActionMessage = throwable.message ?: "Unable to import selected version."
                    isVersionActionError = true
                }
        }
    }

    val uiState = remember(parsedReferences, selectedVersion, hasSearchQuery, requestedVersion, versionEntries) {
        when {
            selectedVersion == null -> {
                HomeScreenUiState.UnsupportedVersion(
                    requestedVersion = requestedVersion,
                    supportedVersions = supportedVersionOptions
                )
            }

            parsedReferences.isEmpty() && hasSearchQuery -> {
                HomeScreenUiState.InvalidReference(
                    message = "Unable to parse the requested reference(s)."
                )
            }

            parsedReferences.isEmpty() -> HomeScreenUiState.Empty
            else -> {
                val selectedEntry = versionEntries.firstOrNull { it.id == selectedVersion }
                val repository = buildRepository(context = context, version = selectedEntry ?: defaultVersionEntry())
                val chunks = parsedReferences.map { reference ->
                    val verses = repository.getVerses(reference).map { verse ->
                        VerseUiModel(number = verse.number, text = verse.text)
                    }
                    ReferenceChunkUiModel(
                        normalizedReference = reference,
                        version = selectedEntry?.id ?: VersionCatalogRepository.defaultVersionId,
                        verses = verses
                    )
                }

                if (chunks.all { it.verses.isEmpty() }) {
                    HomeScreenUiState.Empty
                } else {
                    HomeScreenUiState.Content(chunks)
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(
                parsedReferenceChunks = parsedReferences,
                uiState = uiState,
                selectedVersion = selectedVersion,
                supportedVersions = supportedVersionOptions,
                onVersionSelected = {
                    selectedVersion = normalizeVersionOrNull(
                        version = it,
                        supportedVersions = versionEntries
                    )
                },
                onAddVersionClick = {
                    importDocumentLauncher.launch(arrayOf("application/json"))
                },
                onRenameVersion = { versionId, displayName ->
                    coroutineScope.launch {
                        when (val result = versionManagementService.renameVersion(versionId, displayName)) {
                            is RenameVersionResult.Success -> {
                                if (selectedVersion == result.oldVersionId) {
                                    selectedVersion = result.renamedEntry.id
                                }
                                versionActionMessage = "Renamed ${result.oldVersionId} to ${result.renamedEntry.displayName}."
                                isVersionActionError = false
                            }

                            is RenameVersionResult.Failure -> {
                                versionActionMessage = result.message
                                isVersionActionError = true
                            }
                        }
                    }
                },
                onDeleteVersion = { versionId ->
                    coroutineScope.launch {
                        when (val result = versionManagementService.deleteVersion(versionId, selectedVersion)) {
                            is DeleteVersionResult.Success -> {
                                selectedVersion = result.fallbackSelectedVersion
                                versionActionMessage = "Deleted ${result.deletedVersionId}."
                                isVersionActionError = false
                            }

                            is DeleteVersionResult.Failure -> {
                                versionActionMessage = result.message
                                isVersionActionError = true
                            }
                        }
                    }
                },
                versionActionMessage = versionActionMessage,
                isVersionActionError = isVersionActionError
            )
        }
    }
}

private fun deriveDefaultDisplayName(context: android.content.Context, uri: Uri): String {
    val fileName = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        } else {
            null
        }
    } ?: uri.lastPathSegment.orEmpty()

    return fileName
        .removeSuffix(".json")
        .removeSuffix(".JSON")
        .ifBlank { "Imported Version" }
}

private fun buildRepository(context: android.content.Context, version: VersionEntry): BibleRepository {
    if (version.sqliteDbAssetPath.startsWith(CONTENT_URI_PREFIX)) {
        val wholeBibleJson = runCatching {
            context.contentResolver
                .openInputStream(Uri.parse(version.sqliteDbAssetPath))
                ?.bufferedReader()
                ?.use { it.readText() }
        }.getOrNull()

        return JsonBibleRepository(wholeBibleJson = wholeBibleJson)
    }

    return AssetBibleRepositoryFactory.createForVersionEntry(
        context = context,
        entry = version
    )
}

private fun normalizeVersionOrNull(version: String, supportedVersions: List<VersionEntry>): String? {
    val normalizedVersion = version.trim().uppercase()
    return supportedVersions.firstOrNull { it.id == normalizedVersion }?.id
}

private fun defaultVersionEntry(): VersionEntry {
    return VersionCatalogRepository.builtInEntries
        .firstOrNull { it.id == VersionCatalogRepository.defaultVersionId }
        ?: VersionCatalogRepository.builtInEntries.first()
}

private const val HOME_ROUTE = "home"
private const val SEARCH_PARAM = "search"
private const val VERSION_PARAM = "version"
private const val CONTENT_URI_PREFIX = "content://"
