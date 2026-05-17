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
import com.app.galleryx.model.database.dao.PhotoDao
import com.app.galleryx.model.database.entity.Photo
import com.app.galleryx.search.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumsRepositoryImpl: AlbumRepository,
    private val albumUiStateFactory: AlbumUiStateFactory,
    private val photoDao: PhotoDao,
    private val searchEngine: SearchEngine
) : ViewModel() {

    private val showCreateDialog = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")

    // Combine the new progress tracking flows into a simple Pair
    private val progressFlow = combine(
        photoDao.observeTotalCount(),
        photoDao.observeIndexedCount()
    ) { total, indexed -> Pair(total, indexed) }

    val uiState: StateFlow<AlbumsUiState> = combine(
        albumsRepositoryImpl.observeAllAlbumsWithPhotos(),
        showCreateDialog,
        searchQuery,
        progressFlow,
        photoDao.observeAll() // Needed to run the AI search across photos
    ) { albums, showCreateDialog, query, progress, allPhotos ->
        val (totalCount, indexedCount) = progress
        var filteredAlbums = albums
        var aiPhotoResults: List<Photo>? = null

        if (query.isNotBlank()) {
            filteredAlbums = albums.filter { it.name.contains(query, ignoreCase = true) }

            // THE INTELLIGENCE FALLBACK: If no albums matched the search, check the AI brain for photos
            if (filteredAlbums.isEmpty()) {
                val queryVector = searchEngine.getQueryEmbedding(query)
                if (queryVector != null) {
                    val scoredPhotos = allPhotos.mapNotNull { photo ->
                        photo.embedding?.let { bytes ->
                            val score = searchEngine.cosineSimilarity(
                                queryVector,
                                searchEngine.byteArrayToFloatArray(bytes)
                            )
                            Pair(photo, score)
                        }
                    }
                    aiPhotoResults = scoredPhotos
                        .filter { it.second > 0.22f }
                        .sortedByDescending { it.second }
                        .map { it.first }
                } else {
                    // Safety fallback if AI model is still loading
                    aiPhotoResults = allPhotos.filter { it.fileName.contains(query, ignoreCase = true) }
                }
            }
        }

        // Generate the base state using your existing factory
        val state = albumUiStateFactory.create(filteredAlbums, showCreateDialog)

        // Inject the new AI and Progress parameters
        when (state) {
            is AlbumsUiState.Empty -> {
                if (!aiPhotoResults.isNullOrEmpty()) {
                    // Factory thinks it's empty because albums are empty, but we found photos! Force Content state.
                    AlbumsUiState.Content(
                        albums = emptyList(),
                        photoResults = aiPhotoResults,
                        showCreateDialog = showCreateDialog,
                        searchQuery = query,
                        indexedCount = indexedCount,
                        totalCount = totalCount
                    )
                } else {
                    state.copy(searchQuery = query, indexedCount = indexedCount, totalCount = totalCount)
                }
            }
            is AlbumsUiState.Content -> state.copy(
                searchQuery = query,
                photoResults = aiPhotoResults,
                indexedCount = indexedCount,
                totalCount = totalCount
            )
        }
    }
        .flowOn(Dispatchers.IO) // Keeps matrix math off the UI thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AlbumsUiState.Empty())

    private val navEventChannel = Channel<AlbumsNavigationEvent>()
    val navEvent = navEventChannel.receiveAsFlow()

    fun handleUiEvent(event: AlbumsUiEvent) {
        when (event) {
            AlbumsUiEvent.ShowCreateDialog -> showCreateDialog.value = true
            AlbumsUiEvent.HideCreateDialog -> showCreateDialog.value = false
            is AlbumsUiEvent.OpenAlbum -> navEventChannel.trySend(
                AlbumsNavigationEvent.OpenAlbumDetail(event.uuid)
            )
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }
}