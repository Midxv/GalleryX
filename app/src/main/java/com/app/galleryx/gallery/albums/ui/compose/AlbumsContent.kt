/*
 * Copyright 2020–2026 Leon Latsch
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.galleryx.R
import com.app.galleryx.gallery.albums.ui.AlbumsUiEvent
import com.app.galleryx.gallery.components.AlbumsGrid
import com.app.galleryx.ui.components.MagicFab

@Composable
fun AlbumsContent(
    content: AlbumsUiState.Content,
    handleUiEvent: (AlbumsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Standard Albums Grid
            AlbumsGrid(
                albums = content.albums,
                onAlbumClicked = { handleUiEvent(AlbumsUiEvent.OpenAlbum(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(start = 24.dp, bottom = 96.dp)
        ) {
            MagicFab(
                label = stringResource(R.string.magic_fab_new_album_label),
                onClick = {
                    handleUiEvent(AlbumsUiEvent.ShowCreateDialog)
                }
            )
        }
    }
}