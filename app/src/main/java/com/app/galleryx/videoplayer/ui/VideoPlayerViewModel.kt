package com.app.galleryx.videoplayer.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.app.galleryx.model.repositories.PhotoRepository
import com.app.galleryx.other.onMain
import com.app.galleryx.uicomponnets.bindings.ObservableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val app: Application,
    private val photoRepository: PhotoRepository
) : ObservableViewModel(app) {

    init {
        cleanupCache() // Keep this just to wipe any legacy unencrypted files left from previous app versions
    }

    fun setupPlayer(photoUUID: String, onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val photo = photoRepository.get(photoUUID)
            val encryptedFile = File(app.getFileStreamPath(photo.internalFileName).canonicalPath)

            onMain {
                // Pass the raw, unmodified encrypted file path to the UI
                onReady(encryptedFile.absolutePath)
            }
        }
    }

    fun cleanupCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempDir = File(app.cacheDir, "vlc_secure_cache")
                if (tempDir.exists()) tempDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupCache()
    }
}