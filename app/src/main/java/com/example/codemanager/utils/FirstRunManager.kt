package com.example.codemanager.utils

import android.content.Context

class FirstRunManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun isFirstRun(): Boolean {
        // Devuelve true si es la primera vez (valor por defecto true)
        return prefs.getBoolean("is_first_run", true)
    }

    fun markAsFinished() {
        // Guarda que ya no es la primera vez
        prefs.edit().putBoolean("is_first_run", false).apply()
    }
}