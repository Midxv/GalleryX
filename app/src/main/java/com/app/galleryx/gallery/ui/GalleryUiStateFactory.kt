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
        searchQuery: String,
        totalCount: Int = 0,
        indexedCount: Int = 0
    ): GalleryUiState {

        // Note: We removed the text filtering here because the GalleryViewModel
        // now handles all the heavy lifting (both AI and fallback searches)
        // before handing the 'photos' list to this factory!

        if (photos.isEmpty()) {
            return GalleryUiState.Empty(
                searchQuery = searchQuery,
                totalCount = totalCount,
                indexedCount = indexedCount
            )
        }

        val galleryPhotos = photos.map {
            PhotoTile(
                fileName = it.fileName,
                type = it.type,
                uuid = it.uuid,
                fileSize = it.size,
                dateTaken = it.importedAt
            )
        }

        return GalleryUiState.Content(
            photos = galleryPhotos,
            sort = sort,
            searchQuery = searchQuery,
            totalCount = totalCount,
            indexedCount = indexedCount
        )
    }
}