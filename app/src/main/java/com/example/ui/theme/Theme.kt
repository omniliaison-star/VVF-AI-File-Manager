package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = FrostedPrimaryDark,
    secondary = FrostedSecondaryDark,
    background = FrostedBgDark,
    surface = FrostedSurfaceDark,
    surfaceVariant = FrostedSurfaceVariantDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = FrostedTextDark,
    onSurface = FrostedTextDark,
    onSurfaceVariant = FrostedTextDark.copy(alpha = 0.6f),
    outline = FrostedBorderDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = FrostedPrimaryLight,
    secondary = FrostedSecondaryLight,
    background = FrostedBgLight,
    surface = FrostedSurfaceLight,
    surfaceVariant = FrostedSurfaceVariantLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = FrostedTextLight,
    onSurface = FrostedTextLight,
    onSurfaceVariant = FrostedTextLight.copy(alpha = 0.6f),
    outline = FrostedBorderLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the premium Frosted Glass styling
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
