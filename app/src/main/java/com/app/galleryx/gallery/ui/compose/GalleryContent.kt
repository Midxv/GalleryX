/*
 * Copyright 2020–2026 GalleryX
 */

package com.app.galleryx.gallery.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.galleryx.R
import com.app.galleryx.gallery.components.PhotoGallery
import com.app.galleryx.gallery.components.PhotoTile
import com.app.galleryx.gallery.components.rememberMultiSelectionState
import com.app.galleryx.gallery.ui.GalleryUiEvent
import com.app.galleryx.gallery.ui.GalleryUiState
import com.app.galleryx.model.database.entity.PhotoType
import com.app.galleryx.ui.theme.AppTheme
import com.app.galleryx.sort.domain.SortConfig

@Composable
fun GalleryContent(
    uiState: GalleryUiState.Content,
    handleUiEvent: (GalleryUiEvent) -> Unit,
    onMoveToAlbumClicked: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val multiSelectionState = rememberMultiSelectionState(items = uiState.photos.map { it.uuid })

    Column(modifier = modifier.fillMaxSize()) {

        // --- 1. THE AI INDEXING PROGRESS BAR ---
        if (uiState.totalCount > 0 && uiState.indexedCount < uiState.totalCount) {
            val progress = uiState.indexedCount.toFloat() / uiState.totalCount.toFloat()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "AI Indexing Vault: ${uiState.indexedCount} / ${uiState.totalCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
        }

        // --- 2. THE SEARCH RESULTS HEADER ---
        if (uiState.searchQuery.isNotBlank()) {
            Text(
                text = "AI found ${uiState.photos.size} photos for '${uiState.searchQuery}'",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        // --- 3. THE MAIN GALLERY GRID ---
        PhotoGallery(
            photos = uiState.photos,
            albumName = null,
            showImportButton = false, // Hides the Import Fab in All Files
            multiSelectionState = multiSelectionState,
            onOpenPhoto = { handleUiEvent(GalleryUiEvent.OpenPhoto(it)) },
            onExport = { targetUri ->
                handleUiEvent(GalleryUiEvent.OnExport(multiSelectionState.selectedItems.value.toList(), targetUri))
            },
            onDelete = {
                handleUiEvent(GalleryUiEvent.OnDelete(multiSelectionState.selectedItems.value.toList()))
            },

            // --- NEW: Handle the Share button click ---
            onShare = {
                handleUiEvent(GalleryUiEvent.OnShare(multiSelectionState.selectedItems.value.toList()))
            },

            onImportChoice = { handleUiEvent(GalleryUiEvent.OnImportChoice(it)) },
            additionalMultiSelectionActions = {
                DropdownMenuItem(
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_folder), contentDescription = null) },
                    text = { Text("Move to Album") },
                    onClick = {
                        onMoveToAlbumClicked(multiSelectionState.selectedItems.value.toList())
                        multiSelectionState.dismissMore()
                        multiSelectionState.cancelSelection()
                    },
                )
            },
            modifier = Modifier.weight(1f) // CRITICAL: Tells the grid to take up the rest of the screen
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GalleryContentPreview() {
    AppTheme {
        GalleryContent(
            uiState = GalleryUiState.Content(
                photos = listOf(
                    PhotoTile("file1.jpg", PhotoType.JPEG, "1", 1024, System.currentTimeMillis()),
                    PhotoTile("file2.jpg", PhotoType.JPEG, "2", 2048, System.currentTimeMillis())
                ),
                sort = SortConfig.Gallery.default,
                searchQuery = "Mountain",
                totalCount = 100,
                indexedCount = 45
            ),
            handleUiEvent = {},
            onMoveToAlbumClicked = {}
        )
    }
}