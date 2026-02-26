package com.example.biblefob.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.biblefob.ui.theme.BibleFobTheme

data class VerseUiModel(
    val number: Int,
    val text: String
)

data class ReferenceChunkUiModel(
    val normalizedReference: String,
    val version: String? = null,
    val verses: List<VerseUiModel>
)

sealed interface HomeScreenUiState {
    data object Loading : HomeScreenUiState
    data object Empty : HomeScreenUiState
    data class InvalidReference(val message: String) : HomeScreenUiState
    data class Content(val chunks: List<ReferenceChunkUiModel>) : HomeScreenUiState
}

@Composable
fun HomeScreen(
    parsedReferenceChunks: List<String> = emptyList(),
    uiState: HomeScreenUiState = HomeScreenUiState.Empty,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            HomeScreenUiState.Loading -> CenterMessage {
                CircularProgressIndicator()
            }

            HomeScreenUiState.Empty -> {
                val message = if (parsedReferenceChunks.isEmpty()) {
                    "No references found."
                } else {
                    "No verses found for ${parsedReferenceChunks.size} parsed reference(s)."
                }
                CenterMessage {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is HomeScreenUiState.InvalidReference -> CenterMessage {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            is HomeScreenUiState.Content -> ReferenceResultsList(chunks = uiState.chunks)
        }
    }
}

@Composable
private fun ReferenceResultsList(chunks: List<ReferenceChunkUiModel>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(chunks) { chunk ->
            ReferenceChunkCard(chunk = chunk)
        }
    }
}

@Composable
private fun ReferenceChunkCard(chunk: ReferenceChunkUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = formatReferenceHeader(chunk.normalizedReference, chunk.version),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chunk.verses.forEach { verse ->
                    Text(
                        text = "${verse.number}. ${verse.text}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun formatReferenceHeader(normalizedReference: String, version: String?): String {
    return if (version.isNullOrBlank()) {
        normalizedReference
    } else {
        "$normalizedReference ($version)"
    }
}

@Composable
private fun CenterMessage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Preview(showBackground = true, name = "Home - Light")
@Composable
private fun HomeScreenLightPreview() {
    BibleFobTheme(darkTheme = false) {
        HomeScreen(
            parsedReferenceChunks = listOf("John 3:16-17", "Psalm 23:1-2"),
            uiState = HomeScreenUiState.Content(
                chunks = listOf(
                    ReferenceChunkUiModel(
                        normalizedReference = "John 3:16-17",
                        version = "NET",
                        verses = listOf(
                            VerseUiModel(16, "For this is the way God loved the world..."),
                            VerseUiModel(17, "For God did not send his Son into the world to condemn...")
                        )
                    ),
                    ReferenceChunkUiModel(
                        normalizedReference = "Psalm 23:1-2",
                        verses = listOf(
                            VerseUiModel(1, "The Lord is my shepherd, I lack nothing."),
                            VerseUiModel(2, "He makes me lie down in green pastures...")
                        )
                    )
                )
            )
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Home - Dark"
)
@Composable
private fun HomeScreenDarkPreview() {
    BibleFobTheme(darkTheme = true) {
        HomeScreen(
            parsedReferenceChunks = listOf("John 3:16-17"),
            uiState = HomeScreenUiState.Loading
        )
    }
}
