package com.app.galleryx.videoplayer.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.app.galleryx.model.repositories.PhotoRepository
import com.app.galleryx.other.onMain
import com.app.galleryx.security.EncryptionManager
import com.app.galleryx.uicomponnets.bindings.ObservableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val app: Application,
    private val photoRepository: PhotoRepository,
    private val encryptionManager: EncryptionManager,
) : ObservableViewModel(app) {

    init {
        // SECURITY: Instantly wipe any lingering cache from previous crashes
        cleanupCache()
    }

    /**
     * Extracts the encrypted video to an ultra-secure internal sandbox.
     */
    fun setupPlayer(photoUUID: String, onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val photo = photoRepository.get(photoUUID)
            val encryptedFile = File(app.getFileStreamPath(photo.internalFileName).canonicalPath)

            val tempDir = File(app.cacheDir, "vlc_secure_cache").apply { mkdirs() }
            val tempFile = File(tempDir, "${photoUUID}.mp4")

            if (!tempFile.exists()) {
                encryptionManager.createCipherInputStream(encryptedFile.inputStream())?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            onMain {
                onReady(tempFile.absolutePath)
            }
        }
    }

    /**
     * SECURITY: Aggressively self-destructs all files in the secure cache.
     */
    fun cleanupCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempDir = File(app.cacheDir, "vlc_secure_cache")
                if (tempDir.exists()) {
                    tempDir.listFiles()?.forEach {
                        it.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupCache()
    }
}