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

package com.app.galleryx.gallery.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.galleryx.gallery.ui.GalleryUiEvent
import com.app.galleryx.gallery.ui.GalleryUiState
import com.app.galleryx.gallery.ui.GalleryViewModel
import com.app.galleryx.gallery.components.AlbumPickerDialog
import com.app.galleryx.gallery.components.AlbumPickerViewModel
import com.app.galleryx.gallery.components.ImportSharedDialog
import com.app.galleryx.gallery.components.rememberMultiSelectionState
import com.app.galleryx.sort.domain.SortConfig
import com.app.galleryx.sort.ui.SortingMenu
import com.app.galleryx.sort.ui.SortingMenuIconButton
import com.app.galleryx.ui.components.AppName
import com.app.galleryx.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    albumPickerViewModel: AlbumPickerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    AppTheme {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = { AppName() },
                    windowInsets = WindowInsets.statusBars,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (uiState is GalleryUiState.Content) {
                            val sort = (uiState as GalleryUiState.Content).sort

                            var showSortMenu by remember { mutableStateOf(false) }

                            SortingMenuIconButton(
                                config = SortConfig.Gallery,
                                sort = sort,
                                onClick = { showSortMenu = true },
                            )

                            SortingMenu(
                                config = SortConfig.Gallery,
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                sort = sort,
                                onSortChanged = { sort ->
                                    viewModel.handleUiEvent(GalleryUiEvent.SortChanged(sort))
                                }
                            )
                        }
                    }
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { contentPadding ->
            val modifier = Modifier.padding(top = contentPadding.calculateTopPadding())

            when (uiState) {
                is GalleryUiState.Empty -> GalleryPlaceholder(
                    handleUiEvent = { viewModel.handleUiEvent(it) },
                    modifier = modifier,
                )

                is GalleryUiState.Content -> {
                    val contentUiState = uiState as GalleryUiState.Content
                    val multiSelectionState = rememberMultiSelectionState(
                        items = contentUiState.photos.map { it.uuid }
                    )

                    GalleryContent(
                        uiState = contentUiState,
                        handleUiEvent = { viewModel.handleUiEvent(it) },
                        multiSelectionState = multiSelectionState,
                        modifier = modifier,
                    )

                    if (contentUiState.showAlbumSelectionDialog) {
                        AlbumPickerDialog(
                            viewModel = albumPickerViewModel,
                            onAlbumSelected = { selectedAlbum ->
                                viewModel.handleUiEvent(
                                    GalleryUiEvent.OnAlbumSelected(
                                        multiSelectionState.selectedItems.value.toList(),
                                        selectedAlbum,
                                    )
                                )
                                multiSelectionState.cancelSelection()
                            },
                            onDismiss = { viewModel.handleUiEvent(GalleryUiEvent.CancelAlbumSelection) }
                        )
                    }
                }
            }

            ImportSharedDialog()
        }
    }
}
