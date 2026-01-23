/*
 *   Copyright 2020–2026 Leon Latsch
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.app.galleryx.imageviewer.ui

import android.app.Application
import android.net.Uri
import androidx.databinding.Bindable
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.app.galleryx.BR
import com.app.galleryx.gallery.albums.domain.AlbumRepository
import com.app.galleryx.model.database.entity.Photo
import com.app.galleryx.model.repositories.PhotoRepository
import com.app.galleryx.other.onMain
import com.app.galleryx.uicomponnets.bindings.ObservableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for loading the full size photo to [ViewPhotoActivity].
 *
 * @since 1.0.0
 * @author Leon Latsch
 */
@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    app: Application,
    val photoRepository: PhotoRepository,
    val albumRepository: AlbumRepository,
) : ObservableViewModel(app) {

    var photos = listOf<Photo>()

    @get:Bindable
    var currentPhoto: Photo? = null
        set(value) {
            field = value
            notifyChange(BR.currentPhoto, value)
        }

    /**
     * Load all photo Ids.
     * Save them in viewModel and pass them to [onFinished].
     */
    fun preloadData(
        albumUUID: String,
        onFinished: (List<Photo>) -> Unit
    ) = viewModelScope.launch {
        if (photos.isNotEmpty()) {
            onFinished(photos)
            return@launch
        }

        photos = if (albumUUID.isEmpty()) {
            photoRepository.findAllPhotosByImportDateDesc()
        } else {
            albumRepository.getPhotosForAlbum(albumUUID)
        }

        onFinished(photos)
    }

    /**
     * Loads a photo. Gets called after onViewCreated
     */
    fun updateDetails(position: Int) = viewModelScope.launch {
        currentPhoto = photos[position]
    }

    /**
     * Deletes a single photo. Called after verification.
     *
     * @param onSuccess Block called on success
     * @param onError Block called on error
     */
    fun deletePhoto(onSuccess: () -> Unit, onError: () -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            currentPhoto ?: return@launch

            photoRepository.safeDeletePhoto(currentPhoto!!).let {
                onMain {
                    if (it) onSuccess() else onError()
                }
            }
        }

    /**
     * Exports a single photo. Called after verification.
     *
     * @param onSuccess Block called on success
     * @param onError Block called on error
     */
    fun exportPhoto(target: Uri, onSuccess: () -> Unit, onError: () -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            currentPhoto?.let { safeCurrentPhoto ->
                photoRepository.exportPhoto(safeCurrentPhoto, target).let { success ->
                    onMain {
                        if (success) onSuccess() else onError()
                    }
                }
            }

        }
}