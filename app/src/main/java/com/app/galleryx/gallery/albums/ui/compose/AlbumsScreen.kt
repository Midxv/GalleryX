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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.galleryx.R
import com.app.galleryx.gallery.albums.ui.AlbumsUiEvent
import com.app.galleryx.gallery.albums.ui.AlbumsViewModel
import com.app.galleryx.gallery.components.ImportSharedDialog
import com.app.galleryx.gallery.ui.components.GalleryXTopBarSearch
import com.app.galleryx.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(viewModel: AlbumsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    AppTheme {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.gallery_albums_label)) },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        // FIXED: Added explicit type 'String' to the lambda parameter
                        GalleryXTopBarSearch(
                            query = uiState.searchQuery,
                            onQueryChanged = { query: String -> viewModel.onSearchQueryChanged(query) },
                            placeholderText = "Search...",
                            modifier = Modifier
                                .width(160.dp)
                                .height(36.dp)
                                .padding(end = 8.dp)
                        )
                    }
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { contentPadding ->

            Column(
                modifier = Modifier
                    .padding(top = contentPadding.calculateTopPadding())
                    .fillMaxSize()
            ) {
                when (uiState) {
                    is AlbumsUiState.Empty -> AlbumsPlaceholder(
                        handleUiEvent = { viewModel.handleUiEvent(it) },
                        modifier = Modifier.weight(1f)
                    )

                    is AlbumsUiState.Content -> AlbumsContent(
                        content = uiState as AlbumsUiState.Content,
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