package com.example.biblefob.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.biblefob.data.VersionCatalogDataStoreRepository
import com.example.biblefob.data.VersionCatalogRepository
import com.example.biblefob.data.VersionEntry
import com.example.biblefob.parsing.SearchQueryParser
import com.example.biblefob.ui.HomeScreen
import com.example.biblefob.ui.HomeScreenUiState
import com.example.biblefob.ui.ReferenceChunkUiModel
import com.example.biblefob.ui.VersionOptionUiModel
import com.example.biblefob.ui.VerseUiModel

@Composable
fun BibleFobApp(
    deepLinkUriString: String? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val versionCatalogRepository = remember(context) { VersionCatalogDataStoreRepository(context) }
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
        versionEntries.map { entry -> VersionOptionUiModel(id = entry.id, displayName = entry.displayName) }
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
                }
            )
        }
    }
}

private fun buildRepository(context: android.content.Context, version: VersionEntry): BibleRepository {
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
