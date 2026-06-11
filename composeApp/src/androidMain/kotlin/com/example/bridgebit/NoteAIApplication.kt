package com.example.bridgebit

import android.app.Application
import com.example.bridgebit.core.di.androidModule
import com.example.bridgebit.core.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * Android Application class
 * 
 * Entry point untuk inisialisasi app-wide dependencies.
 */
class NoteAIApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Koin DI
        initKoin(
            platformModules = listOf(androidModule)
        ) {
            androidLogger()
            androidContext(this@NoteAIApplication)
        }
    }

    companion object {
        /** Singleton instance untuk akses ApplicationContext di luar Composable scope. */
        lateinit var instance: NoteAIApplication
            private set
    }
}
