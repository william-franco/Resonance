package br.com.williamfranco.resonance

import android.app.Application
import br.com.williamfranco.resonance.src.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class ResonanceApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ResonanceApplication)
            modules(appModule)
        }
    }
}
