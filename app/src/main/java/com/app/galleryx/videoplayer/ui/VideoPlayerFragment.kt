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

package com.app.galleryx.videoplayer.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.app.galleryx.R
import com.app.galleryx.databinding.FragmentVideoPlayerBinding
import com.app.galleryx.other.IntentParams
import com.app.galleryx.other.extensions.hideSystemUI
import com.app.galleryx.uicomponnets.bindings.BindableFragment
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.util.Locale

@AndroidEntryPoint
class VideoPlayerFragment :
    BindableFragment<FragmentVideoPlayerBinding>(R.layout.fragment_video_player) {

    private val viewModel: VideoPlayerViewModel by viewModels()

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null

    private var isUserSeeking = false
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideControls() }

    // Aspect Ratio States
    private val aspectRatios = arrayOf(null, "16:9", "4:3", "1:1")
    private val aspectRatioLabels = arrayOf("FIT", "16:9", "4:3", "1:1")
    private var currentAspectRatioIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().hideSystemUI()

        binding.videoPlayerToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val photoUUID = arguments?.getString(IntentParams.PHOTO_UUID)
        if (photoUUID == null) {
            findNavController().navigateUp()
            return
        }

        val options = arrayListOf("-vvv", "--drop-late-frames", "--skip-frames")
        libVLC = LibVLC(requireContext(), options)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer?.attachViews(binding.vlcVideoLayout, null, false, false)

        setupControls()
        setupGestures()

        viewModel.setupPlayer(photoUUID) { filePath ->
            libVLC?.let { vlc ->
                val media = Media(vlc, filePath)
                media.setHWDecoderEnabled(true, false)
                mediaPlayer?.media = media
                media.release()
                mediaPlayer?.play()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (binding.controlsContainer.visibility == View.VISIBLE) hideControls() else showControls()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenWidth = binding.touchOverlay.width
                mediaPlayer?.let { player ->
                    if (e.x < screenWidth / 2) {
                        // Double Tap Left: Rewind 5s
                        player.time = maxOf(0, player.time - 5000)
                        animateIndicator("-5s")
                    } else {
                        // Double Tap Right: Skip 5s
                        player.time = minOf(player.length, player.time + 5000)
                        animateIndicator("+5s")
                    }
                }
                return true
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        binding.touchOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun setupControls() {
        // Play / Pause
        binding.btnPlayPause.setOnClickListener {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.pause() else player.play()
                resetHideTimer()
            }
        }

        // Aspect Ratio Toggle
        binding.btnAspectRatio.setOnClickListener {
            currentAspectRatioIndex = (currentAspectRatioIndex + 1) % aspectRatios.size
            mediaPlayer?.aspectRatio = aspectRatios[currentAspectRatioIndex]

            val label = aspectRatioLabels[currentAspectRatioIndex]
            binding.btnAspectRatio.text = label
            animateIndicator("Aspect: $label")
            resetHideTimer()
        }

        // Rotate Screen Toggle
        binding.btnRotate.setOnClickListener {
            val currentOrientation = requireActivity().requestedOrientation
            requireActivity().requestedOrientation = if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
            resetHideTimer()
        }

        // SeekBar Logic
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvCurrentTime.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                hideHandler.removeCallbacks(hideRunnable)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let { mediaPlayer?.time = it.progress.toLong() }
                resetHideTimer()
            }
        })

        // VLC Callbacks
        mediaPlayer?.setEventListener { event ->
            requireActivity().runOnUiThread {
                when (event.type) {
                    MediaPlayer.Event.TimeChanged -> {
                        if (!isUserSeeking) {
                            binding.seekBar.progress = event.timeChanged.toInt()
                            binding.tvCurrentTime.text = formatTime(event.timeChanged)
                        }
                    }
                    MediaPlayer.Event.LengthChanged -> {
                        binding.seekBar.max = event.lengthChanged.toInt()
                        binding.tvTotalTime.text = formatTime(event.lengthChanged)
                    }
                    MediaPlayer.Event.Playing -> {
                        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                        resetHideTimer()
                    }
                    MediaPlayer.Event.Paused -> {
                        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        hideHandler.removeCallbacks(hideRunnable)
                    }
                }
            }
        }
    }

    /**
     * Beautiful ripple fade animation for user actions
     */
    private fun animateIndicator(text: String) {
        binding.tvIndicator.text = text
        binding.tvIndicator.alpha = 1f
        binding.tvIndicator.scaleX = 0.8f
        binding.tvIndicator.scaleY = 0.8f

        binding.tvIndicator.animate()
            .alpha(0f)
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(600)
            .start()
    }

    private fun showControls() {
        binding.controlsContainer.visibility = View.VISIBLE
        binding.videoPlayerAppBarLayout.visibility = View.VISIBLE
        resetHideTimer()
    }

    private fun hideControls() {
        binding.controlsContainer.visibility = View.GONE
        binding.videoPlayerAppBarLayout.visibility = View.GONE
    }

    private fun resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)
        if (mediaPlayer?.isPlaying == true) {
            hideHandler.postDelayed(hideRunnable, 3000)
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onDestroyView() {
        hideHandler.removeCallbacks(hideRunnable)
        mediaPlayer?.stop()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        libVLC?.release()
        mediaPlayer = null
        libVLC = null
        
        if (!requireActivity().isChangingConfigurations) {
            viewModel.cleanupCache()
        }

        super.onDestroyView()
    }

    override fun bind(binding: FragmentVideoPlayerBinding) {
        super.bind(binding)
        binding.context = this
    }
}