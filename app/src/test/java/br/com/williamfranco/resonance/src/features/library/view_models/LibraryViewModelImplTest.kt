package br.com.williamfranco.resonance.src.features.library.view_models

import br.com.williamfranco.resonance.src.common.patterns.StatePattern
import br.com.williamfranco.resonance.src.fakes.FakeLibraryRepository
import br.com.williamfranco.resonance.src.fakes.FakeSettingsRepository
import br.com.williamfranco.resonance.src.fakes.song
import br.com.williamfranco.resonance.src.features.library.exceptions.LibraryException
import br.com.williamfranco.resonance.src.features.library.models.LibraryTab
import br.com.williamfranco.resonance.src.features.settings.models.SettingsModel
import br.com.williamfranco.resonance.src.services.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val songs = listOf(
        song(id = 1L, title = "Aurora", artist = "Nébula", album = "Primeiro Sinal", albumId = 1L),
        song(id = 2L, title = "Bruma", artist = "Nébula", album = "Primeiro Sinal", albumId = 1L),
        song(id = 3L, title = "Cinza", artist = "Ares", album = "Segundo Sinal", albumId = 2L, isFavorite = true),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `expoe biblioteca agrupada em faixas albuns e artistas`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.state.value as StatePattern.Success
        assertEquals(3, state.data.songs.size)
        assertEquals(2, state.data.albums.size)
        assertEquals(2, state.data.artists.size)
        assertEquals(1, state.data.favoritesCount)
    }

    @Test
    fun `busca filtra por titulo artista e album`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateQuery("nébula")
        advanceUntilIdle()

        val state = viewModel.state.value as StatePattern.Success
        assertEquals(listOf("Aurora", "Bruma"), state.data.songs.map { it.title })
        assertEquals(listOf("Nébula"), state.data.artists.map { it.name })
    }

    @Test
    fun `busca sem resultado devolve listas vazias sem marcar biblioteca vazia`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateQuery("inexistente")
        advanceUntilIdle()

        val state = viewModel.state.value as StatePattern.Success
        assertTrue(state.data.songs.isEmpty())
        assertFalse(state.data.isEmptyLibrary)
    }

    @Test
    fun `troca de aba altera apenas a aba selecionada`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectTab(LibraryTab.ALBUNS)
        advanceUntilIdle()

        val state = viewModel.state.value as StatePattern.Success
        assertEquals(LibraryTab.ALBUNS, state.data.tab)
        assertEquals(3, state.data.songs.size)
    }

    @Test
    fun `indexa a biblioteca no inicio quando a permissao ja existe`() = runTest {
        val repository = FakeLibraryRepository(initialSongs = songs)
        LibraryViewModelImpl(repository, FakeSettingsRepository())
        advanceUntilIdle()

        assertEquals(1, repository.syncCount)
        assertEquals(Constants.SHORT_TRACK_THRESHOLD_MS, repository.lastSyncMinDuration)
    }

    @Test
    fun `nao indexa a biblioteca sem permissao`() = runTest {
        val repository = FakeLibraryRepository(initialSongs = songs, permissionGranted = false)
        val viewModel = LibraryViewModelImpl(repository, FakeSettingsRepository())
        advanceUntilIdle()

        assertEquals(0, repository.syncCount)
        assertTrue(viewModel.state.value is StatePattern.Initial)
    }

    @Test
    fun `indexa a biblioteca assim que a permissao e concedida`() = runTest {
        val repository = FakeLibraryRepository(initialSongs = songs, permissionGranted = false)
        val viewModel = LibraryViewModelImpl(repository, FakeSettingsRepository())
        advanceUntilIdle()

        repository.grantPermission()
        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.state.value as StatePattern.Success
        assertTrue(state.data.hasPermission)
        assertEquals(1, repository.syncCount)
    }

    @Test
    fun `respeita a preferencia de nao ignorar faixas curtas ao indexar`() = runTest {
        val repository = FakeLibraryRepository(initialSongs = songs)
        val settings = FakeSettingsRepository(SettingsModel(ignoreShortTracks = false))
        LibraryViewModelImpl(repository, settings)
        advanceUntilIdle()

        assertEquals(0L, repository.lastSyncMinDuration)
    }

    @Test
    fun `favoritar delega ao repositorio e reflete no estado`() = runTest {
        val repository = FakeLibraryRepository(initialSongs = songs)
        val viewModel = LibraryViewModelImpl(repository, FakeSettingsRepository())
        advanceUntilIdle()

        viewModel.toggleFavorite(songId = 1L)
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.toggledFavorites)
        val state = viewModel.state.value as StatePattern.Success
        assertEquals(2, state.data.favoritesCount)
    }

    @Test
    fun `ignora criacao de playlist com nome em branco`() = runTest {
        val repository = FakeLibraryRepository(initialSongs = songs)
        val viewModel = LibraryViewModelImpl(repository, FakeSettingsRepository())
        advanceUntilIdle()

        viewModel.createPlaylist("   ")
        viewModel.createPlaylist("  Foco  ")
        advanceUntilIdle()

        assertEquals(listOf("Foco"), repository.createdPlaylists)
    }

    @Test
    fun `ignora adicao de playlist sem faixas`() = runTest {
        val repository = FakeLibraryRepository(initialSongs = songs)
        val viewModel = LibraryViewModelImpl(repository, FakeSettingsRepository())
        advanceUntilIdle()

        viewModel.addToPlaylist(playlistId = 1L, songIds = emptyList())
        viewModel.addToPlaylist(playlistId = 1L, songIds = listOf(2L))
        advanceUntilIdle()

        assertEquals(listOf(1L to listOf(2L)), repository.addedToPlaylist)
    }

    @Test
    fun `falha no sync expoe StatePattern Error`() = runTest {
        val repository = FakeLibraryRepository(
            initialSongs = songs,
            syncFailure = LibraryException("Falha simulada."),
        )
        val viewModel = LibraryViewModelImpl(repository, FakeSettingsRepository())
        advanceUntilIdle()

        val state = viewModel.state.value as StatePattern.Error
        assertEquals("Falha simulada.", state.error.message)
    }

    private fun viewModel(): LibraryViewModelImpl = LibraryViewModelImpl(
        libraryRepository = FakeLibraryRepository(initialSongs = songs),
        settingsRepository = FakeSettingsRepository(),
    )
}
