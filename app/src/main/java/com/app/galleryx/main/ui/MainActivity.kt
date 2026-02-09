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
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.app.galleryx.R
import com.app.galleryx.databinding.ActivityMainBinding
import com.app.galleryx.settings.data.Config
import com.app.galleryx.uicomponnets.bindings.BindableActivity
import javax.inject.Inject

/**
 * The main Activity.
 * Holds all fragments and initializes toolbar.
 *
 * @since 1.0.0
 * @author Leon Latsch
 */
@AndroidEntryPoint
class MainActivity : BindableActivity<ActivityMainBinding>(R.layout.activity_main) {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    override lateinit var config: Config

    var onOrientationChanged: (Int) -> Unit = {} // Init empty

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
        // Bottom Navigation Menu setup removed
    }
}