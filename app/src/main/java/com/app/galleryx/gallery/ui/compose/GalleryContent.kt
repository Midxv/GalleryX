/*
 * Copyright 2020–2026 GalleryX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.app.galleryx.gallery.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    modifier: Modifier = Modifier
) {
    val multiSelectionState = rememberMultiSelectionState(items = uiState.photos.map { it.uuid })

    PhotoGallery(
        photos = uiState.photos,
        albumName = null,
        multiSelectionState = multiSelectionState,
        onOpenPhoto = { handleUiEvent(GalleryUiEvent.OpenPhoto(it)) },
        onExport = { handleUiEvent(GalleryUiEvent.OnExport(multiSelectionState.selectedItems.value.toList(), it)) },
        onDelete = { handleUiEvent(GalleryUiEvent.OnDelete(multiSelectionState.selectedItems.value.toList())) },
        onImportChoice = { handleUiEvent(GalleryUiEvent.OnImportChoice(it)) },
        additionalMultiSelectionActions = {},
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
                    PhotoTile("file1.jpg", PhotoType.JPEG, "1", 1024),
                    PhotoTile("file2.jpg", PhotoType.JPEG, "2", 2048),
                    PhotoTile("file3.jpg", PhotoType.JPEG, "3", 4096)
                ),
                sort = SortConfig.Gallery.default
            ),
            handleUiEvent = {}
        )
    }
}