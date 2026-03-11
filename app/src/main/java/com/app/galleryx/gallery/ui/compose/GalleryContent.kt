/*
 * Copyright 2020–2026 GalleryX
 */

package com.app.galleryx.gallery.ui.compose

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
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
    onMoveToAlbumClicked: (List<String>) -> Unit, // Callback for moving photos
    modifier: Modifier = Modifier
) {
    val multiSelectionState = rememberMultiSelectionState(items = uiState.photos.map { it.uuid })

    PhotoGallery(
        photos = uiState.photos,
        albumName = null,
        showImportButton = false, // Hides the Import Fab in All Files
        multiSelectionState = multiSelectionState,
        onOpenPhoto = { handleUiEvent(GalleryUiEvent.OpenPhoto(it)) },
        // FIXED: Correctly matching your original OnExport and OnDelete parameters
        onExport = { targetUri ->
            handleUiEvent(GalleryUiEvent.OnExport(multiSelectionState.selectedItems.value.toList(), targetUri))
        },
        onDelete = {
            handleUiEvent(GalleryUiEvent.OnDelete(multiSelectionState.selectedItems.value.toList()))
        },
        onImportChoice = { handleUiEvent(GalleryUiEvent.OnImportChoice(it)) },
        additionalMultiSelectionActions = {
            // Added the Move to Album button to the 3-dots menu
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
        modifier = modifier
    )
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
                sort = SortConfig.Gallery.default
            ),
            handleUiEvent = {},
            onMoveToAlbumClicked = {}
        )
    }
}