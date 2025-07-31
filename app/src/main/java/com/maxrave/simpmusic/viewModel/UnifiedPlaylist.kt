package com.maxrave.simpmusic.viewModel

import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

// Esta clase sellada actúa como un "adaptador" para que todos los tipos de playlist
// puedan ser tratados de la misma manera en la interfaz de usuario.
sealed class UnifiedPlaylist(
    open val id: String,
    open val title: String,
    open val subtitle: String,
    open val imageUrl: String?,
    open val originalObject: Any // Mantenemos el objeto original por si lo necesitas
) {
    /**
     * Adaptador para las playlists locales.
     */
    data class Local(val entity: LocalPlaylistEntity) : UnifiedPlaylist(
        id = "local_${entity.id}",
        title = entity.title,
        subtitle = "Playlist local",
        imageUrl = null, // Las playlists locales no tienen imagen, se mostrará un placeholder
        originalObject = entity
    )

    /**
     * Adaptador para las playlists de YouTube Music.
     */
    data class YouTube(val result: PlaylistsResult) : UnifiedPlaylist(
        id = result.browseId ?: "yt_${result.hashCode()}",
        title = result.title,
        subtitle = result.author ?: "YouTube",
        // Esto es correcto: PlaylistsResult usa una lista de objetos Thumbnail
        imageUrl = result.thumbnails.lastOrNull()?.url,
        originalObject = result
    )

    /**
     * Adaptador para los Favoritos (que pueden ser Álbumes o Playlists).
     */
    data class Favorite(val item: com.maxrave.simpmusic.data.type.PlaylistType) : UnifiedPlaylist(
        id = when (item) {
            is AlbumEntity -> "album_${item.browseId ?: item.hashCode()}"
            is PlaylistEntity -> "playlist_${item.id}"
            else -> "favorite_unknown_${item.hashCode()}"
        },
        title = when (item) {
            is AlbumEntity -> item.title
            is PlaylistEntity -> item.title
            else -> "Desconocido"
        },
        subtitle = when (item) {
            is AlbumEntity -> item.artistName?.joinToString(", ") ?: "Álbum"
            is PlaylistEntity -> item.author ?: "Playlist"
            else -> "Favorito"
        },
        // ### CORRECCIÓN FINAL Y DEFINITIVA ###
        imageUrl = when (item) {
            // AlbumEntity tiene la URL directamente en la propiedad `thumbnails` (que es un String)
            is AlbumEntity -> item.thumbnails
            // PlaylistEntity TAMBIÉN tiene la URL directamente en la propiedad `thumbnails` (que es un String)
            is PlaylistEntity -> item.thumbnails
            else -> null
        },
        originalObject = item
    )
}