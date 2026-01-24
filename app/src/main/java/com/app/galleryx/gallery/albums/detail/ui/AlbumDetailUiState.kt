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

package com.app.galleryx.gallery.albums.detail.ui

import com.app.galleryx.gallery.components.PhotoTile
import com.app.galleryx.sort.domain.Sort
import com.app.galleryx.sort.domain.SortConfig

data class AlbumDetailUiState(
    val albumId: String = "",
    val albumName: String = "",
    val photos: List<PhotoTile> = emptyList(),
    val sort: Sort = SortConfig.Album.default,
    val searchQuery: String = ""
)