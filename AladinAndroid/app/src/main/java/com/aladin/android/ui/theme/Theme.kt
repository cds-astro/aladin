@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport", "MagicNumber", "MaxLineLength", "ComplexCondition", "CyclomaticComplexMethod", "LongMethod", "ReturnCount", "LongParameterList", "SwallowedException", "TooGenericExceptionCaught", "NestedBlockDepth", "FunctionNaming", "LoopWithTooManyJumpStatements", "TooManyFunctions")
package com.aladin.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AladinBlue,
    secondary = AladinGold,
    background = SkyBlack,
    surface = SkyNight,
    onPrimary = TextPrimary,
    onSecondary = SkyBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun AladinTheme(content: @Composable () -> Unit) {
    // Edge-to-edge mode is enabled in MainActivity (enableEdgeToEdge); system
    // bars are transparent over the dark sky background.
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content,
    )
}
