package com.yugentech.quill.di

import android.app.Application
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
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * Lirelia's local-first application container.
 *
 * Startup intentionally contains only the modules needed for local EPUB
 * import, library storage, appearance and reading. Cloud sync, accounts,
 * billing, remote catalogs, AI indexing and background workers are not part
 * of the clean baseline runtime.
 */
class QuillApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@QuillApp)

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
}
