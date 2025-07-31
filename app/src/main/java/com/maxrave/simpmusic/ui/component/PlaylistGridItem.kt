package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.viewModel.UnifiedPlaylist

@Composable
fun PlaylistGridItem(
    playlist: UnifiedPlaylist,
    onClick: () -> Unit
) {
    // La tarjeta ahora solo contiene la imagen
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), // Mantenemos los bordes suaves
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // La carátula ocupa todo el espacio de la tarjeta
        AsyncImage(
            model = playlist.imageUrl,
            contentDescription = playlist.title, // El título sigue siendo útil para la accesibilidad
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Lo hacemos cuadrado para que se vea uniforme
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}