package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.type.RecentlyType
import com.maxrave.simpmusic.utils.LocalResource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

@UnstableApi
class LibraryViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    // ... (Tus otros StateFlows se mantienen igual)
    private val _recentlyAdded: MutableStateFlow<LocalResource<List<RecentlyType>>> =
        MutableStateFlow(LocalResource.Loading())
    val recentlyAdded: StateFlow<LocalResource<List<RecentlyType>>> get() = _recentlyAdded

    private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist

    private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist

    private val _favoritePlaylist: MutableStateFlow<LocalResource<List<com.maxrave.simpmusic.data.type.PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePlaylist: StateFlow<LocalResource<List<com.maxrave.simpmusic.data.type.PlaylistType>>> get() = _favoritePlaylist

    // ... (Tus otros StateFlows como favoritePodcasts, downloadedPlaylist, listCanvasSong se mantienen)
    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<com.maxrave.simpmusic.data.type.PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<com.maxrave.simpmusic.data.type.PlaylistType>>> get() = _favoritePodcasts

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<com.maxrave.simpmusic.data.type.PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<com.maxrave.simpmusic.data.type.PlaylistType>>> get() = _downloadedPlaylist

    private val _listCanvasSong: MutableStateFlow<LocalResource<List<SongEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val listCanvasSong: StateFlow<LocalResource<List<SongEntity>>> get() = _listCanvasSong

    // NUEVO: El StateFlow que contendrá la lista unificada para la UI.
    private val _unifiedPlaylists: MutableStateFlow<LocalResource<List<UnifiedPlaylist>>> =
        MutableStateFlow(LocalResource.Loading())
    val unifiedPlaylists: StateFlow<LocalResource<List<UnifiedPlaylist>>> get() = _unifiedPlaylists


    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

    // NUEVO: Bloque init para cargar todo al iniciar el ViewModel.
    init {
        // Carga todas las playlists que queremos combinar.
        getPlaylistFavorite()
        getYouTubePlaylist()
        getLocalPlaylist()

        // Inicia el proceso de combinación.
        combineAllPlaylists()

        // Puedes seguir cargando el resto de datos que no se combinan aquí.
        getRecentlyAdded()
        getFavoritePodcasts()
        getCanvasSong()
        getDownloadedPlaylist()
    }

    // NUEVA: La función clave que combina las tres fuentes de playlists.
    private fun combineAllPlaylists() {
        viewModelScope.launch {
            // Usamos 'combine' para escuchar cambios en las tres listas simultáneamente.
            combine(
                yourLocalPlaylist,
                youTubePlaylist,
                favoritePlaylist
            ) { localRes, youtubeRes, favoriteRes ->
                // Este bloque se ejecutará cada vez que cualquiera de las tres listas cambie.

                // Primero, comprobamos si todas las fuentes están todavía cargando.
                if (localRes is LocalResource.Loading || youtubeRes is LocalResource.Loading || favoriteRes is LocalResource.Loading) {
                    return@combine LocalResource.Loading()
                }

                // Extraemos los datos si la carga fue exitosa. Si no, usamos una lista vacía.
                val localData = (localRes as? LocalResource.Success)?.data ?: emptyList()
                val youtubeData = (youtubeRes as? LocalResource.Success)?.data ?: emptyList()
                val favoriteData = (favoriteRes as? LocalResource.Success)?.data ?: emptyList()

                // Mapeamos cada lista a nuestro nuevo tipo 'UnifiedPlaylist'.
                val localPlaylists = localData.map { UnifiedPlaylist.Local(it) }
                val youtubePlaylists = youtubeData.map { UnifiedPlaylist.YouTube(it) }
                val favoritePlaylists = favoriteData.map { UnifiedPlaylist.Favorite(it) }

                // Las juntamos todas en una sola lista.
                val combinedList = (localPlaylists + youtubePlaylists + favoritePlaylists)
                    // Opcional: Ordena la lista final por título, ignorando mayúsculas/minúsculas.
                    .sortedBy { it.title.lowercase() }


                LocalResource.Success(combinedList)

            }.collectLatest { combinedResource ->
                // Publicamos la lista combinada (o el estado de carga) en nuestro nuevo StateFlow.
                _unifiedPlaylists.value = combinedResource
            }
        }
    }


    fun getRecentlyAdded() {
        viewModelScope.launch {
            val temp: MutableList<RecentlyType> = mutableListOf()
            mainRepository.getAllRecentData().collect { data ->
                temp.addAll(data)
                temp
                    .find {
                        it is PlaylistEntity && (it.id.contains("RDEM") || it.id.contains("RDAMVM"))
                    }.let {
                        temp.remove(it)
                    }
                temp.removeIf { it is SongEntity && it.inLibrary == Config.REMOVED_SONG_DATE_TIME }
                temp.reverse()
                _recentlyAdded.value = LocalResource.Success(temp)
            }
        }
    }

    fun getYouTubePlaylist() {
        _youTubePlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            mainRepository.getLibraryPlaylist().collect { data ->
                _youTubePlaylist.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeLoggedIn(): Boolean = runBlocking { dataStoreManager.loggedIn.first() } == DataStoreManager.TRUE

    fun getPlaylistFavorite() {
        viewModelScope.launch {
            mainRepository.getLikedAlbums().collect { album ->
                val temp: MutableList<com.maxrave.simpmusic.data.type.PlaylistType> = mutableListOf()
                temp.addAll(album)
                mainRepository.getLikedPlaylists().collect { playlist ->
                    temp.addAll(playlist)
                    val sortedList =
                        temp.sortedWith<com.maxrave.simpmusic.data.type.PlaylistType>(
                            Comparator { p0, p1 ->
                                val timeP0: LocalDateTime? =
                                    when (p0) {
                                        is AlbumEntity -> p0.inLibrary
                                        is PlaylistEntity -> p0.inLibrary
                                        else -> null
                                    }
                                val timeP1: LocalDateTime? =
                                    when (p1) {
                                        is AlbumEntity -> p1.inLibrary
                                        is PlaylistEntity -> p1.inLibrary
                                        else -> null
                                    }
                                if (timeP0 == null || timeP1 == null) {
                                    return@Comparator if (timeP0 == null && timeP1 == null) {
                                        0
                                    } else if (timeP0 == null) {
                                        -1
                                    } else {
                                        1
                                    }
                                }
                                timeP0.compareTo(timeP1)
                            },
                        ).reversed() // Spotify suele mostrar los más recientes primero
                    _favoritePlaylist.value = LocalResource.Success(sortedList)
                }
            }
        }
    }

    fun getFavoritePodcasts() {
        viewModelScope.launch {
            mainRepository.getFavoritePodcasts().collectLatest { podcasts ->
                val sortedList = podcasts.sortedByDescending { it.favoriteTime }
                _favoritePodcasts.value = LocalResource.Success(sortedList)
            }
        }
    }

    fun getCanvasSong() {
        _listCanvasSong.value = LocalResource.Loading()
        viewModelScope.launch {
            mainRepository.getCanvasSong(max = 5).collect { data ->
                _listCanvasSong.value = LocalResource.Success(data)
            }
        }
    }

    fun getLocalPlaylist() {
        _yourLocalPlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            mainRepository.getAllLocalPlaylists().collect { values ->
                _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
            }
        }
    }

    fun getDownloadedPlaylist() {
        viewModelScope.launch {
            mainRepository.getAllDownloadedPlaylist().collect { values ->
                _downloadedPlaylist.value = LocalResource.Success(values)
            }
        }
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            val localPlaylistEntity = LocalPlaylistEntity(title = title)
            mainRepository.insertLocalPlaylist(localPlaylistEntity)
            // No necesitamos hacer nada más aquí.
            // Al llamar a getLocalPlaylist, se actualizará el flow `_yourLocalPlaylist`,
            // y la función `combineAllPlaylists` se ejecutará automáticamente para
            // actualizar la lista unificada. ¡Es la magia de la programación reactiva!
            getLocalPlaylist()
        }
    }

    // ... (El resto de tus funciones como updateLikeStatus, updateLocalPlaylistTracks, etc. se mantienen igual)
    // ...
    fun updateLikeStatus(
        videoId: String,
        likeStatus: Int,
    ) {
        viewModelScope.launch {
            mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
        }
    }

    fun updateLocalPlaylistTracks(
        list: List<String>,
        id: Long,
    ) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect { values ->
                var count = 0
                values.forEach { song ->
                    if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast.makeText(getApplication(), application.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                } else {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                }
            }
        }
    }

    fun addToYouTubePlaylist(
        localPlaylistId: Long,
        youtubePlaylistId: String,
        videoId: String,
    ) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Syncing)
            mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                if (response == "STATUS_SUCCEEDED") {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        LocalPlaylistEntity.YouTubeSyncState.Synced,
                    )
                    Toast
                        .makeText(
                            getApplication(),
                            application.getString(R.string.added_to_youtube_playlist),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                    )
                    Toast.makeText(getApplication(), application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    fun deleteSong(videoId: String) {
        viewModelScope.launch {
            mainRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
            delay(500)
            getRecentlyAdded()
        }
    }
}