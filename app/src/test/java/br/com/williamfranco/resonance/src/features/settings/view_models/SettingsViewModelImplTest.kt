package br.com.williamfranco.resonance.src.features.settings.view_models

import br.com.williamfranco.resonance.src.fakes.FakeLibraryRepository
import br.com.williamfranco.resonance.src.fakes.FakeSettingsRepository
import br.com.williamfranco.resonance.src.fakes.song
import br.com.williamfranco.resonance.src.features.settings.models.ColorSource
import br.com.williamfranco.resonance.src.features.settings.models.ThemeMode
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
class SettingsViewModelImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var settings: FakeSettingsRepository
    private lateinit var library: FakeLibraryRepository
    private lateinit var viewModel: SettingsViewModelImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settings = FakeSettingsRepository()
        library = FakeLibraryRepository(initialSongs = listOf(song(1L), song(2L)))
        viewModel = SettingsViewModelImpl(settings, library)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `publica os valores padrao ao iniciar`() = runTest {
        advanceUntilIdle()

        val current = viewModel.uiState.value.settings
        assertEquals(ThemeMode.SISTEMA, current.themeMode)
        assertEquals(ColorSource.CAPA_DO_ALBUM, current.colorSource)
        assertFalse(current.crossfadeEnabled)
        assertTrue(current.ignoreShortTracks)
    }

    @Test
    fun `persiste tema e origem das cores`() = runTest {
        advanceUntilIdle()

        viewModel.updateThemeMode(ThemeMode.ESCURO)
        viewModel.updateColorSource(ColorSource.MATERIAL_YOU)
        advanceUntilIdle()

        assertEquals(ThemeMode.ESCURO, settings.current.themeMode)
        assertEquals(ColorSource.MATERIAL_YOU, viewModel.uiState.value.settings.colorSource)
    }

    @Test
    fun `crossfade em zero segundos so existe quando desligado`() = runTest {
        advanceUntilIdle()

        viewModel.updateCrossfadeSeconds(6)
        advanceUntilIdle()
        assertEquals(0L, viewModel.uiState.value.settings.crossfadeMs)

        viewModel.updateCrossfadeEnabled(true)
        advanceUntilIdle()
        assertEquals(6_000L, viewModel.uiState.value.settings.crossfadeMs)
    }

    @Test
    fun `duracao do crossfade fica dentro dos limites`() = runTest {
        advanceUntilIdle()

        viewModel.updateCrossfadeSeconds(99)
        advanceUntilIdle()
        assertEquals(12, viewModel.uiState.value.settings.crossfadeSeconds)

        viewModel.updateCrossfadeSeconds(0)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.settings.crossfadeSeconds)
    }

    @Test
    fun `reindexar usa o limiar de faixas curtas e informa o total`() = runTest {
        advanceUntilIdle()

        viewModel.rescanLibrary()
        advanceUntilIdle()

        assertEquals(1, library.syncCount)
        assertEquals(Constants.SHORT_TRACK_THRESHOLD_MS, library.lastSyncMinDuration)
        assertFalse(viewModel.uiState.value.isScanning)
        assertEquals(2, viewModel.uiState.value.lastScanCount)
    }

    @Test
    fun `reindexar sem ignorar faixas curtas nao aplica limiar`() = runTest {
        advanceUntilIdle()

        viewModel.updateIgnoreShortTracks(false)
        advanceUntilIdle()
        viewModel.rescanLibrary()
        advanceUntilIdle()

        assertEquals(0L, library.lastSyncMinDuration)
    }
}
