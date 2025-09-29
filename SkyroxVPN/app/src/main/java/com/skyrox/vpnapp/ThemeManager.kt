package com.skyrox.vpnapp.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

object ThemeManager {
    private lateinit var preferences: SharedPreferences

    var isDarkTheme by mutableStateOf(false) // ✅ Управляем темой глобально

    private val observers = mutableStateListOf<() -> Unit>() // ✅ Список наблюдателей

    fun init(context: Context) {
        preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        isDarkTheme = preferences.getBoolean("dark_mode", false) // ✅ Загружаем сохранённую тему
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        preferences.edit().putBoolean("dark_mode", isDarkTheme).apply() // ✅ Сохраняем тему
        notifyObservers() // ✅ Оповещаем UI
    }

    fun addObserver(observer: () -> Unit) {
        observers.add(observer)
    }

    private fun notifyObservers() {
        observers.forEach { it() }
    }

    // ✅ Цвета тем
    val primary: Color
        get() = if (isDarkTheme) Color(0xFF4C0084) else Color(0xFF73BAE6)

    val text: Color
        get() = if (isDarkTheme) Color.White else Color.Black

    val netralText: Color
        get() = if (isDarkTheme) Color.LightGray else Color.Gray

    val netral: Color
        get() = if (isDarkTheme) Color.LightGray else Color.Gray

    val background: Color
        get() = if (isDarkTheme) Color(0xFF121212) else Color(0xFFCDE6ED)

    val surface: Color
        get() = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White

    val onBackground: Color
        get() = if (isDarkTheme) Color.White else Color.Black

    val onSurface: Color
        get() = if (isDarkTheme) Color.LightGray else Color.DarkGray

    val card: Color
        get() = if (isDarkTheme) Color.DarkGray else Color(0xFFF8F8F8)

    val border: Color
        get() = if (isDarkTheme) Color(0xFF006400) else Color(0xFF73BAE6)

    val inBoxArrow: Color
        get() = if (isDarkTheme) Color(0xFFB455FC) else Color(0xFFCDE6ED)

    val secondary: Color
        get() = if (isDarkTheme) Color(0xFF444444) else Color(0xFFCCCCCC)
}
