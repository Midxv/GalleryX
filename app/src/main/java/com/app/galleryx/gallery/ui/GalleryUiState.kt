/*
 * Copyright 2020–2026 GalleryX
 */

package com.app.galleryx.gallery.ui

import com.app.galleryx.gallery.components.PhotoTile
import com.app.galleryx.sort.domain.Sort

sealed interface GalleryUiState {
    val searchQuery: String
    val totalCount: Int
    val indexedCount: Int

    data class Empty(
        override val searchQuery: String = "",
        override val totalCount: Int = 0,
        override val indexedCount: Int = 0
    ) : GalleryUiState

    data class Content(
        val photos: List<PhotoTile>,
        val sort: Sort,
        override val searchQuery: String = "",
        override val totalCount: Int = 0,
        override val indexedCount: Int = 0
    ) : GalleryUiState
}