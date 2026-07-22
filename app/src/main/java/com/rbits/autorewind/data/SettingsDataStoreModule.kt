package com.rbits.autorewind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

const val SETTINGS_STORE_FILE_NAME = "settings.json"

@Module
@InstallIn(SingletonComponent::class)
class SettingsDataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Settings> =
        MultiProcessDataStoreFactory.create(
            serializer = SettingsSerializer,
            produceFile = { context.dataStoreFile(SETTINGS_STORE_FILE_NAME) },
            corruptionHandler = null,
        )
}