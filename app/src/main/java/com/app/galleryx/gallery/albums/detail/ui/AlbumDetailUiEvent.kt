/*
 * Copyright 2020–2026 Leon Latsch
 */

package com.app.galleryx.gallery.albums.detail.ui

import android.net.Uri
import com.app.galleryx.sort.domain.Sort
import com.app.galleryx.gallery.components.ImportChoice
import com.app.galleryx.gallery.components.PhotoTile

sealed interface AlbumDetailUiEvent {
    data class OpenPhoto(val item: PhotoTile) : AlbumDetailUiEvent
    data class OnDelete(val items: List<String>) : AlbumDetailUiEvent
    data class OnExport(val items: List<String>, val target: Uri?) : AlbumDetailUiEvent

    data class MoveToAlbum(val items: List<String>, val targetAlbumUuid: String) : AlbumDetailUiEvent

    data object DeleteAlbum : AlbumDetailUiEvent
    data class RenameAlbum(val newName: String) : AlbumDetailUiEvent
    data object HideAlbum : AlbumDetailUiEvent

    data class OnImportChoice(val choice: ImportChoice) : AlbumDetailUiEvent
    data class SortChanged(val sort: Sort) : AlbumDetailUiEvent
}