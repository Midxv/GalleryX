package com.app.galleryx.gallery.albums.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.galleryx.R
import com.app.galleryx.gallery.albums.ui.AlbumsUiEvent
import com.app.galleryx.ui.components.MagicFab
import com.app.galleryx.ui.theme.AppTheme

@Composable
fun AlbumsPlaceholder(
    handleUiEvent: (AlbumsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.gallery_albums_placeholder),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        // FIXED: Anchored to bottom, matches AlbumsContent logic!
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        ) {
            MagicFab(
                label = stringResource(R.string.magic_fab_new_album_label),
                onClick = {
                    handleUiEvent(AlbumsUiEvent.ShowCreateDialog)
                },
            )
        }
    }
}