package com.example.walletapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val CURRENCY_KEY = stringPreferencesKey("app_currency")
        val DATE_FORMAT_KEY = stringPreferencesKey("app_date_format")
    }

    // Flussi che leggono continuamente i dati salvati (o i valori di default se è la prima volta)
    val themeFlow: Flow<String> = dataStore.data.map { it[THEME_KEY] ?: "System Default" }
    val currencyFlow: Flow<String> = dataStore.data.map { it[CURRENCY_KEY] ?: "€ (Euro)" }
    val dateFormatFlow: Flow<String> = dataStore.data.map { it[DATE_FORMAT_KEY] ?: "DD/MM/YYYY" }

    // Funzioni per scrivere e sovrascrivere i dati
    suspend fun saveTheme(theme: String) { dataStore.edit { it[THEME_KEY] = theme } }
    suspend fun saveCurrency(currency: String) { dataStore.edit { it[CURRENCY_KEY] = currency } }
    suspend fun saveDateFormat(format: String) { dataStore.edit { it[DATE_FORMAT_KEY] = format } }
}