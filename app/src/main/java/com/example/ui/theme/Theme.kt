package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TacticalDarkColorScheme = darkColorScheme(
    primary = WarGold,
    onPrimary = ObsidianNight,
    primaryContainer = WarGoldContainer,
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = CyberCyan,
    onSecondary = ObsidianNight,
    secondaryContainer = CyberCyanContainer,
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = SpellViolet,
    onTertiary = Color.White,
    tertiaryContainer = SpellVioletContainer,
    onTertiaryContainer = Color(0xFFE9D5FF),
    error = ThreatCrimson,
    onError = Color.White,
    errorContainer = ThreatCrimsonContainer,
    onErrorContainer = Color(0xFFFECACA),
    background = ObsidianNight,
    onBackground = TextPrimary,
    surface = SlateCommand,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalPanel,
    onSurfaceVariant = TextSecondary,
    outline = SteelBorder,
    outlineVariant = Color(0xFF1E293B)
)

private val TacticalLightColorScheme = lightColorScheme(
    primary = WarGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = Color(0xFF7E22CE),
    onTertiary = Color.White,
    background = LightSurface,
    onBackground = LightText,
    surface = Color.White,
    onSurface = LightText,
    surfaceVariant = LightPanel,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

val WarStratShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WarStratTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalDarkColorScheme else TacticalLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WarStratTypography,
        shapes = WarStratShapes,
        content = content
    )
}
