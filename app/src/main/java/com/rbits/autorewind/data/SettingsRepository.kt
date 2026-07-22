package com.rbits.autorewind.data

import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.Serializer
import com.rbits.autorewind.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject


@Serializable
data class Settings(
    val rewindTimeMs: Long = 5_000L,
)

object SettingsSerializer : Serializer<Settings> {
    override val defaultValue = Settings()

    override suspend fun readFrom(input: InputStream): Settings =
        try {
            Json.decodeFromString<Settings>(
                input.readBytes().decodeToString()
            )
        } catch (exception: SerializationException) {
            throw CorruptionException("Unable to read settings", exception)
        }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(t)
                    .toByteArray()
            )
        }
    }
}

class SettingsRepository @Inject constructor(private val settingsStore: DataStore<Settings>) {
    val settingsFlow = settingsStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading Settings")
                emit(Settings())
            } else {
                throw exception
            }
        }

    suspend fun setRewindTime(rewindTimeMs: Long) {
        settingsStore.updateData { settings ->
            settings.copy(rewindTimeMs = rewindTimeMs)
        }
    }
}