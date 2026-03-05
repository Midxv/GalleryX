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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.galleryx.gallery.albums.ui.AlbumsUiEvent
import com.app.galleryx.gallery.albums.ui.AlbumsViewModel
import com.app.galleryx.gallery.components.ImportSharedDialog
import com.app.galleryx.gallery.ui.components.GalleryXHomeTopBar
import com.app.galleryx.main.ui.MainActivity
import com.app.galleryx.main.ui.MainViewModel
import com.app.galleryx.ui.theme.AppTheme

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onSettingsClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 1. Fetch the Shared MainViewModel scoped to the MainActivity
    val activity = LocalContext.current as? MainActivity
    val mainViewModel: MainViewModel? = activity?.let { hiltViewModel(it) }

    // 2. Collect the global search visibility state
    val isSearchVisible by mainViewModel?.isSearchVisible?.collectAsStateWithLifecycle(initialValue = false)
        ?: mutableStateOf(false)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    AppTheme {
        Scaffold(
            topBar = {
                // 3. Beautifully animate the TopBar sliding down when the user clicks the Navbar search icon
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