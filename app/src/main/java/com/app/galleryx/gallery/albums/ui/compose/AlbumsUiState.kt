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

package com.app.galleryx.gallery.albums.ui.compose

sealed interface AlbumsUiState {
    val showCreateDialog: Boolean
    val searchQuery: String

    data class Empty(
        override val showCreateDialog: Boolean = false,
        override val searchQuery: String = ""
    ) : AlbumsUiState

    data class Content(
        val albums: List<AlbumItem>,
        override val showCreateDialog: Boolean = false,
        override val searchQuery: String = ""
    ) : AlbumsUiState
}

data class AlbumItem(
    val id: String,
    val name: String,
    val itemCount: Int,
    val albumCover: AlbumCover? = null,
)

data class AlbumCover(
    val filename: String,
    val mimeType: String,
)