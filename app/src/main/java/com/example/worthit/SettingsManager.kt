
package com.example.worthit

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val MONTHLY_GOAL_KEY = floatPreferencesKey("monthly_goal")
        val REF_ITEM_NAME_KEY = stringPreferencesKey("ref_item_name")
        val REF_ITEM_PRICE_KEY = floatPreferencesKey("ref_item_price")
        val NAV_TO_HOME_KEY = booleanPreferencesKey("nav_to_home")
    }

    suspend fun saveSettings(monthlyGoal: Float, refItemName: String, refItemPrice: Float, navToHome: Boolean) {
        dataStore.edit { settings ->
            settings[MONTHLY_GOAL_KEY] = monthlyGoal
            settings[REF_ITEM_NAME_KEY] = refItemName
            settings[REF_ITEM_PRICE_KEY] = refItemPrice
            settings[NAV_TO_HOME_KEY] = navToHome
        }
    }

    val monthlyGoal: Flow<Float> = dataStore.data.map { preferences ->
        preferences[MONTHLY_GOAL_KEY] ?: 1000f
    }

    val refItemName: Flow<String> = dataStore.data.map { preferences ->
        preferences[REF_ITEM_NAME_KEY] ?: "支铅笔"
    }

    val refItemPrice: Flow<Float> = dataStore.data.map { preferences ->
        preferences[REF_ITEM_PRICE_KEY] ?: 0.7f
    }
    
    val navToHome: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NAV_TO_HOME_KEY] ?: false
    }
}
