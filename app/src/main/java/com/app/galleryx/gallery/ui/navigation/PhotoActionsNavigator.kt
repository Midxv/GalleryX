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

package com.app.galleryx.gallery.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.app.galleryx.gallery.ui.menu.DeleteBottomSheetDialogFragment
import com.app.galleryx.gallery.ui.menu.ExportBottomSheetDialogFragment
import com.app.galleryx.imageviewer.ui.ImageViewerFragmentDirections
import com.app.galleryx.model.database.entity.Photo
import com.app.galleryx.model.repositories.PhotoRepository
import com.app.galleryx.other.extensions.show
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class PhotoActionsNavigator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoRepository: PhotoRepository
) {
    fun navigate(action: PhotoAction, navController: NavController, fragment: Fragment) {
        when (action) {
            is PhotoAction.DeletePhotos -> confirmAndDelete(
                action.photos,
                fragment.childFragmentManager
            )

            is PhotoAction.ExportPhotos -> confirmAndExport(
                action.photos,
                action.target,
                fragment.childFragmentManager
            )

            is PhotoAction.SharePhotos -> sharePhotos(
                action.photos,
                fragment
            )

            is PhotoAction.OpenPhoto -> navigateOpenPhoto(action.photoUUID, action.albumUUID, navController)
        }
    }

    private fun sharePhotos(photos: List<Photo>, fragment: Fragment) {
        if (photos.isEmpty()) return

        // 1. Instant UI Feedback so it doesn't feel frozen during decryption
        Toast.makeText(context, "Preparing files for sharing...", Toast.LENGTH_SHORT).show()

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val shareCacheDir = File(context.cacheDir, "share_cache")
                if (shareCacheDir.exists()) {
                    shareCacheDir.deleteRecursively() // Wipe old files
                }
                shareCacheDir.mkdirs()

                val uris = ArrayList<Uri>()

                photos.forEach { photo ->
                    val bytes = photoRepository.loadPhoto(photo)
                    if (bytes != null) {
                        val tempFile = File(shareCacheDir, photo.fileName)
                        FileOutputStream(tempFile).use { fos ->
                            fos.write(bytes)
                        }

                        // Use context.packageName to guarantee it matches the Manifest authority
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        uris.add(uri)
                    } else {
                        Log.e("ShareError", "Failed to decrypt/load bytes for ${photo.fileName}")
                    }
                }

                if (uris.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to decrypt files.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (!fragment.isAdded) return@withContext

                    // 2. Smart MIME Type calculation to ensure all apps appear in the Share Sheet
                    val mimeType = when {
                        photos.all { it.type.isVideo } -> "video/*"
                        photos.all { !it.type.isVideo } -> "image/*"
                        else -> "*/*"
                    }

                    val intent = if (uris.size == 1) {
                        Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uris.first())
                            type = mimeType
                        }
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                            type = mimeType
                        }
                    }

                    // 3. CRITICAL: Grant the target app permission to read our secure URI
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    val chooser = Intent.createChooser(intent, "Share via")
                    fragment.startActivity(chooser)
                }
            } catch (e: Exception) {
                // Catch any underlying security or I/O crashes and show them on screen
                Log.e("ShareError", "Crash during share preparation", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmAndExport(
        photos: List<Photo>,
        target: Uri,
        fragmentManager: FragmentManager,
    ) {
        ExportBottomSheetDialogFragment(photos, target).show(fragmentManager)
    }

    private fun confirmAndDelete(
        photos: List<Photo>,
        fragmentManager: FragmentManager
    ) {
        DeleteBottomSheetDialogFragment(photos).show(fragmentManager)
    }

    private fun navigateOpenPhoto(photoUUID: String, albumUUID: String, navController: NavController) {
        val direction = ImageViewerFragmentDirections.actionGlobalImageViewerFragment(photoUuid = photoUUID, albumUuid = albumUUID)
        navController.navigate(direction)
    }
}

sealed interface PhotoAction {
    data class OpenPhoto(val photoUUID: String, val albumUUID: String = "") : PhotoAction
    data class DeletePhotos(val photos: List<Photo>) : PhotoAction
    data class ExportPhotos(val photos: List<Photo>, val target: Uri) : PhotoAction
    data class SharePhotos(val photos: List<Photo>) : PhotoAction
}