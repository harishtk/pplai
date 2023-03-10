package com.aiavatar.app.core.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aiavatar.app.commons.util.secure.SecurityUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DataStoreUtil @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val security: SecurityUtil
) {
    private val securityKeyAlias = "ai-avatar-secure-store"
    private val bytesToStringSeparator = "|"

    private val json by lazy { Json { encodeDefaults = true } }

    fun getData() = dataStore.data
        .map { preferences ->
            preferences[DATA].orEmpty()
        }

    suspend fun setData(value: String) {
        dataStore.edit {
            it[DATA] = value
        }
    }

    fun getSecuredData() = dataStore.data
        .secureMap<String> { preferences ->
            preferences[SECURED_DATA].orEmpty()
        }

    suspend fun setSecuredData(value: String) {
        dataStore.secureEdit(value) { prefs, encryptedValue ->
            prefs[SECURED_DATA] = encryptedValue
        }
    }

    suspend fun hasKey(key: Preferences.Key<*>) = dataStore.edit { it.contains(key) }

    suspend fun clearDataStore() {
        dataStore.edit { it.clear() }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T> Flow<Preferences>.secureMap(crossinline fetchValue: (value: Preferences) -> String): Flow<T> {
        return map {
            val decryptedValue = security.decryptData(
                securityKeyAlias,
                fetchValue(it).split(bytesToStringSeparator).map { it.toByte() }.toByteArray()
            )
            json.decodeFromString(decryptedValue)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> DataStore<Preferences>.secureEdit(
        value: T,
        crossinline editStore: (MutablePreferences, String) -> Unit
    ) {
        edit {
            val encryptedValue =
                security.encryptData(securityKeyAlias, Json.encodeToString(value))
            editStore.invoke(it, encryptedValue.joinToString(bytesToStringSeparator))
        }
    }

    companion object {
        val DATA = stringPreferencesKey("data")
        val SECURED_DATA = stringPreferencesKey("secured_data")
    }

}