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

package com.app.galleryx.main.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.galleryx.R
import com.app.galleryx.ui.theme.AppTheme

@Composable
fun MainMenu(
    uiState: MainMenuUiState,
    onNavigationItemClicked: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // LOWERED to 16.dp from 32.dp to hug the bottom edge nicely
            .padding(bottom = 16.dp, start = 32.dp, end = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MainNavItem(
                id = R.id.galleryFragment,
                iconRes = R.drawable.ic_image,
                labelRes = R.string.gallery_all_photos_label,
                isSelected = uiState.currentFragmentId == R.id.galleryFragment,
                onClick = { onNavigationItemClicked(R.id.galleryFragment) }
            )

            MainNavItem(
                id = R.id.albumsFragment,
                iconRes = R.drawable.ic_folder,
                labelRes = R.string.gallery_albums_label,
                isSelected = uiState.currentFragmentId == R.id.albumsFragment || uiState.currentFragmentId == R.id.albumDetailFragment,
                onClick = { onNavigationItemClicked(R.id.albumsFragment) }
            )

            MainNavItem(
                id = R.id.settingsFragment,
                iconRes = R.drawable.ic_settings,
                labelRes = R.string.menu_main_settings,
                isSelected = uiState.currentFragmentId == R.id.settingsFragment,
                onClick = { onNavigationItemClicked(R.id.settingsFragment) }
            )
        }
    }
}

@Composable
private fun MainNavItem(
    id: Int,
    iconRes: Int,
    labelRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(labelRes),
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        AnimatedVisibility(visible = isSelected) {
            Text(
                text = stringResource(labelRes),
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun MainMenuPreview() {
    AppTheme {
        MainMenu(
            uiState = MainMenuUiState(R.id.galleryFragment),
            onNavigationItemClicked = {}
        )
    }
}