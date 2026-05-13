package nl.rvt.gatas.companion

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// Usage suggestions:
// - AviationBlue: Primary buttons, interactive elements
// - AviationDarkBlue: Headers, navigation bar
// - AviationLightGrey: Backgrounds, cards
// - AviationDarkGrey: Text, borders
// - AviationWarning: Caution indicators
// - AviationSuccess: Confirmation states
// - AviationError: Error messages, destructive actions

// 🎨 Colors
private val AviationBlue = Color(0xFF0A84FF)       // Bright sky blue
private val AviationDarkBlue = Color(0xFF0047AB)    // Deep navy blue
private val AviationLightGrey = Color(0xFFF0F4F8)   // Very light cool grey
private val AviationDarkGrey = Color(0xFF1C1C1E)    // Near-black grey
private val AviationWarning = Color(0xFFFFC107)     // Amber/yellow
private val AviationSuccess = Color(0xFF4CAF50)     // Green
private val AviationError = Color(0xFFCF6679)       // Soft red/pink

private val LightColors = lightColorScheme(
    primary = AviationBlue,
    onPrimary = Color.White,
    primaryContainer = AviationLightGrey,
    onPrimaryContainer = AviationDarkBlue,
    secondary = AviationDarkBlue,
    onSecondary = Color.White,
    background = AviationLightGrey,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = AviationError,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = AviationDarkBlue,
    onPrimary = Color.White,
    primaryContainer = AviationDarkGrey,
    onPrimaryContainer = AviationBlue,
    secondary = AviationBlue,
    onSecondary = Color.Black,
    background = AviationDarkGrey,
    onBackground = Color.White,
    surface = AviationDarkGrey,
    onSurface = Color.White,
    error = AviationError,
    onError = Color.Black
)

// 🔤 Typography
private val AviationTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

// 🟰 Shapes
private val AviationShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

// 🎨🚀 Full App Theme
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AviationTypography,
        shapes = AviationShapes,
        content = content
    )
}
