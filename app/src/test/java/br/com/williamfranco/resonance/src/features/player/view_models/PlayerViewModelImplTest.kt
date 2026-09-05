package br.com.williamfranco.resonance.src.features.player.view_models

import br.com.williamfranco.resonance.src.fakes.FakeLibraryRepository
import br.com.williamfranco.resonance.src.fakes.FakePlaybackConnection
import br.com.williamfranco.resonance.src.fakes.song
import br.com.williamfranco.resonance.src.services.playback.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val songs = listOf(
        song(id = 1L, title = "Aurora"),
        song(id = 2L, title = "Bruma"),
        song(id = 3L, title = "Cinza"),
    )

    private lateinit var connection: FakePlaybackConnection
    private lateinit var library: FakeLibraryRepository
    private lateinit var viewModel: PlayerViewModelImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        connection = FakePlaybackConnection()
        library = FakeLibraryRepository(initialSongs = songs)
        viewModel = PlayerViewModelImpl(connection, library)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tocar fila publica faixa atual e indice`() = runTest {
        val job = observeState()

        viewModel.play(songs, startIndex = 1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Bruma", state.currentSong?.title)
        assertEquals(1, state.queueIndex)
        assertEquals(3, state.queue.size)
        assertTrue(state.isPlaying)

        job.cancel()
    }

    @Test
    fun `aleatorio liga o modo shuffle`() = runTest {
        val job = observeState()

        viewModel.shuffle(songs)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.shuffleEnabled)

        viewModel.toggleShuffle()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.shuffleEnabled)

        job.cancel()
    }

    @Test
    fun `repeticao circula entre desligado todas e uma`() = runTest {
        val job = observeState()

        assertEquals(RepeatMode.DESLIGADO, viewModel.state.value.repeatMode)

        viewModel.cycleRepeat()
        advanceUntilIdle()
        assertEquals(RepeatMode.TODAS, viewModel.state.value.repeatMode)

        viewModel.cycleRepeat()
        advanceUntilIdle()
        assertEquals(RepeatMode.UMA, viewModel.state.value.repeatMode)

        viewModel.cycleRepeat()
        advanceUntilIdle()
        assertEquals(RepeatMode.DESLIGADO, viewModel.state.value.repeatMode)

        job.cancel()
    }

    @Test
    fun `anterior reinicia a faixa quando ja passou dos tres segundos`() = runTest {
        val job = observeState()

        viewModel.play(songs, startIndex = 1)
        connection.emitPosition(10_000L)
        advanceUntilIdle()

        viewModel.previous()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.queueIndex)
        assertEquals(0L, state.positionMs)

        job.cancel()
    }

    @Test
    fun `anterior volta de faixa no comeco da reproducao`() = runTest {
        val job = observeState()

        viewModel.play(songs, startIndex = 1)
        advanceUntilIdle()

        viewModel.previous()
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.queueIndex)

        job.cancel()
    }

    @Test
    fun `mover item reordena a fila`() = runTest {
        val job = observeState()

        viewModel.play(songs, startIndex = 0)
        viewModel.moveQueueItem(from = 0, to = 2)
        advanceUntilIdle()

        assertEquals(listOf("Bruma", "Cinza", "Aurora"), viewModel.state.value.queue.map { it.title })

        job.cancel()
    }

    @Test
    fun `remover item tira a faixa da fila`() = runTest {
        val job = observeState()

        viewModel.play(songs, startIndex = 0)
        viewModel.removeFromQueue(index = 1)
        advanceUntilIdle()

        assertEquals(listOf("Aurora", "Cinza"), viewModel.state.value.queue.map { it.title })

        job.cancel()
    }

    @Test
    fun `favorito da faixa atual vem da biblioteca e nao da fila`() = runTest {
        val job = observeState()

        viewModel.play(songs, startIndex = 0)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.currentSong?.isFavorite ?: true)

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(listOf(1L), library.toggledFavorites)
        assertTrue(viewModel.state.value.currentSong?.isFavorite ?: false)

        job.cancel()
    }

    @Test
    fun `expandir e ignorado quando nada esta tocando`() = runTest {
        val job = observeState()

        viewModel.expand()
        advanceUntilIdle()

        assertNull(viewModel.state.value.currentSong)
        assertFalse(viewModel.isExpanded.value)

        viewModel.play(songs, startIndex = 0)
        advanceUntilIdle()
        viewModel.expand()

        assertTrue(viewModel.isExpanded.value)

        viewModel.collapse()
        assertFalse(viewModel.isExpanded.value)

        job.cancel()
    }

    /** O `state` usa `WhileSubscribed`, então precisa de um coletor ativo durante o teste. */
    private fun TestScope.observeState(): Job = backgroundScope.launch { viewModel.state.collect {} }
}
