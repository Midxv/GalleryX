package com.app.galleryx.videoplayer.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.app.galleryx.R
import com.app.galleryx.databinding.FragmentVideoPlayerBinding
import com.app.galleryx.other.IntentParams
import com.app.galleryx.other.extensions.hideSystemUI
import com.app.galleryx.security.EncryptionManager
import com.app.galleryx.uicomponnets.bindings.BindableFragment
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VideoPlayerFragment :
    BindableFragment<FragmentVideoPlayerBinding>(R.layout.fragment_video_player) {

    private val viewModel: VideoPlayerViewModel by viewModels()

    @Inject
    lateinit var encryptionManager: EncryptionManager

    private var exoPlayer: ExoPlayer? = null

    private var isUserSeeking = false
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideControls() }

    // Runnable to update custom seekbar since ExoPlayer doesn't emit tick events automatically
    private val progressRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying && !isUserSeeking) {
                    binding.seekBar.progress = player.currentPosition.toInt()
                    binding.tvCurrentTime.text = formatTime(player.currentPosition)
                }
            }
            hideHandler.postDelayed(this, 1000)
        }
    }

    // ExoPlayer Resize Modes
    private val resizeModes = arrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )
    private val resizeModeLabels = arrayOf("FIT", "FILL", "ZOOM")
    private var currentAspectRatioIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().hideSystemUI()

        // Intro animation for a smoother opening experience
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        binding.videoPlayerToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val photoUUID = arguments?.getString(IntentParams.PHOTO_UUID)
        if (photoUUID == null) {
            findNavController().navigateUp()
            return
        }

        setupControls()
        setupGestures()

        viewModel.setupPlayer(photoUUID) { filePath ->
            initializePlayer(filePath)
        }
    }

    private fun initializePlayer(filePath: String) {
        // 1. Create our custom offline decryptor
        val dataSourceFactory = DataSource.Factory {
            EncryptedFileDataSource(encryptionManager)
        }

        // 2. Wrap it so ExoPlayer understands how to stream it
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.fromFile(File(filePath))))

        exoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            binding.playerView.player = this

            // Use setMediaSource instead of setMediaItem for custom decryption
            setMediaSource(mediaSource)

            // INSTANT AUTO-PLAY
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        binding.seekBar.max = duration.toInt()
                        binding.tvTotalTime.text = formatTime(duration)

                        // BULLETPROOF AUTO-PLAY
                        play()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                        hideHandler.post(progressRunnable)
                        resetHideTimer()
                    } else {
                        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        hideHandler.removeCallbacks(progressRunnable)
                        hideHandler.removeCallbacks(hideRunnable)
                    }
                }
            })

            prepare()
            play()
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
                exoPlayer?.let { player ->
                    if (e.x < screenWidth / 2) {
                        // Double Tap Left: Rewind 5s
                        player.seekTo(maxOf(0, player.currentPosition - 5000))
                        animateIndicator("-5s")
                    } else {
                        // Double Tap Right: Skip 5s
                        player.seekTo(minOf(player.duration, player.currentPosition + 5000))
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
            exoPlayer?.let { player ->
                if (player.isPlaying) player.pause() else player.play()
                resetHideTimer()
            }
        }

        // Aspect Ratio Toggle
        binding.btnAspectRatio.setOnClickListener {
            currentAspectRatioIndex = (currentAspectRatioIndex + 1) % resizeModes.size
            binding.playerView.resizeMode = resizeModes[currentAspectRatioIndex]

            val label = resizeModeLabels[currentAspectRatioIndex]
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
                seekBar?.let { exoPlayer?.seekTo(it.progress.toLong()) }
                resetHideTimer()
            }
        })
    }

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
        if (exoPlayer?.isPlaying == true) {
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
        exoPlayer?.pause()
    }

    override fun onDestroyView() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.removeCallbacks(progressRunnable)
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null

        if (!requireActivity().isChangingConfigurations) {
            viewModel.cleanupCache()

            // FIX: Reset the Activity orientation back to the system default
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        super.onDestroyView()
    }

    override fun bind(binding: FragmentVideoPlayerBinding) {
        super.bind(binding)
        binding.context = this
    }
}