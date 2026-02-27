package com.example.biblefob.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.biblefob.data.AssetBibleRepositoryFactory
import com.example.biblefob.data.BibleRepository
import com.example.biblefob.parsing.SearchQueryParser
import com.example.biblefob.ui.HomeScreen
import com.example.biblefob.ui.HomeScreenUiState
import com.example.biblefob.ui.ReferenceChunkUiModel
import com.example.biblefob.ui.VerseUiModel

@Composable
fun BibleFobApp(
    deepLinkUriString: String? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val parsedReferences = remember(deepLinkUriString) {
        SearchQueryParser().parseFromUriString(deepLinkUriString)
    }

    val selectedVersion = remember(deepLinkUriString) {
        deepLinkUriString
            ?.let(Uri::parse)
            ?.getQueryParameter(VERSION_PARAM)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_VERSION
    }

    val hasSearchQuery = remember(deepLinkUriString) {
        deepLinkUriString
            ?.let(Uri::parse)
            ?.getQueryParameter(SEARCH_PARAM)
            ?.isNotBlank()
            ?: false
    }

    val uiState = remember(parsedReferences, selectedVersion, hasSearchQuery) {
        when {
            parsedReferences.isEmpty() && hasSearchQuery -> {
                HomeScreenUiState.InvalidReference(
                    message = "Unable to parse the requested reference(s)."
                )
            }

            parsedReferences.isEmpty() -> HomeScreenUiState.Empty
            else -> {
                val repository = buildRepository(context = context, version = selectedVersion)
                val chunks = parsedReferences.map { reference ->
                    val verses = repository.getVerses(reference).map { verse ->
                        VerseUiModel(number = verse.number, text = verse.text)
                    }
                    ReferenceChunkUiModel(
                        normalizedReference = reference,
                        version = selectedVersion,
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
                uiState = uiState
            )
        }
    }
}

private fun buildRepository(context: android.content.Context, version: String): BibleRepository {
    return AssetBibleRepositoryFactory.createForVersion(
        context = context,
        version = version
    )
}

private const val HOME_ROUTE = "home"
private const val SEARCH_PARAM = "search"
private const val VERSION_PARAM = "version"
private const val DEFAULT_VERSION = "KJV"
