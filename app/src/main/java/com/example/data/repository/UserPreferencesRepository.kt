package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "revenue_pilot_prefs")

data class UserPreferences(
    val supervisorName: String = "মোহাম্মদ জাহিদ হাসান",
    val wardInfo: String = "ওয়ার্ড-০৭, মিরপুর জোন",
    val biometricLockEnabled: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val SUPERVISOR_NAME = stringPreferencesKey("supervisor_name")
        val WARD_INFO = stringPreferencesKey("ward_info")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            supervisorName = prefs[Keys.SUPERVISOR_NAME] ?: "মোহাম্মদ জাহিদ হাসান",
            wardInfo = prefs[Keys.WARD_INFO] ?: "ওয়ার্ড-০৭, মিরপুর জোন",
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false
        )
    }

    suspend fun updateSupervisorProfile(name: String, ward: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPERVISOR_NAME] = name.ifBlank { "মোহাম্মদ জাহিদ হাসান" }
            prefs[Keys.WARD_INFO] = ward.ifBlank { "ওয়ার্ড-০৭, মিরপুর জোন" }
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }
}
