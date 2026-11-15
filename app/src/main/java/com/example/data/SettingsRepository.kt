package com.example.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.domDeckDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "domdeck_settings"
)

data class Ps1Settings(
    val folderUri: String? = null,
    val folderLabel: String? = null,
    val emulatorPackage: String? = null,
    val lastScanTimestamp: Long? = null
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val PS1_FOLDER_URI = stringPreferencesKey("ps1_folder_uri")
        val PS1_FOLDER_LABEL = stringPreferencesKey("ps1_folder_label")
        val PS1_EMULATOR_PACKAGE = stringPreferencesKey("ps1_emulator_package")
        val PS1_LAST_SCAN_AT = longPreferencesKey("ps1_last_scan_at")
    }

    val ps1Settings: Flow<Ps1Settings> = context.domDeckDataStore.data.map { prefs ->
        Ps1Settings(
            folderUri = prefs[Keys.PS1_FOLDER_URI],
            folderLabel = prefs[Keys.PS1_FOLDER_LABEL],
            emulatorPackage = prefs[Keys.PS1_EMULATOR_PACKAGE],
            lastScanTimestamp = prefs[Keys.PS1_LAST_SCAN_AT]
        )
    }

    suspend fun savePs1Folder(uri: String, label: String) {
        context.domDeckDataStore.edit { prefs ->
            prefs[Keys.PS1_FOLDER_URI] = uri
            prefs[Keys.PS1_FOLDER_LABEL] = label
        }
    }

    suspend fun clearPs1Folder() {
        context.domDeckDataStore.edit { prefs ->
            prefs.remove(Keys.PS1_FOLDER_URI)
            prefs.remove(Keys.PS1_FOLDER_LABEL)
        }
    }

    suspend fun savePs1EmulatorPackage(packageName: String?) {
        context.domDeckDataStore.edit { prefs ->
            if (packageName.isNullOrBlank()) {
                prefs.remove(Keys.PS1_EMULATOR_PACKAGE)
            } else {
                prefs[Keys.PS1_EMULATOR_PACKAGE] = packageName
            }
        }
    }

    suspend fun updatePs1LastScanTimestamp(timestampMillis: Long) {
        context.domDeckDataStore.edit { prefs ->
            prefs[Keys.PS1_LAST_SCAN_AT] = timestampMillis
        }
    }

    fun hasPersistedReadPermission(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val targetUri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == targetUri && permission.isReadPermission
        }
    }
}
