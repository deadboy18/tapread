package com.tapread.nfc.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.tapread.nfc.model.ScanResult

/**
 * Persists scanned card data to SharedPreferences using JSON.
 * All data stays on-device — no INTERNET permission.
 */
class CardStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tapread_cards", Context.MODE_PRIVATE)
    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences("tapread_settings", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create()

    // ── Card Storage ──

    fun saveScans(scans: List<ScanResult>) {
        val json = gson.toJson(scans)
        prefs.edit().putString("scans", json).apply()
    }

    fun loadScans(): List<ScanResult> {
        val json = prefs.getString("scans", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ScanResult>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearScans() {
        prefs.edit().remove("scans").apply()
    }

    fun exportJson(scans: List<ScanResult>): String {
        return gson.toJson(scans)
    }

    // ── Settings ──

    var maskPan: Boolean
        get() = settingsPrefs.getBoolean("mask_pan", true)
        set(value) { settingsPrefs.edit().putBoolean("mask_pan", value).apply() }

    var darkMode: Boolean?
        get() = if (settingsPrefs.contains("dark_mode")) settingsPrefs.getBoolean("dark_mode", false) else null
        set(value) {
            if (value != null) settingsPrefs.edit().putBoolean("dark_mode", value).apply()
            else settingsPrefs.edit().remove("dark_mode").apply()
        }
}
