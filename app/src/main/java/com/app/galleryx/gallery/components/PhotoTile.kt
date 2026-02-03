package com.app.galleryx.gallery.components

import com.app.galleryx.model.database.entity.PhotoType
import com.app.galleryx.model.database.entity.internalThumbnailFileName

data class PhotoTile(
    val fileName: String,
    val type: PhotoType,
    val uuid: String,
    val fileSize: Long,
    val dateTaken: Long // Added for Date Headers
) {
    val internalThumbnailFileName = internalThumbnailFileName(uuid)
}