package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ThemeManager handles light, dark, and system mode toggling while applying the
 * specific design specification color palette:
 * - Primary Brand: #4F46E5 (Indigo 600)
 * - Dark Heading / Slate: #0F172A (Slate 900)
 * - Body Text / Muted: #475569 (Slate 600)
 * - Secondary / Badge Accent: #6366F1 (Indigo 500)
 * - Badge / Container Background: #EEF2FF (Indigo 50)
 * - Border / Divider: #E2E8F0 (Slate 200)
 * - Light Background Start: #F8FAFC (Slate 50)
 * - Light Background End: #F1F5F9 (Slate 100)
 */
class ThemeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadInitialTheme())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    companion object {
        private const val PREFS_NAME = "wphub_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"

        // Design Specs Primary Palette
        val COLOR_PRIMARY = Color(0xFF4F46E5)       // #4F46E5
        val COLOR_PRIMARY_DARK = Color(0xFF4338CA)  // #4338CA
        val COLOR_TEXT_DARK = Color(0xFF0F172A)     // #0F172A
        val COLOR_TEXT_BODY = Color(0xFF475569)     // #475569
        val COLOR_BADGE_ACCENT = Color(0xFF6366F1)  // #6366F1
        val COLOR_BADGE_BG = Color(0xFFEEF2FF)      // #EEF2FF
        val COLOR_BORDER_SLATE = Color(0xFFE2E8F0)  // #E2E8F0
        val COLOR_BG_START = Color(0xFFF8FAFC)      // #F8FAFC
        val COLOR_BG_END = Color(0xFFF1F5F9)        // #F1F5F9

        // Semantic Colors
        val COLOR_SUCCESS = Color(0xFF10B981)
        val COLOR_SUCCESS_BG = Color(0xFFD1FAE5)
        val COLOR_WARNING = Color(0xFFF59E0B)
        val COLOR_WARNING_BG = Color(0xFFFEF3C7)
        val COLOR_ERROR = Color(0xFFEF4444)
        val COLOR_ERROR_BG = Color(0xFFFEE2E2)

        // Dark Palette Mappings
        val COLOR_DARK_SURFACE = Color(0xFF0F172A)  // #0F172A
        val COLOR_DARK_BG = Color(0xFF0A0F1D)       // Slate 950
        val COLOR_DARK_CARD = Color(0xFF1E293B)     // Slate 800
        val COLOR_DARK_BORDER = Color(0xFF334155)   // Slate 700

        @Volatile
        private var INSTANCE: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getLightColorScheme(): ColorScheme = lightColorScheme(
            primary = COLOR_PRIMARY,
            onPrimary = Color.White,
            primaryContainer = COLOR_BADGE_BG,
            onPrimaryContainer = COLOR_PRIMARY_DARK,
            secondary = COLOR_BADGE_ACCENT,
            onSecondary = Color.White,
            secondaryContainer = COLOR_BADGE_BG,
            onSecondaryContainer = COLOR_PRIMARY_DARK,
            background = COLOR_BG_START,
            onBackground = COLOR_TEXT_DARK,
            surface = Color.White,
            onSurface = COLOR_TEXT_DARK,
            surfaceVariant = COLOR_BG_END,
            onSurfaceVariant = COLOR_TEXT_BODY,
            outline = COLOR_BORDER_SLATE,
            outlineVariant = Color(0xFFF1F5F9),
            error = COLOR_ERROR,
            onError = Color.White,
            errorContainer = COLOR_ERROR_BG,
            onErrorContainer = Color(0xFF991B1B)
        )

        fun getDarkColorScheme(): ColorScheme = darkColorScheme(
            primary = COLOR_PRIMARY,
            onPrimary = Color.White,
            primaryContainer = COLOR_PRIMARY_DARK,
            onPrimaryContainer = Color.White,
            secondary = COLOR_BADGE_ACCENT,
            onSecondary = Color.White,
            secondaryContainer = COLOR_DARK_CARD,
            onSecondaryContainer = Color(0xFFE2E8F0),
            background = COLOR_DARK_BG,
            onBackground = Color(0xFFF8FAFC),
            surface = COLOR_DARK_SURFACE,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = COLOR_DARK_CARD,
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = COLOR_DARK_BORDER,
            outlineVariant = COLOR_DARK_CARD,
            error = COLOR_ERROR,
            onError = Color.White,
            errorContainer = Color(0xFF4C1D24),
            onErrorContainer = Color(0xFFFFD1D6)
        )
    }

    private fun loadInitialTheme(): AppThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.LIGHT.name)
        } catch (e: Exception) {
            AppThemeMode.LIGHT
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun toggleThemeMode(): AppThemeMode {
        val next = when (_themeMode.value) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.SYSTEM
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
        }
        setThemeMode(next)
        return next
    }

    fun isDarkActive(context: Context, mode: AppThemeMode = _themeMode.value): Boolean {
        return when (mode) {
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
            AppThemeMode.SYSTEM -> {
                val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
