package com.embercrown.game.game

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

lateinit var appContext: Context

actual fun createSettings(): Settings =
    SharedPreferencesSettings(appContext.getSharedPreferences("embercrown_prefs", Context.MODE_PRIVATE))
