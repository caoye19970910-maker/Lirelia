package com.yugentech.quill.di.modules.core

import androidx.room.Room
import com.yugentech.quill.database.database.AppDatabase
import com.yugentech.quill.database.database.MIGRATION_1_2
import com.yugentech.quill.database.database.MIGRATION_2_3
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Database bindings required by Lirelia's first stable reading loop only.
 *
 * The schema is left intact for safe upstream comparison and migration, while
 * unused cloud/AI DAOs are no longer exposed through the runtime container.
 */
val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "quill_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single { get<AppDatabase>().bookDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().readingSessionDao() }
    single { get<AppDatabase>().bookIndexingStateDao() }
    single { get<AppDatabase>().highlightDao() }
}
