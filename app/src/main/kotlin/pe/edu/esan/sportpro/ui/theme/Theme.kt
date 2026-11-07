package pe.edu.esan.sportpro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Esquema de colores para SportPro.
 * Tema con paleta verde de fútbol.
 */
@Composable
fun SportProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("appearance",Context.MODE_PRIVATE) }
    var mode by remember { mutableStateOf(prefs.getString("theme","system")) }
    var font by remember { mutableFloatStateOf(prefs.getFloat("font",1f)) }
    DisposableEffect(prefs) {
        val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,_ ->
            mode=prefs.getString("theme","system");font=prefs.getFloat("font",1f)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val useDark=when(mode){"light"->false;"dark"->true;else->darkTheme}
    val baseDensity=LocalDensity.current
    val chosenFont=font.takeIf { it in listOf(1f,1.3f,1.5f) }?:1f
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            if (useDark) DarkColorScheme else LightColorScheme
        }
    }

    SideEffect { (context as? Activity)?.window?.let { window ->
        window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))
        androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {
            isAppearanceLightStatusBars=!useDark
            isAppearanceLightNavigationBars=!useDark
        }
    } }
    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density,if(chosenFont==1f)baseDensity.fontScale else max(baseDensity.fontScale,chosenFont))) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(28.dp)),
        content = content
    )
    }
}
