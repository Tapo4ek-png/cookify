package com.voidd.cookify.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

// Цвета для светлой темы
private val md_theme_light_primary = Color(0xFF6750A4)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFE9DDFF)
private val md_theme_light_onPrimaryContainer = Color(0xFF22005D)

// Цвета для темной темы
private val md_theme_dark_primary = Color(0xFFCFBCFF)
private val md_theme_dark_onPrimary = Color(0xFF381E72)
private val md_theme_dark_primaryContainer = Color(0xFF4F378B)
private val md_theme_dark_onPrimaryContainer = Color(0xFFE9DDFF)

// Светлая цветовая схема, использующая определенные выше цвета
private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
)

// Темная цветовая схема, использующая определенные выше цвета
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
)

// Формы (Shapes) для темы (пока используются значения по умолчанию)
private val CookifyShapes = Shapes()

/**
 * Основная тема приложения Cookify.
 *
 * @param darkTheme Флаг, указывающий, должна ли применяться темная тема. По умолчанию используется системная настройка.
 * @param dynamicColor Флаг, указывающий, должны ли использоваться динамические цвета (на поддерживаемых устройствах).
 * @param content Composable-контент, к которому будет применена тема.
 */
@Composable
fun CookifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Выбор цветовой схемы в зависимости от настроек и возможностей устройства
    val colorScheme = when {
        // Использование динамических цветов на Android 12+ (API 31+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Использование статичной темной или светлой схемы
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Эффект для настройки системных баров (статус-бара и навигационной панели)
    LaunchedEffect(darkTheme) {
        val window = (context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.getInsetsController(it, it.decorView).apply {
                // Установка светлого/темного внешнего вида системных баров в зависимости от темы
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Применение темы Material Design 3 с выбранной цветовой схемой, типографикой и формами
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Предполагается, что Typography определена в другом файле
        shapes = CookifyShapes,
        content = content
    )
}