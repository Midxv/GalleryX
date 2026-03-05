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

package com.app.galleryx.gallery.albums.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.galleryx.R
import com.app.galleryx.gallery.albums.ui.AlbumsUiEvent
import com.app.galleryx.gallery.albums.ui.AlbumsViewModel
import com.app.galleryx.gallery.components.ImportSharedDialog
import com.app.galleryx.gallery.ui.components.GalleryXHomeTopBar
import com.app.galleryx.main.ui.MainViewModel
import com.app.galleryx.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    mainViewModel: MainViewModel,
    onSettingsClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSearchVisible by mainViewModel.isSearchVisible.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    AppTheme {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Box {
                    // Default Large Header (Same as Settings)
                    AnimatedVisibility(
                        visible = !isSearchVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        LargeTopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.gallery_albums_label),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            scrollBehavior = scrollBehavior,
                            windowInsets = WindowInsets.statusBars
                        )
                    }

                    // Search Bar Header
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        GalleryXHomeTopBar(
                            query = searchQuery,
                            onQueryChanged = { newQuery: String ->
                                searchQuery = newQuery
                                viewModel.onSearchQueryChanged(newQuery)
                            },
                            onSettingsClicked = onSettingsClicked,
                            onLogoClicked = {
                                uriHandler.openUri("https://github.com/midxv/galleryx")
                            },
                            placeholderText = "Search albums..."
                        )
                    }
                }
            }
        ) { contentPadding ->

            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            ) {
                when (val state = uiState) {
                    is AlbumsUiState.Empty -> AlbumsPlaceholder(
                        handleUiEvent = { viewModel.handleUiEvent(it) },
                        modifier = Modifier.weight(1f)
                    )

                    is AlbumsUiState.Content -> AlbumsContent(
                        content = state,
                        handleUiEvent = { viewModel.handleUiEvent(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            CreateAlbumDialog(
                show = uiState.showCreateDialog,
                onDismissRequest = {
                    viewModel.handleUiEvent(AlbumsUiEvent.HideCreateDialog)
                },
            )

            ImportSharedDialog()
        }
    }
}