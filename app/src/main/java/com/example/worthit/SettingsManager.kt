package com.example.worthit

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
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
        val XP_KEY = intPreferencesKey("user_xp")
        val LAST_CHECK_IN_KEY = longPreferencesKey("last_check_in")
        val INTERCEPT_GOAL_LEVEL_KEY = intPreferencesKey("intercept_goal_level")
        val SAVINGS_GOAL_LEVEL_KEY = intPreferencesKey("savings_goal_level")
    }

    suspend fun saveSettings(monthlyGoal: Float, refItemName: String, refItemPrice: Float, navToHome: Boolean) {
        dataStore.edit { settings ->
            settings[MONTHLY_GOAL_KEY] = monthlyGoal
            settings[REF_ITEM_NAME_KEY] = refItemName
            settings[REF_ITEM_PRICE_KEY] = refItemPrice
            settings[NAV_TO_HOME_KEY] = navToHome
        }
    }

    suspend fun addXP(amount: Int) {
        dataStore.edit { settings ->
            settings[XP_KEY] = (settings[XP_KEY] ?: 0) + amount
        }
    }

    suspend fun resetXP() {
        dataStore.edit { settings ->
            settings[XP_KEY] = 0
            settings[INTERCEPT_GOAL_LEVEL_KEY] = 0
            settings[SAVINGS_GOAL_LEVEL_KEY] = 0
        }
    }

    suspend fun updateCheckIn(timestamp: Long) {
        dataStore.edit { settings ->
            settings[LAST_CHECK_IN_KEY] = timestamp
        }
    }

    suspend fun incrementInterceptGoal() {
        dataStore.edit { it[INTERCEPT_GOAL_LEVEL_KEY] = (it[INTERCEPT_GOAL_LEVEL_KEY] ?: 0) + 1 }
    }

    suspend fun incrementSavingsGoal() {
        dataStore.edit { it[SAVINGS_GOAL_LEVEL_KEY] = (it[SAVINGS_GOAL_LEVEL_KEY] ?: 0) + 1 }
    }

    val monthlyGoal: Flow<Float> = dataStore.data.map { it[MONTHLY_GOAL_KEY] ?: 1000f }
    val refItemName: Flow<String> = dataStore.data.map { it[REF_ITEM_NAME_KEY] ?: "支铅笔" }
    val refItemPrice: Flow<Float> = dataStore.data.map { it[REF_ITEM_PRICE_KEY] ?: 0.7f }
    val navToHome: Flow<Boolean> = dataStore.data.map { it[NAV_TO_HOME_KEY] ?: false }
    val userXP: Flow<Int> = dataStore.data.map { it[XP_KEY] ?: 0 }
    val lastCheckIn: Flow<Long> = dataStore.data.map { it[LAST_CHECK_IN_KEY] ?: 0L }
    val interceptGoalLevel: Flow<Int> = dataStore.data.map { it[INTERCEPT_GOAL_LEVEL_KEY] ?: 0 }
    val savingsGoalLevel: Flow<Int> = dataStore.data.map { it[SAVINGS_GOAL_LEVEL_KEY] ?: 0 }
}
