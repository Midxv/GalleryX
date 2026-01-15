/*
 *   Copyright 2020-2026 Leon Latsch
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

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.buttons.RepeatButton
import androidx.media3.ui.compose.material3.buttons.SeekBackButton
import androidx.media3.ui.compose.material3.buttons.SeekForwardButton
import androidx.navigation.NavController
import dev.leonlatsch.photok.R
import dev.leonlatsch.photok.ui.theme.AppTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    photoUuid: String,
    navController: NavController,
) {
    val viewModel = hiltViewModel<VideoPlayerViewModel>()
    val player by viewModel.player.collectAsStateWithLifecycle()

    val controlBackground = Color.Black.copy(alpha = 0.5f)

    AppTheme(useDarkTheme = true) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = { navController.navigateUp() }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_back),
                                    contentDescription = null
                                )
                            }
                        },
                        actions = {
                            player?.let {
                                RepeatButton(
                                    player = it,
                                    toggleModeSequence = listOf(
                                        Player.REPEAT_MODE_OFF,
                                        Player.REPEAT_MODE_ONE,
                                    )
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = controlBackground,
                            scrolledContainerColor = controlBackground,
                        )
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        containerColor = controlBackground,
                        contentColor = LocalContentColor.current,
                    ) {
                        LinearProgressIndicator(
                            progress = { 0.5f },
                        )
                    }
                },
                containerColor = Color.Black,
                modifier = Modifier.consumeWindowInsets(WindowInsets.systemBars)
            ) {
                LifecycleStartEffect(Unit) {
                    viewModel.setupPlayer(photoUuid)
                    onStopOrDispose {
                        viewModel.releasePlayer()
                    }
                }

                Box(
                    modifier = Modifier
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Magenta)
                            .fillMaxSize()
                    )

                    ContentFrame(
                        player = player,
                    )

                    player?.let {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                        ) {
                            SeekBackButton(it)
                            PlayPauseButton(it)
                            SeekForwardButton(it)
                        }
                    }
                }
            }
        }
    }
}
