package com.aushadh.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BluePrimary = Color(0xFF1976D2)
val GreenSuccess = Color(0xFF388E3C)
val WhiteBg = Color(0xFFF5F5F5)

@Composable
fun AushadhTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BluePrimary,
            secondary = GreenSuccess,
            background = WhiteBg,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White
        ),
        typography = Typography(
            headlineLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            )
        ),
        content = content
    )
}
