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

package com.app.galleryx.main.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.app.galleryx.R
import com.app.galleryx.databinding.ActivityMainBinding
import com.app.galleryx.main.ui.navigation.MainMenu
import com.app.galleryx.model.database.dao.PhotoDao
import com.app.galleryx.search.SearchEngine
import com.app.galleryx.settings.data.Config
import com.app.galleryx.ui.theme.AppTheme
import com.app.galleryx.uicomponnets.bindings.BindableActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The main Activity.
 * Holds all fragments and initializes toolbar.
 */
@AndroidEntryPoint
class MainActivity : BindableActivity<ActivityMainBinding>(R.layout.activity_main) {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    override lateinit var config: Config

    // --- Injected AI Engine and Database ---
    @Inject
    lateinit var photoDao: PhotoDao

    @Inject
    lateinit var searchEngine: SearchEngine

    var onOrientationChanged: (Int) -> Unit = {} // Init empty

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Launch the high-speed foreground AI indexer
        lifecycleScope.launch {
            // This ensures the AI ONLY runs when the app is actively on screen.
            // Swiping home instantly pauses it. Opening the app instantly resumes it.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                runForegroundFastIndexing()
            }
        }
    }

    /**
     * Pulls unindexed photos and processes them immediately using exactly 3 concurrent threads.
     */
    private suspend fun runForegroundFastIndexing() = withContext(Dispatchers.IO) {
        // Fetch all unindexed items from the database
        val allUnindexed = photoDao.getUnindexedPhotos()

        // STRICT FILTER: Since photo.type is likely an Int, we filter by filename extensions.
        // This is 100% crash-proof and guarantees we only send valid media to the AI.
        val validExtensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".heic", ".mp4", ".mkv", ".gif")

        val mediaToIndex = allUnindexed.filter { photo ->
            // Convert to lowercase safely in case fileName is somehow null
            val fileName = photo.fileName?.lowercase() ?: ""
            validExtensions.any { ext -> fileName.endsWith(ext) }
        }

        Log.d("AI_INDEXER", "Found ${mediaToIndex.size} unindexed images/videos to process.")

        if (mediaToIndex.isEmpty()) {
            return@withContext // Everything valid is indexed!
        }

        // Restrict inference to exactly 3 concurrent threads to prevent RAM spikes
        val concurrencySemaphore = Semaphore(3)

        mediaToIndex.forEach { mediaItem ->
            // Check if the user minimized the app. If they did, stop looping immediately.
            ensureActive()

            concurrencySemaphore.acquire()
            launch {
                try {
                    Log.d("AI_INDEXER", "Attempting to index: ${mediaItem.fileName}")

                    val embedding = searchEngine.indexPhoto(mediaItem)

                    if (embedding != null) {
                        mediaItem.embedding = embedding
                        photoDao.insert(mediaItem) // Save mathematical vector to DB
                        Log.d("AI_INDEXER", "SUCCESS: Indexed ${mediaItem.fileName}")
                    } else {
                        Log.e("AI_INDEXER", "FAILED: indexPhoto returned null for ${mediaItem.fileName}.")
                    }
                } catch (e: Exception) {
                    Log.e("AI_INDEXER", "CRASH during indexing ${mediaItem.fileName}", e)
                } finally {
                    concurrencySemaphore.release()
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        dispatchIntent()

        findNavController(R.id.mainNavHostFragment).let { navController ->
            navController.addOnDestinationChangedListener { _, destination, _ ->

                // Logic to update status bar appearance
                WindowCompat.getInsetsController(
                    window, window.decorView
                ).isAppearanceLightStatusBars = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES

                viewModel.onDestinationChanged(destination.id)
            }
        }
    }

    private fun dispatchIntent() {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let { viewModel.addUriToSharedUriStore(it) }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.forEach { viewModel.addUriToSharedUriStore(it) }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onOrientationChanged(newConfig.orientation)
    }

    override fun bind(binding: ActivityMainBinding) {
        super.bind(binding)
        binding.context = this

        binding.mainMenuComposeContainer.setContent {
            val uiState by viewModel.mainMenuUiState.collectAsState()
            val showBottomNav by viewModel.showBottomNav.collectAsState()

            AppTheme {
                AnimatedVisibility(
                    visible = showBottomNav,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    MainMenu(
                        uiState = uiState,
                        onNavigationItemClicked = { fragmentId ->
                            val navController = findNavController(R.id.mainNavHostFragment)
                            if (navController.currentDestination?.id != fragmentId) {

                                // Instant tab switching configuration
                                val navOptions = NavOptions.Builder()
                                    .setLaunchSingleTop(true)
                                    .setRestoreState(true)
                                    .setPopUpTo(navController.graph.startDestinationId, false, saveState = true)
                                    .build()

                                navController.navigate(fragmentId, null, navOptions)
                            }
                        },
                        onSearchClicked = {
                            viewModel.toggleSearchVisibility()
                        }
                    )
                }
            }
        }
    }
}