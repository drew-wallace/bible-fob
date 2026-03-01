package com.example.biblefob.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

data class VersionOptionUiModel(
    val id: String,
    val displayName: String,
    val canRename: Boolean = false,
    val canDelete: Boolean = false
)

sealed interface HomeScreenUiState {
    data object Loading : HomeScreenUiState
    data object Empty : HomeScreenUiState
    data class InvalidReference(val message: String) : HomeScreenUiState
    data class UnsupportedVersion(
        val requestedVersion: String,
        val supportedVersions: List<VersionOptionUiModel>
    ) : HomeScreenUiState
    data class Content(val chunks: List<ReferenceChunkUiModel>) : HomeScreenUiState
}

enum class VerseDisplayMode {
    CONCATENATED,
    LINE_BY_LINE
}

@Composable
fun HomeScreen(
    parsedReferenceChunks: List<String> = emptyList(),
    uiState: HomeScreenUiState = HomeScreenUiState.Empty,
    selectedVersion: String? = null,
    supportedVersions: List<VersionOptionUiModel> = emptyList(),
    onVersionSelected: (String) -> Unit = {},
    onAddVersionClick: () -> Unit = {},
    onRenameVersion: (String, String) -> Unit = { _, _ -> },
    onDeleteVersion: (String) -> Unit = {},
    versionActionMessage: String? = null,
    isVersionActionError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var verseDisplayMode by rememberSaveable { mutableStateOf(VerseDisplayMode.CONCATENATED) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
            ) {
                SettingsDrawerContent(
                    selectedVersion = selectedVersion,
                    supportedVersions = supportedVersions,
                    onVersionSelected = onVersionSelected,
                    onAddVersionClick = onAddVersionClick,
                    onRenameVersion = onRenameVersion,
                    onDeleteVersion = onDeleteVersion,
                    versionActionMessage = versionActionMessage,
                    isVersionActionError = isVersionActionError,
                    verseDisplayMode = verseDisplayMode,
                    onVerseDisplayModeChange = { verseDisplayMode = it }
                )
            }
        },
        gesturesEnabled = true,
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing
        ) { contentPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                color = MaterialTheme.colorScheme.background
            ) {
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

                    is HomeScreenUiState.UnsupportedVersion -> CenterMessage {
                        UnsupportedVersionMessage(
                            requestedVersion = uiState.requestedVersion,
                            supportedVersions = uiState.supportedVersions,
                            onVersionSelected = onVersionSelected
                        )
                    }

                    is HomeScreenUiState.Content -> ReferenceResultsList(
                        chunks = uiState.chunks,
                        verseDisplayMode = verseDisplayMode
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDrawerContent(
    selectedVersion: String?,
    supportedVersions: List<VersionOptionUiModel>,
    onVersionSelected: (String) -> Unit,
    onAddVersionClick: () -> Unit,
    onRenameVersion: (String, String) -> Unit,
    onDeleteVersion: (String) -> Unit,
    versionActionMessage: String?,
    isVersionActionError: Boolean,
    verseDisplayMode: VerseDisplayMode,
    onVerseDisplayModeChange: (VerseDisplayMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var editingVersionId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameInput by rememberSaveable { mutableStateOf("") }
    val versionLabel = supportedVersions
        .firstOrNull { it.id == selectedVersion }
        ?.displayName
        ?: "Select version"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Version",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = onAddVersionClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Add Version")
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = versionLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                supportedVersions.forEach { version ->
                    DropdownMenuItem(
                        text = { Text(version.displayName) },
                        onClick = {
                            onVersionSelected(version.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = "Manage versions",
            style = MaterialTheme.typography.titleSmall
        )

        supportedVersions.forEach { version ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = version.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (version.id == selectedVersion) FontWeight.SemiBold else FontWeight.Normal
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (version.canRename) {
                            TextButton(onClick = {
                                editingVersionId = version.id
                                renameInput = version.displayName
                            }) {
                                Text(text = "Edit")
                            }
                        }

                        if (version.canDelete) {
                            TextButton(onClick = { onDeleteVersion(version.id) }) {
                                Text(text = "Delete")
                            }
                        } else {
                            Text(
                                text = "Bundled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (editingVersionId == version.id) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = { Text("New display name") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onRenameVersion(version.id, renameInput)
                                editingVersionId = null
                                renameInput = ""
                            },
                            enabled = renameInput.isNotBlank()
                        ) {
                            Text(text = "Save")
                        }
                    }
                }
            }
        }

        versionActionMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isVersionActionError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "Display",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Display verses line-by-line")
            Switch(
                checked = verseDisplayMode == VerseDisplayMode.LINE_BY_LINE,
                onCheckedChange = { isChecked ->
                    onVerseDisplayModeChange(
                        if (isChecked) VerseDisplayMode.LINE_BY_LINE else VerseDisplayMode.CONCATENATED
                    )
                }
            )
        }
    }
}

@Composable
private fun UnsupportedVersionMessage(
    requestedVersion: String,
    supportedVersions: List<VersionOptionUiModel>,
    onVersionSelected: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Version $requestedVersion is not supported.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Choose one of the supported versions:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        supportedVersions.forEach { version ->
            Button(onClick = { onVersionSelected(version.id) }) {
                Text(text = version.displayName)
            }
        }
    }
}

@Composable
private fun ReferenceResultsList(
    chunks: List<ReferenceChunkUiModel>,
    verseDisplayMode: VerseDisplayMode
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(chunks) { chunk ->
            ReferenceChunkCard(chunk = chunk, verseDisplayMode = verseDisplayMode)
        }
    }
}

@Composable
private fun ReferenceChunkCard(
    chunk: ReferenceChunkUiModel,
    verseDisplayMode: VerseDisplayMode
) {
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
                val versesWithText = chunk.verses.filter { verse -> verse.text.isNotBlank() }

                if (versesWithText.isEmpty()) {
                    Text(
                        text = "No verses available for this reference.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    when (verseDisplayMode) {
                        VerseDisplayMode.LINE_BY_LINE -> {
                            versesWithText.forEach { verse ->
                                Text(
                                    text = "${verse.number}. ${verse.text}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        VerseDisplayMode.CONCATENATED -> {
                            Text(
                                text = versesWithText.joinToString(" ") { verse ->
                                    "${verse.number}. ${verse.text}"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
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
            selectedVersion = "NET",
            supportedVersions = listOf("KJV", "ASV", "WEB", "YLT", "NET").map { VersionOptionUiModel(it, it) },
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
            selectedVersion = "ASV",
            supportedVersions = listOf("KJV", "ASV", "WEB", "YLT").map { VersionOptionUiModel(it, it) },
            uiState = HomeScreenUiState.Loading
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 640,
    heightDp = 360,
    name = "Home - Landscape"
)
@Composable
private fun HomeScreenLandscapePreview() {
    BibleFobTheme(darkTheme = false) {
        HomeScreen(
            parsedReferenceChunks = listOf("John 3:16-17"),
            selectedVersion = "WEB",
            supportedVersions = listOf("KJV", "ASV", "WEB", "YLT", "NET").map { VersionOptionUiModel(it, it) },
            uiState = HomeScreenUiState.Content(
                chunks = listOf(
                    ReferenceChunkUiModel(
                        normalizedReference = "John 3:16-17",
                        version = "WEB",
                        verses = listOf(
                            VerseUiModel(16, "For God so loved the world, that he gave his one and only Son..."),
                            VerseUiModel(17, "For God didn't send his Son into the world to judge the world...")
                        )
                    )
                )
            )
        )
    }
}
