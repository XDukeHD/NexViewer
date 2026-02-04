package com.xduke.nexviewer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val IP_ADDRESS_KEY = stringPreferencesKey("ip_address")
        val PORT_KEY = stringPreferencesKey("port")
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    val getIpAddress: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[IP_ADDRESS_KEY]
        }

    val getPort: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PORT_KEY]
        }

    val getAuthToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }

    suspend fun saveConnectionDetails(ip: String, port: String) {
        context.dataStore.edit { preferences ->
            preferences[IP_ADDRESS_KEY] = ip
            preferences[PORT_KEY] = port
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    suspend fun clearConnectionDetails() {
        context.dataStore.edit { preferences ->
            preferences.remove(IP_ADDRESS_KEY)
            preferences.remove(PORT_KEY)
            preferences.remove(AUTH_TOKEN_KEY)
        }
    }
}
