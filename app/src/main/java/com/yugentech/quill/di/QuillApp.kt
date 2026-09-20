package com.yugentech.quill.di

import android.app.Application
import androidx.work.Configuration
import com.yugentech.quill.BuildConfig
import com.yugentech.quill.di.modules.books.booksModule
import com.yugentech.quill.di.modules.books.sourcesModule
import com.yugentech.quill.di.modules.config.categoryModule
import com.yugentech.quill.di.modules.config.themeModule
import com.yugentech.quill.di.modules.core.dataStoreModule
import com.yugentech.quill.di.modules.core.databaseModule
import com.yugentech.quill.di.modules.shared.readerModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * Lirelia's local-first application container.
 *
 * The original Quill project wires Firebase, account, billing, cloud sync,
 * Gutenberg and Aira at startup. Lirelia deliberately starts only the
 * modules required for local EPUB import, library storage and reading.
 */
class QuillApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@QuillApp)
            workManagerFactory()

            modules(
                databaseModule,
                dataStoreModule,
                themeModule,
                categoryModule,
                booksModule,
                sourcesModule,
                readerModule
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}
