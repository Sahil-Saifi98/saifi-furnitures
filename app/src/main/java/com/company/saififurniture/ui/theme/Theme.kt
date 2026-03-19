package com.saififurnitures.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WoodColorScheme = lightColorScheme(
    primary             = WoodMid,
    onPrimary           = Color.White,
    primaryContainer    = WoodCream,
    onPrimaryContainer  = WoodDark,
    secondary           = AccentGold,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFFFF3DC),
    background          = BgMain,
    surface             = BgCard,
    onBackground        = TextDark,
    onSurface           = TextDark,
    outline             = WoodWarm,
    error               = AccentRed
)

@Composable
fun SaifiFurnituresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WoodColorScheme,
        content = content
    )
}