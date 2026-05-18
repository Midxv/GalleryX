/*
 * Copyright 2020–2026 GalleryX
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
import com.app.galleryx.gallery.ui.navigation.PhotoAction.SharePhotos
import com.app.galleryx.model.repositories.ImportSource
import com.app.galleryx.model.repositories.PhotoRepository
import com.app.galleryx.search.SearchEngine
import com.app.galleryx.settings.data.Config
import com.app.galleryx.sort.domain.SortConfig
import com.app.galleryx.sort.domain.SortRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val sortRepository: SortRepository,
    private val galleryUiStateFactory: GalleryUiStateFactory,
    private val searchEngine: SearchEngine,
    private val config: Config
) : ViewModel() {

    private val navEventChannel = Channel<GalleryNavigationEvent>()
    val eventsFlow = navEventChannel.receiveAsFlow()

    private val photoActionsChannel = Channel<PhotoAction>()
    val photoActions = photoActionsChannel.receiveAsFlow()

    private val sortId = "gallery"
    private val sortFlow = sortRepository.observeSortFor(sortId, SortConfig.Gallery.default)
    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val photosState = combine(
        sortFlow,
        config.valuesFlow
    ) { sort, _ ->
        val showHidden = config.galleryShowHiddenAlbums
        val hiddenUuids = config.galleryHiddenAlbums
        Pair(sort, if (showHidden) emptySet() else hiddenUuids)
    }.flatMapLatest { (sort, hiddenUuids) ->
        photoRepository.observeAllSortedAndFiltered(sort, hiddenUuids)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState = combine(
        photosState,
        sortFlow,
        searchQuery
    ) { photos, sort, query ->

        val isAiEnabled = config.galleryAiSearchEnabled
        val total = if (isAiEnabled) photos.size else 0
        val indexed = if (isAiEnabled) photos.count { it.embedding != null } else 0

        if (query.isNotBlank()) {
            val standardResults = photos.filter { it.fileName.contains(query, ignoreCase = true) }

            if (isAiEnabled && query.length >= 2) {
                val queryVector = searchEngine.getQueryEmbedding(query)

                if (queryVector != null) {
                    val semanticResults = photos.mapNotNull { photo ->
                        photo.embedding?.let { embeddingBytes ->
                            val photoVector = searchEngine.byteArrayToFloatArray(embeddingBytes)
                            val score = searchEngine.cosineSimilarity(queryVector, photoVector)
                            // --- REVERTED: Back to 0.22f ---
                            if (score > 0.22f) Pair(photo, score) else null
                        }
                    }.sortedByDescending { it.second }.map { it.first }

                    val combinedResults = (semanticResults + standardResults).distinctBy { it.uuid }
                    galleryUiStateFactory.create(combinedResults, sort, query, total, indexed)
                } else {
                    galleryUiStateFactory.create(standardResults, sort, query, total, indexed)
                }
            } else {
                galleryUiStateFactory.create(standardResults, sort, query, total, indexed)
            }
        } else {
            galleryUiStateFactory.create(photos, sort, "", total, indexed)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), GalleryUiState.Empty())

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
            is GalleryUiEvent.OnShare -> {
                val allPhotos = photosState.value
                val entitiesToShare = allPhotos.filter { photo -> event.items.contains(photo.uuid) }
                photoActionsChannel.trySend(SharePhotos(entitiesToShare))
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
            is GalleryUiEvent.MoveToAlbum -> {
                viewModelScope.launch(Dispatchers.IO) {}
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}