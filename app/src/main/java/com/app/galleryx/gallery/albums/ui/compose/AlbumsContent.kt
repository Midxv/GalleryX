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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.galleryx.R
import com.app.galleryx.gallery.albums.ui.AlbumsUiEvent
import com.app.galleryx.gallery.components.AlbumsGrid
import com.app.galleryx.gallery.components.PhotoTile
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

            // --- AI INDEXING PROGRESS BAR ---
            if (content.totalCount > 0 && content.indexedCount < content.totalCount) {
                val progress = content.indexedCount.toFloat() / content.totalCount.toFloat()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "AI Indexing Progress: ${content.indexedCount} / ${content.totalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            if (!content.photoResults.isNullOrEmpty()) {
                Text(
                    text = "AI found ${content.photoResults.size} photos for '${content.searchQuery}'",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    // THE FIX: Use weight(1f) instead of fillMaxSize()
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(content.photoResults) { photo ->
                        Box(
                            modifier = Modifier
                                .padding(1.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    // TODO: Open photo viewer
                                }
                        ) {
                            PhotoTile(
                                dateTaken = photo.importedAt,
                                fileName = photo.fileName,
                                fileSize = photo.size,
                                type = photo.type,
                                uuid = photo.uuid
                            )
                        }
                    }
                }
            } else {
                // Otherwise, just show the standard Album Grid
                AlbumsGrid(
                    albums = content.albums,
                    onAlbumClicked = { handleUiEvent(AlbumsUiEvent.OpenAlbum(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }

        // Fixed to the bottom-left to avoid blocking interaction
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