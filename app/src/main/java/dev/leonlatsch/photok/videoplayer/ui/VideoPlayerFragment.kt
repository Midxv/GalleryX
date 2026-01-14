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

package dev.leonlatsch.photok.videoplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.leonlatsch.photok.other.IntentParams
import dev.leonlatsch.photok.other.extensions.hideSystemUI

/**
 * Fragment to play videos.
 *
 * @since 1.3.0
 * @author Leon Latsch
 */
@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val photoUUID = arguments?.getString(IntentParams.PHOTO_UUID)
        if (photoUUID == null) {
            findNavController().navigateUp()
            return null
        }

        return ComposeView(requireContext()).apply {
            setContent {
                VideoPlayerScreen(
                    photoUUID,
                    findNavController(),
                )
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().hideSystemUI()

//        TODO
//        binding.playerView.setControllerVisibilityListener(
//            PlayerView.ControllerVisibilityListener { visibility ->
//                binding.videoPlayerAppBarLayout.visibility = visibility
//            }
//        )

//        TODO
//        binding.playerView.showController()

//        if (photoUUID == null) {
//            findNavController().navigateUp()
//            return
//        }

//        viewModel.setupPlayer(photoUUID)
    }
}