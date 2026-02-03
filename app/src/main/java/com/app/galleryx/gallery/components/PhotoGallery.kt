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

package com.app.galleryx.gallery.components

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.galleryx.R
import com.app.galleryx.imageloading.compose.model.EncryptedImageRequestData
import com.app.galleryx.imageloading.compose.rememberEncryptedImagePainter
import com.app.galleryx.other.extensions.launchAndIgnoreTimer
import com.app.galleryx.settings.ui.compose.LocalConfig
import com.app.galleryx.ui.components.ConfirmationDialog
import com.app.galleryx.ui.components.MagicFab
import com.app.galleryx.ui.components.MultiSelectionMenu
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Composable
fun PhotoGallery(
    photos: List<PhotoTile>,
    albumName: String?,
    multiSelectionState: MultiSelectionState,
    onOpenPhoto: (PhotoTile) -> Unit,
    onExport: (Uri?) -> Unit,
    onDelete: () -> Unit,
    onImportChoice: (ImportChoice) -> Unit,
    additionalMultiSelectionActions: @Composable (ColumnScope.() -> Unit),
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    var importMenuBottomSheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(multiSelectionState.isActive.value) {
        if (multiSelectionState.isActive.value) {
            importMenuBottomSheetVisible = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Pinch to Zoom Logic
        var scale by remember { mutableFloatStateOf(1f) }
        var columnCount by remember { mutableIntStateOf(4) } // Default 4

        val transformableState = rememberTransformableState { zoomChange, _, _ ->
            scale *= zoomChange
            // Logic to snap scale to column count (2 to 6 columns)
            if (scale > 1.2f) {
                columnCount = max(2, columnCount - 1)
                scale = 1f
            } else if (scale < 0.8f) {
                columnCount = min(6, columnCount + 1)
                scale = 1f
            }
        }

        PhotoGrid(
            photos = photos,
            columnCount = columnCount,
            multiSelectionState = multiSelectionState,
            openPhoto = onOpenPhoto,
            transformableState = transformableState
        )

        AnimatedVisibility(
            visible = multiSelectionState.isActive.value.not(),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            MagicFab(
                label = stringResource(R.string.import_menu_fab_label),
                onClick = { importMenuBottomSheetVisible = true }
            )
        }

        ImportMenuBottomSheet(
            open = importMenuBottomSheetVisible,
            onDismissRequest = { importMenuBottomSheetVisible = false },
            onImportChoice = onImportChoice,
            albumName = albumName,
        )

        var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
        var showExportConfirmationDialog by remember { mutableStateOf(false) }
        var exportDirectoryUri by remember { mutableStateOf<Uri?>(null) }

        val pickExportTargetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { exportTarget ->
            exportTarget ?: return@rememberLauncherForActivityResult
            exportDirectoryUri = exportTarget
            showExportConfirmationDialog = true
        }

        ConfirmationDialog(
            show = showDeleteConfirmationDialog,
            onDismissRequest = { showDeleteConfirmationDialog = false },
            text = stringResource(R.string.delete_are_you_sure, multiSelectionState.selectedItems.value.size),
            onConfirm = {
                onDelete()
                multiSelectionState.cancelSelection()
            }
        )

        ConfirmationDialog(
            show = showExportConfirmationDialog,
            onDismissRequest = { showExportConfirmationDialog = false },
            text = stringResource(
                if (LocalConfig.current?.deleteExportedFiles == true) R.string.export_and_delete_are_you_sure else R.string.export_are_you_sure,
                multiSelectionState.selectedItems.value.size
            ),
            onConfirm = {
                onExport(exportDirectoryUri)
                multiSelectionState.cancelSelection()
            }
        )

        MultiSelectionMenu(
            modifier = Modifier.align(Alignment.BottomCenter),
            multiSelectionState = multiSelectionState,
        ) {
            DropdownMenuItem(
                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_select_all), contentDescription = null) },
                text = { Text(stringResource(R.string.menu_ms_select_all)) },
                onClick = {
                    multiSelectionState.selectAll()
                    multiSelectionState.dismissMore()
                },
            )
            DropdownMenuItem(
                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null) },
                text = { Text(stringResource(R.string.common_delete)) },
                onClick = {
                    showDeleteConfirmationDialog = true
                    multiSelectionState.dismissMore()
                },
            )
            DropdownMenuItem(
                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_export), contentDescription = null) },
                text = { Text(stringResource(R.string.common_export)) },
                onClick = {
                    pickExportTargetLauncher.launchAndIgnoreTimer(input = null, activity = activity)
                    multiSelectionState.dismissMore()
                },
            )
            additionalMultiSelectionActions()
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<PhotoTile>,
    columnCount: Int,
    multiSelectionState: MultiSelectionState,
    openPhoto: (PhotoTile) -> Unit,
    transformableState: TransformableState,
    modifier: Modifier = Modifier,
) {
    val gridState: LazyGridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current

    // Group photos by Date
    val groupedPhotos = remember(photos) {
        photos.groupBy {
            // Safe date formatting
            if (it.dateTaken > 0) {
                SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it.dateTaken))
            } else {
                "Unknown Date"
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier
            .fillMaxWidth()
            .transformable(state = transformableState), // Attach Zoom State
        state = gridState
    ) {
        groupedPhotos.forEach { (dateHeader, photosInDate) ->
            // Date Header - FIXED SPANNING
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = dateHeader,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                        .fillMaxWidth()
                )
            }

            // Photos
            items(photosInDate, key = { it.uuid }) { photo ->
                GalleryPhotoTile(
                    photoTile = photo,
                    multiSelectionActive = multiSelectionState.isActive.value,
                    onClicked = {
                        if (multiSelectionState.isActive.value.not()) {
                            openPhoto(photo)
                            return@GalleryPhotoTile
                        }
                        if (multiSelectionState.selectedItems.value.contains(photo.uuid)) {
                            multiSelectionState.deselectItem(photo.uuid)
                        } else {
                            multiSelectionState.selectItem(photo.uuid)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    },
                    selected = multiSelectionState.selectedItems.value.contains(photo.uuid),
                    onLongPress = {
                        if (multiSelectionState.isActive.value.not()) {
                            multiSelectionState.selectItem(photo.uuid)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

// DEFINED HERE TO FIX "Unresolved reference" ERROR
@Composable
fun Modifier.multiSelectionItem(selected: Boolean): Modifier {
    val animatedPadding by animateDpAsState(
        targetValue = if (selected) { 15.dp } else { 0.dp }, label = "padding"
    )
    val animatedShape by animateDpAsState(
        targetValue = if (selected) { 12.dp } else { 0.dp }, label = "shape"
    )

    return this
        .padding(animatedPadding)
        .clip(RoundedCornerShape(animatedShape))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryPhotoTile(
    modifier: Modifier = Modifier,
    photoTile: PhotoTile,
    multiSelectionActive: Boolean,
    selected: Boolean,
    onClicked: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(.5.dp)
                .combinedClickable(
                    role = Role.Image,
                    onClick = onClicked,
                    onLongClick = onLongPress,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                )
        ) {
            val contentModifier = Modifier
                .multiSelectionItem(selected)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))

            if (LocalInspectionMode.current) {
                Box(modifier = contentModifier.background(Color.DarkGray))
            } else {
                val requestData = remember(photoTile) {
                    EncryptedImageRequestData(
                        internalFileName = photoTile.internalThumbnailFileName,
                        mimeType = photoTile.type.mimeType
                    )
                }
                Image(
                    painter = rememberEncryptedImagePainter(requestData),
                    contentDescription = photoTile.fileName,
                    modifier = contentModifier
                )
            }

            // Using full package name to avoid ColumnScope issues if any
            androidx.compose.animation.AnimatedVisibility(
                visible = photoTile.type.isVideo && !selected,
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier
                    .padding(4.dp)
                    .size(20.dp)
                    .align(Alignment.BottomStart)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_videocam),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.dropShadow(shape = RoundedCornerShape(12.dp), shadow = Shadow(radius = 6.dp, alpha = 0.3f))
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = multiSelectionActive && selected,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .align(Alignment.TopStart)
                )
            }
        }

        Text(
            text = photoTile.fileName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = formatFileSize(photoTile.fileSize),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}