/*
 * Copyright 2020–2026 GalleryX
 */

package com.app.galleryx.gallery.albums.detail.ui.compose

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.app.galleryx.R
import com.app.galleryx.gallery.albums.detail.ui.AlbumDetailUiEvent
import com.app.galleryx.gallery.albums.detail.ui.AlbumDetailUiState
import com.app.galleryx.gallery.components.PhotoGallery
import com.app.galleryx.gallery.components.PhotoTile
import com.app.galleryx.gallery.components.rememberMultiSelectionState
import com.app.galleryx.model.database.entity.PhotoType
import com.app.galleryx.ui.theme.AppTheme

@Composable
fun AlbumDetailContent(
    uiState: AlbumDetailUiState,
    handleUiEvent: (AlbumDetailUiEvent) -> Unit,
    onMoveToAnotherAlbum: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val multiSelectionState =
        rememberMultiSelectionState(items = uiState.photos.map { it.uuid })

    PhotoGallery(
        photos = uiState.photos,
        albumName = uiState.albumName,
        multiSelectionState = multiSelectionState,
        onOpenPhoto = { handleUiEvent(AlbumDetailUiEvent.OpenPhoto(it)) },
        onExport = { targetUri ->
            handleUiEvent(
                AlbumDetailUiEvent.OnExport(
                    multiSelectionState.selectedItems.value.toList(),
                    targetUri,
                )
            )
        },
        onDelete = {
            handleUiEvent(
                AlbumDetailUiEvent.OnDelete(
                    multiSelectionState.selectedItems.value.toList()
                )
            )
        },
        onImportChoice = {
            handleUiEvent(AlbumDetailUiEvent.OnImportChoice(it))
        },
        additionalMultiSelectionActions = {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_ms_move_to_album)) },
                onClick = {
                    onMoveToAnotherAlbum(multiSelectionState.selectedItems.value.toList())
                    multiSelectionState.dismissMore()
                    multiSelectionState.cancelSelection()
                },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_folder),
                        contentDescription = stringResource(R.string.menu_ms_move_to_album),
                    )
                }
            )
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AlbumsDetailScreenPreview() {
    AppTheme {
        AlbumDetailContent(
            uiState = AlbumDetailUiState(
                "",
                "Album Name",
                listOf(
                    PhotoTile("file1", PhotoType.JPEG, "uuid1", 1024, System.currentTimeMillis()),
                    PhotoTile("file2", PhotoType.JPEG, "uuid2", 2048, System.currentTimeMillis())
                )
            ),
            handleUiEvent = {},
            onMoveToAnotherAlbum = {},
        )
    }
}