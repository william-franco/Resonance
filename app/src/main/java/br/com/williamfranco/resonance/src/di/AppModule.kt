package br.com.williamfranco.resonance.src.di

import br.com.williamfranco.resonance.src.data.local.ResonanceDatabase
import br.com.williamfranco.resonance.src.features.library.repositories.LibraryRepository
import br.com.williamfranco.resonance.src.features.library.repositories.LibraryRepositoryImpl
import br.com.williamfranco.resonance.src.features.library.view_models.CollectionViewModel
import br.com.williamfranco.resonance.src.features.library.view_models.CollectionViewModelImpl
import br.com.williamfranco.resonance.src.features.library.view_models.LibraryViewModel
import br.com.williamfranco.resonance.src.features.library.view_models.LibraryViewModelImpl
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModel
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModelImpl
import br.com.williamfranco.resonance.src.features.settings.repositories.SettingsRepository
import br.com.williamfranco.resonance.src.features.settings.repositories.SettingsRepositoryImpl
import br.com.williamfranco.resonance.src.features.settings.repositories.settingsDataStore
import br.com.williamfranco.resonance.src.features.settings.view_models.SettingsViewModel
import br.com.williamfranco.resonance.src.features.settings.view_models.SettingsViewModelImpl
import br.com.williamfranco.resonance.src.services.library.MediaStoreScanner
import br.com.williamfranco.resonance.src.services.playback.PlaybackConnection
import br.com.williamfranco.resonance.src.services.playback.PlaybackConnectionImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { ResonanceDatabase.build(androidContext()) }
    single { get<ResonanceDatabase>().songDao() }
    single { get<ResonanceDatabase>().playlistDao() }
    single { androidContext().settingsDataStore }

    single { MediaStoreScanner(androidContext()) }
    single<LibraryRepository> { LibraryRepositoryImpl(get(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<PlaybackConnection> { PlaybackConnectionImpl(androidContext()) }

    viewModelOf(::LibraryViewModelImpl) { bind<LibraryViewModel>() }
    viewModelOf(::CollectionViewModelImpl) { bind<CollectionViewModel>() }
    viewModelOf(::PlayerViewModelImpl) { bind<PlayerViewModel>() }
    viewModelOf(::SettingsViewModelImpl) { bind<SettingsViewModel>() }
}
