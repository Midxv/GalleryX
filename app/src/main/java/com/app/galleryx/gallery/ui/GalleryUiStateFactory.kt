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
import com.app.galleryx.model.database.entity.Photo
import com.app.galleryx.sort.domain.Sort
import javax.inject.Inject

class GalleryUiStateFactory @Inject constructor() {

    fun create(
        photos: List<Photo>,
        sort: Sort,
        searchQuery: String
    ): GalleryUiState {
        val filteredPhotos = if (searchQuery.isBlank()) {
            photos
        } else {
            photos.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
        }

        if (filteredPhotos.isEmpty()) {
            return GalleryUiState.Empty
        }

        val galleryPhotos = filteredPhotos.map {
            PhotoTile(
                fileName = it.fileName,
                type = it.type,
                uuid = it.uuid,
                fileSize = it.size
            )
        }

        return GalleryUiState.Content(
            photos = galleryPhotos,
            sort = sort,
            searchQuery = searchQuery
        )
    }
}