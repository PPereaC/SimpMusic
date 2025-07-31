package com.maxrave.simpmusic.ui.screen.library

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.ui.component.PlaylistGridItem
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.LocalResource
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
) {
    val unifiedPlaylistsState by viewModel.unifiedPlaylists.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.library),
                    style = typo.titleMedium,
                )
            },
        )

        when (val resource = unifiedPlaylistsState) {
            is LocalResource.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LocalResource.Success -> {
                val playlists = resource.data
                if (playlists.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Añade música para verla aquí",
                            style = typo.bodyLarge
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Playlists",
                                style = typo.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(
                            items = playlists,
                            key = { it.id }
                        ) { playlist ->
                            PlaylistGridItem(
                                playlist = playlist,
                                onClick = {
                                    // ### NAVEGACIÓN CORREGIDA ###
                                    // Ahora creamos el objeto de destino correcto y navegamos a él.
                                    when (val original = playlist.originalObject) {

                                        is LocalPlaylistEntity -> {
                                            Log.d("LibraryScreenNav", "Navigating to LocalPlaylistDestination with id: ${original.id}")
                                            // Creamos el objeto de destino para playlists locales
                                            val destination = LocalPlaylistDestination(id = original.id)
                                            navController.navigate(destination)
                                        }

                                        is PlaylistsResult -> {
                                            val navId = original.browseId
                                            Log.d("LibraryScreenNav", "Navigating to PlaylistDestination with id: $navId")
                                            if (!navId.isNullOrEmpty()) {
                                                // Creamos el objeto de destino para playlists de YouTube
                                                val destination = PlaylistDestination(playlistId = navId, isYourYouTubePlaylist = true)
                                                navController.navigate(destination)
                                            }
                                        }

                                        is AlbumEntity -> {
                                            val navId = original.browseId
                                            Log.d("LibraryScreenNav", "Navigating to AlbumDestination with id: $navId")
                                            if (!navId.isNullOrEmpty()) {
                                                // Creamos el objeto de destino para álbumes
                                                val destination = AlbumDestination(browseId = navId)
                                                navController.navigate(destination)
                                            }
                                        }

                                        is PlaylistEntity -> {
                                            val navId = original.id
                                            Log.d("LibraryScreenNav", "Navigating to PlaylistDestination with id: $navId")
                                            if (!navId.isNullOrEmpty()) {
                                                // Creamos el objeto de destino para playlists (genéricas)
                                                val destination = PlaylistDestination(playlistId = navId)
                                                navController.navigate(destination)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            is LocalResource.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = resource.message ?: "Ocurrió un error")
                }
            }
        }
    }
}