package com.example.walletapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Crea l'istanza del database delle impostazioni
private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(context: Context) {
    private val dataStore = context.dataStore

    // Le "chiavi" con cui si salvano
    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
        val COLOR_PALETTE_KEY = stringPreferencesKey("app_color_palette")
        val CURRENCY_KEY = stringPreferencesKey("app_currency")
        val DATE_FORMAT_KEY = stringPreferencesKey("app_date_format")
        val HIDE_TOTAL_BALANCE = booleanPreferencesKey("hide_total_balance")
        val HIDDEN_ACCOUNTS = stringSetPreferencesKey("hidden_accounts")
    }

    // Flussi che leggono continuamente i dati salvati (o i valori di default se è la prima volta)
    val themeFlow: Flow<String> = dataStore.data.map { it[THEME_KEY] ?: "System Default" }
    val colorPaletteFlow: Flow<String> = dataStore.data.map { it[COLOR_PALETTE_KEY] ?: "Dynamic" }
    val currencyFlow: Flow<String> = dataStore.data.map { it[CURRENCY_KEY] ?: "€ (Euro)" }
    val dateFormatFlow: Flow<String> = dataStore.data.map { it[DATE_FORMAT_KEY] ?: "DD/MM/YYYY" }
    val hideTotalBalanceFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HIDE_TOTAL_BALANCE] ?: false // Default: eye opened (false)
    }
    val hiddenAccountsFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[HIDDEN_ACCOUNTS] ?: emptySet() // Default: no account is hidden
    }

    // Funzioni per scrivere e sovrascrivere i dati
    suspend fun saveTheme(theme: String) { dataStore.edit { it[THEME_KEY] = theme } }
    suspend fun saveColorPalette(palette: String) { dataStore.edit { it[COLOR_PALETTE_KEY] = palette } }
    suspend fun saveCurrency(currency: String) { dataStore.edit { it[CURRENCY_KEY] = currency } }
    suspend fun saveDateFormat(format: String) { dataStore.edit { it[DATE_FORMAT_KEY] = format } }

    suspend fun saveHideTotalBalance(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIDE_TOTAL_BALANCE] = hide
        }
    }

    suspend fun saveHiddenAccounts(accounts: Set<String>) {
        dataStore.edit { preferences ->
            preferences[HIDDEN_ACCOUNTS] = accounts
        }
    }
}