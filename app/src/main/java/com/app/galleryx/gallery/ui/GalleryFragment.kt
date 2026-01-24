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

package com.app.galleryx.gallery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import com.app.galleryx.gallery.ui.compose.GalleryScreen
import com.app.galleryx.gallery.ui.navigation.GalleryNavigationEvent
import com.app.galleryx.gallery.ui.navigation.GalleryNavigator
import com.app.galleryx.gallery.ui.navigation.PhotoActionsNavigator
import com.app.galleryx.imageloading.compose.LocalEncryptedImageLoader
import com.app.galleryx.imageloading.di.EncryptedImageLoader
import com.app.galleryx.other.extensions.launchLifecycleAwareJob
import com.app.galleryx.settings.data.Config
import com.app.galleryx.settings.ui.compose.LocalConfig
import com.app.galleryx.ui.theme.AppTheme
import javax.inject.Inject

@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private val viewModel: GalleryViewModel by viewModels()

    @Inject
    lateinit var galleryNavigator: GalleryNavigator

    @Inject
    lateinit var photoActionsNavigator: PhotoActionsNavigator

    @Inject
    lateinit var config: Config

    @EncryptedImageLoader
    @Inject
    lateinit var encryptedImageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    CompositionLocalProvider(
                        LocalEncryptedImageLoader provides encryptedImageLoader,
                        LocalConfig provides config
                    ) {
                        // Fixed: Removed extra arguments. GalleryScreen now only takes the ViewModel.
                        GalleryScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchLifecycleAwareJob {
            // Fixed: Explicit type for 'event' to help compiler inference
            viewModel.eventsFlow.collect { event: GalleryNavigationEvent ->
                galleryNavigator.navigate(event, this@GalleryFragment)
            }
        }

        launchLifecycleAwareJob {
            viewModel.photoActions.collect { action ->
                photoActionsNavigator.navigate(action, findNavController(), this@GalleryFragment)
            }
        }
    }
}