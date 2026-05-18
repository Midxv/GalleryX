/*
 * Copyright 2020–2026 GalleryX
 */

package com.app.galleryx.gallery.ui

import android.net.Uri
import com.app.galleryx.gallery.components.ImportChoice
import com.app.galleryx.gallery.components.PhotoTile
import com.app.galleryx.sort.domain.Sort

sealed interface GalleryUiEvent {
    data class OpenPhoto(val item: PhotoTile) : GalleryUiEvent
    data class OnDelete(val items: List<String>) : GalleryUiEvent
    data class OnExport(val items: List<String>, val target: Uri?) : GalleryUiEvent

    // --- THIS IS THE MISSING TRIGGER ---
    data class OnShare(val items: List<String>) : GalleryUiEvent

    data class OnImportChoice(val choice: ImportChoice) : GalleryUiEvent

    data class SortChanged(val sort: Sort) : GalleryUiEvent

    // Upgraded to data object for better Compose state matching
    data object OnAddToAlbum : GalleryUiEvent
    data class OnAlbumSelected(val albumId: String) : GalleryUiEvent
    data object CancelAlbumSelection : GalleryUiEvent

    // Move to album event
    data class MoveToAlbum(val photoUuids: List<String>, val albumId: String) : GalleryUiEvent
}