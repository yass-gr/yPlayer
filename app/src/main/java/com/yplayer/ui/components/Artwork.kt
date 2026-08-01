package com.yplayer.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

private fun loadSongArt(context: Context, songUri: Uri): Bitmap? = runCatching {
    context.contentResolver.loadThumbnail(songUri, Size(512, 512), null)
}.getOrNull()

private fun loadAlbumArt(context: Context, albumId: Long): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(albumArtUri(albumId), Size(512, 512), null)
    } else {
        @Suppress("DEPRECATION")
        context.contentResolver.openInputStream(albumArtUri(albumId))?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }
}.getOrNull()

@Composable
fun AlbumArt(
    albumId: Long,
    songUri: Uri? = null,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, albumId, songUri) {
        value = withContext(Dispatchers.IO) {
            if (songUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                loadSongArt(context, songUri) ?: loadAlbumArt(context, albumId)
            } else {
                loadAlbumArt(context, albumId)
            }
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
