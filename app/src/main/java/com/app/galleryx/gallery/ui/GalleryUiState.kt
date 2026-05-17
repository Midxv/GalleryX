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

package com.app.galleryx.gallery.ui

import com.app.galleryx.gallery.components.PhotoTile
import com.app.galleryx.sort.domain.Sort
import com.app.galleryx.sort.domain.SortConfig

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
        val sort: Sort = SortConfig.Gallery.default,
        override val searchQuery: String = "",
        override val totalCount: Int = 0,
        override val indexedCount: Int = 0
    ) : GalleryUiState
}