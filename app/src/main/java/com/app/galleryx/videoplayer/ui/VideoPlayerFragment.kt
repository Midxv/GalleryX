/*
 *   Copyright 2020–2026 Leon Latsch
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.app.galleryx.videoplayer.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.app.galleryx.BR
import com.app.galleryx.R
import com.app.galleryx.databinding.FragmentVideoPlayerBinding
import com.app.galleryx.other.IntentParams
import com.app.galleryx.other.extensions.hideSystemUI
import com.app.galleryx.uicomponnets.bindings.BindableFragment

/**
 * Fragment to play videos.
 *
 * @since 1.3.0
 * @author Leon Latsch
 */
@AndroidEntryPoint
class VideoPlayerFragment :
    BindableFragment<FragmentVideoPlayerBinding>(R.layout.fragment_video_player) {

    private val viewModel: VideoPlayerViewModel by viewModels()

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().hideSystemUI()

        binding.videoPlayerToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.addOnPropertyChange<ExoPlayer?>(BR.player) {
            it ?: return@addOnPropertyChange
            binding.playerView.player = it
        }

        binding.playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                binding.videoPlayerAppBarLayout.visibility = visibility
            }
        )

        binding.playerView.showController()

        val photoUUID = arguments?.getString(IntentParams.PHOTO_UUID)
        if (photoUUID == null) {
            findNavController().navigateUp()
            return
        }

        viewModel.setupPlayer(photoUUID)
    }

    override fun bind(binding: FragmentVideoPlayerBinding) {
        super.bind(binding)
        binding.context = this
    }
}