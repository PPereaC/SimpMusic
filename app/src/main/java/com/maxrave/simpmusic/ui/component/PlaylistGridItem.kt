package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.UnifiedPlaylist

// Este es el Composable para cada tarjeta en nuestra nueva cuadrícula unificada.
@Composable
fun PlaylistGridItem(
    playlist: UnifiedPlaylist, // Recibe el objeto unificado
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Hacemos la tarjeta clickeable
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Usamos Coil (AsyncImage) para cargar la carátula desde la URL
            AsyncImage(
                model = playlist.imageUrl,
                contentDescription = playlist.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f), // Forzamos una relación de aspecto 1:1 (cuadrada)
                contentScale = ContentScale.Crop // Recorta la imagen para que llene el espacio
            )
            // Título de la playlist
            Text(
                text = playlist.title,
                style = typo.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
            )
            // Subtítulo (Autor, "Playlist Local", etc.)
            Text(
                text = playlist.subtitle,
                style = typo.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    }
}