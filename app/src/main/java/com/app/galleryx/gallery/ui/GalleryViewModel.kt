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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.app.galleryx.gallery.ui.navigation.GalleryNavigationEvent
import com.app.galleryx.gallery.ui.navigation.PhotoAction
import com.app.galleryx.gallery.ui.navigation.PhotoAction.DeletePhotos
import com.app.galleryx.gallery.ui.navigation.PhotoAction.ExportPhotos
import com.app.galleryx.gallery.ui.navigation.PhotoAction.OpenPhoto
import com.app.galleryx.model.repositories.ImportSource
import com.app.galleryx.model.repositories.PhotoRepository
import com.app.galleryx.sort.domain.SortConfig
import com.app.galleryx.sort.domain.SortRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val sortRepository: SortRepository,
    private val galleryUiStateFactory: GalleryUiStateFactory,
) : ViewModel() {

    private val navEventChannel = Channel<GalleryNavigationEvent>()
    val eventsFlow = navEventChannel.receiveAsFlow()

    private val photoActionsChannel = Channel<PhotoAction>()
    val photoActions = photoActionsChannel.receiveAsFlow()

    private val sortId = "gallery"
    private val sortFlow = sortRepository.observeSortFor(sortId, SortConfig.Gallery.default)
    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val photosState = sortFlow.flatMapLatest { sort ->
        photoRepository.observeAll(sort)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState = combine(
        photosState,
        sortFlow,
        searchQuery
    ) { photos, sort, query ->
        galleryUiStateFactory.create(photos, sort, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), GalleryUiState.Empty)

    fun handleUiEvent(event: GalleryUiEvent) {
        when (event) {
            is GalleryUiEvent.OnDelete -> {
                val allPhotos = photosState.value
                val entitiesToDelete = allPhotos.filter { photo -> event.items.contains(photo.uuid) }
                photoActionsChannel.trySend(DeletePhotos(entitiesToDelete))
            }
            is GalleryUiEvent.OnExport -> {
                if (event.target != null) {
                    val allPhotos = photosState.value
                    val entitiesToExport = allPhotos.filter { photo -> event.items.contains(photo.uuid) }
                    photoActionsChannel.trySend(ExportPhotos(entitiesToExport, event.target))
                }
            }
            is GalleryUiEvent.OpenPhoto -> {
                photoActionsChannel.trySend(OpenPhoto(event.item.uuid))
            }
            is GalleryUiEvent.OnImportChoice -> {
                val navEvent = when (event.choice) {
                    is com.app.galleryx.gallery.components.ImportChoice.AddNewFiles ->
                        GalleryNavigationEvent.StartImport(event.choice.fileUris, ImportSource.InApp)
                    is com.app.galleryx.gallery.components.ImportChoice.RestoreBackup ->
                        GalleryNavigationEvent.StartRestoreBackup(event.choice.backupUri)
                }
                navEventChannel.trySend(navEvent)
            }
            is GalleryUiEvent.SortChanged -> {
                viewModelScope.launch {
                    sortRepository.updateSortFor(sortId, event.sort)
                }
            }
            is GalleryUiEvent.OnAddToAlbum -> {}
            is GalleryUiEvent.OnAlbumSelected -> {}
            GalleryUiEvent.CancelAlbumSelection -> {}

            // NEW: Move To Album
            is GalleryUiEvent.MoveToAlbum -> {
                viewModelScope.launch(Dispatchers.IO) {
                    // Call the appropriate method in your PhotoRepository!
                    // Example: photoRepository.addPhotosToAlbum(event.photoUuids, event.albumId)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}