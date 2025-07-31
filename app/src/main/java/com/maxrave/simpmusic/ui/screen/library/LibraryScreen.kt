package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.maxrave.simpmusic.ui.component.LibraryItem
import com.maxrave.simpmusic.ui.component.LibraryItemState
import com.maxrave.simpmusic.ui.component.LibraryItemType
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.component.PlaylistGridItem
import com.maxrave.simpmusic.ui.component.PlaylistListItem
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.LocalResource
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

// Un enum para controlar el modo de vista
private enum class ViewMode { GRID, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
) {
    // --- ESTADOS DEL VIEWMODEL ---
    val unifiedPlaylistsState by viewModel.unifiedPlaylists.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    // --- ESTADO DE LA UI ---
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.GRID) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // --- APP BAR PERSONALIZADO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.library),
                style = typo.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID }) {
                Icon(
                    imageVector = if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = stringResource(R.string.preview)
                )
            }
        }

        // --- CUERPO DE LA PANTALLA ---
        when (viewMode) {
            ViewMode.GRID -> LibraryGridView(
                unifiedPlaylistsState = unifiedPlaylistsState,
                downloadedPlaylist = downloadedPlaylist,
                recentlyAdded = recentlyAdded,
                nowPlaying = nowPlaying,
                navController = navController
            )
            ViewMode.LIST -> LibraryListView(
                unifiedPlaylistsState = unifiedPlaylistsState,
                downloadedPlaylist = downloadedPlaylist,
                recentlyAdded = recentlyAdded,
                nowPlaying = nowPlaying,
                navController = navController
            )
        }
    }
}

// --- COMPOSABLE PARA LA VISTA DE CUADRÍCULA ---
@UnstableApi
@Composable
private fun LibraryGridView(
    unifiedPlaylistsState: LocalResource<List<com.maxrave.simpmusic.viewModel.UnifiedPlaylist>>,
    downloadedPlaylist: LocalResource<List<com.maxrave.simpmusic.data.type.PlaylistType>>,
    recentlyAdded: LocalResource<List<com.maxrave.simpmusic.data.type.RecentlyType>>,
    nowPlaying: String,
    navController: NavController,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 130.dp),
        // ---> ¡CAMBIO AQUÍ! Padding horizontal reducido
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp) // <-- Espacio entre columnas
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            // ---> ¡CAMBIO AQUÍ! Se elimina el Box con padding negativo
            LibraryTilingBox(navController)
        }

        when (val resource = unifiedPlaylistsState) {
            is LocalResource.Loading -> {
                item(span = { GridItemSpan(maxLineSpan) }) { CircularProgressIndicator() }
            }
            is LocalResource.Success -> {
                val playlists = resource.data
                if (!playlists.isNullOrEmpty()) {
                    items(items = playlists, key = { "grid_${it.id}" }) { playlist ->
                        PlaylistGridItem(playlist = playlist, onClick = { onPlaylistClick(playlist, navController) })
                    }
                }
            }
            is LocalResource.Error -> {}
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                LibraryItem(
                    state = LibraryItemState(LibraryItemType.DownloadedPlaylist, downloadedPlaylist.data ?: emptyList(), downloadedPlaylist is LocalResource.Loading),
                    navController = navController
                )
                Spacer(modifier = Modifier.height(8.dp))
                LibraryItem(
                    state = LibraryItemState(LibraryItemType.RecentlyAdded(nowPlaying), recentlyAdded.data ?: emptyList(), recentlyAdded is LocalResource.Loading),
                    navController = navController
                )
            }
        }
    }
}

// --- COMPOSABLE PARA LA VISTA DE LISTA ---
@UnstableApi
@Composable
private fun LibraryListView(
    unifiedPlaylistsState: LocalResource<List<com.maxrave.simpmusic.viewModel.UnifiedPlaylist>>,
    downloadedPlaylist: LocalResource<List<com.maxrave.simpmusic.data.type.PlaylistType>>,
    recentlyAdded: LocalResource<List<com.maxrave.simpmusic.data.type.RecentlyType>>,
    nowPlaying: String,
    navController: NavController,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            LibraryTilingBox(navController)
        }

        when (val resource = unifiedPlaylistsState) {
            is LocalResource.Loading -> {
                item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
            }
            is LocalResource.Success -> {
                val playlists = resource.data
                if (!playlists.isNullOrEmpty()) {
                    items(items = playlists, key = { "list_${it.id}" }) { playlist ->
                        PlaylistListItem(playlist = playlist, onClick = { onPlaylistClick(playlist, navController) })
                    }
                }
            }
            is LocalResource.Error -> {}
        }

        item {
            LibraryItem(
                state = LibraryItemState(LibraryItemType.DownloadedPlaylist, downloadedPlaylist.data ?: emptyList(), downloadedPlaylist is LocalResource.Loading),
                navController = navController
            )
        }
        item {
            LibraryItem(
                state = LibraryItemState(LibraryItemType.RecentlyAdded(nowPlaying), recentlyAdded.data ?: emptyList(), recentlyAdded is LocalResource.Loading),
                navController = navController
            )
        }
    }
}

// --- LÓGICA DE NAVEGACIÓN (REUTILIZADA) ---
private fun onPlaylistClick(playlist: com.maxrave.simpmusic.viewModel.UnifiedPlaylist, navController: NavController) {
    when (val original = playlist.originalObject) {
        is LocalPlaylistEntity -> navController.navigate(LocalPlaylistDestination(id = original.id))
        is PlaylistsResult -> original.browseId?.let { navController.navigate(PlaylistDestination(playlistId = it, isYourYouTubePlaylist = true)) }
        is AlbumEntity -> original.browseId?.let { navController.navigate(AlbumDestination(browseId = it)) }
        is PlaylistEntity -> navController.navigate(PlaylistDestination(playlistId = original.id))
    }
}