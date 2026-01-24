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

package com.app.galleryx.gallery.albums.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.app.galleryx.gallery.albums.domain.AlbumRepository
import com.app.galleryx.gallery.albums.ui.compose.AlbumsUiState
import com.app.galleryx.gallery.albums.ui.navigation.AlbumsNavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumsRepositoryImpl: AlbumRepository,
    private val albumUiStateFactory: AlbumUiStateFactory,
) : ViewModel() {

    private val showCreateDialog = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<AlbumsUiState> = combine(
        albumsRepositoryImpl.observeAllAlbumsWithPhotos(),
        showCreateDialog,
        searchQuery
    ) { albums, showCreateDialog, query ->
        // Filter albums based on query
        val filteredAlbums = if (query.isBlank()) {
            albums
        } else {
            albums.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Pass filtered albums to factory
        val state = albumUiStateFactory.create(filteredAlbums, showCreateDialog)

        // Return state with query attached (assuming factory returns base state)
        // We need to ensure the UI knows the current query
        when(state) {
            is AlbumsUiState.Content -> state.copy(searchQuery = query)
            is AlbumsUiState.Empty -> state.copy(searchQuery = query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AlbumsUiState.Empty())

    private val navEventChannel = Channel<AlbumsNavigationEvent>()
    val navEvent = navEventChannel.receiveAsFlow()

    fun handleUiEvent(event: AlbumsUiEvent) {
        when (event) {
            AlbumsUiEvent.ShowCreateDialog -> showCreateDialog.value = true
            AlbumsUiEvent.HideCreateDialog -> showCreateDialog.value = false
            is AlbumsUiEvent.OpenAlbum -> navEventChannel.trySend(
                AlbumsNavigationEvent.OpenAlbumDetail(
                    event.uuid
                )
            )
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }
}