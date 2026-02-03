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
                fileSize = it.size,
                // Use importedAt as the sort/grouping date
                dateTaken = it.importedAt
            )
        }

        return GalleryUiState.Content(
            photos = galleryPhotos,
            sort = sort,
            searchQuery = searchQuery
        )
    }
}