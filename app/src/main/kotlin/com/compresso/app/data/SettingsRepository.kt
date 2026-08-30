package com.compresso.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.compresso.app.data.model.CompressionMode
import com.compresso.app.data.model.CompressionSettings
import com.compresso.app.data.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "compresso_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val PRESERVE_METADATA = booleanPreferencesKey("preserve_metadata")
        val ALLOW_CONVERSION = booleanPreferencesKey("allow_conversion")
        val DELETE_ORIGINALS = booleanPreferencesKey("delete_originals")
        val THEME = stringPreferencesKey("theme")
    }

    val settingsFlow: Flow<CompressionSettings> = context.dataStore.data.map { prefs ->
        CompressionSettings(
            mode = prefs[Keys.MODE]?.let { value ->
                runCatching { CompressionMode.valueOf(value) }.getOrNull()
            } ?: CompressionMode.STANDARD,
            preserveMetadata = prefs[Keys.PRESERVE_METADATA] ?: true,
            allowFormatConversion = prefs[Keys.ALLOW_CONVERSION] ?: true,
            deleteOriginalsWhenDone = prefs[Keys.DELETE_ORIGINALS] ?: false,
            theme = prefs[Keys.THEME]?.let { value ->
                runCatching { ThemePreference.valueOf(value) }.getOrNull()
            } ?: ThemePreference.SYSTEM
        )
    }

    suspend fun setMode(mode: CompressionMode) {
        context.dataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setPreserveMetadata(value: Boolean) {
        context.dataStore.edit { it[Keys.PRESERVE_METADATA] = value }
    }

    suspend fun setAllowFormatConversion(value: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_CONVERSION] = value }
    }

    suspend fun setDeleteOriginalsWhenDone(value: Boolean) {
        context.dataStore.edit { it[Keys.DELETE_ORIGINALS] = value }
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }
}
