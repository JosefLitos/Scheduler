package cz.kepis.scheduler.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class Settings {
    companion object {
        private val removeAfterDoneKey = booleanPreferencesKey("removeAfterDone")
        private val startBaseKey = intPreferencesKey("startBase")
        private val untilBaseKey = intPreferencesKey("untilBase")
        private val typeKey = intPreferencesKey("type")
        private val notifyMissedKey = booleanPreferencesKey("notifyMissed")

        private lateinit var dataStore: DataStore<Preferences>
        var removeAfterDone = false
            private set

        suspend fun setRemoveAfterDone(value: Boolean) {
            removeAfterDone = value
            dataStore.edit { it[removeAfterDoneKey] = value }
        }

        var startBase = 0
            private set

        suspend fun setStartBase(value: Int) {
            startBase = value
            dataStore.edit { it[startBaseKey] = value }
        }

        var untilBase = 0
            private set

        suspend fun setUntilBase(value: Int) {
            untilBase = value
            dataStore.edit { it[untilBaseKey] = value }
        }

        var type = 0
            private set

        suspend fun setType(value: Int) {
            type = value
            dataStore.edit { it[typeKey] = value }
        }

        var notifyMissed = false
            private set

        suspend fun setNotifyMissed(value: Boolean) {
            notifyMissed = value
            dataStore.edit { it[notifyMissedKey] = value }
        }

        suspend fun loadSettings(context: Context) {
            if (::dataStore.isInitialized) return

            dataStore = PreferenceDataStoreFactory.create {
                context.applicationContext.preferencesDataStoreFile("settings")
            }
            removeAfterDone =
                dataStore.data.map { it[removeAfterDoneKey] ?: removeAfterDone }.first()
            startBase = dataStore.data.map { it[startBaseKey] ?: startBase }.first()
            untilBase = dataStore.data.map { it[untilBaseKey] ?: untilBase }.first()
            type = dataStore.data.map { it[typeKey] ?: type }.first()
            notifyMissed =
                dataStore.data.map { it[notifyMissedKey] ?: notifyMissed }.first()
        }
    }
}