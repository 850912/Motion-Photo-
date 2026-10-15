package com.motionphoto.converter.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material 3 Expressive (MD3E) 情感化高饱和度色彩系统
 * 提供 4 套富有张力的主题色板 + Android 12+ 动态取色支持
 */
enum class ExpressivePalette(
    val title: String,
    val subtitle: String,
    val seedColor: Color,
    val accentPreview: Color
) {
    IRIS_VIOLET(
        title = "流光紫韵",
        subtitle = "MD3 Expressive 标志色",
        seedColor = Color(0xFF5B43D6),
        accentPreview = Color(0xFFFFB1C8)
    ),
    PIXEL_MINT(
        title = "薄荷森境",
        subtitle = "Pixel 灵动翠绿",
        seedColor = Color(0xFF006D43),
        accentPreview = Color(0xFF75F8B3)
    ),
    SUNSET_CORAL(
        title = "暮光珊瑚",
        subtitle = "温暖高对比活力色",
        seedColor = Color(0xFF9C3D28),
        accentPreview = Color(0xFFFFB59D)
    ),
    CYBER_OCEAN(
        title = "深海电光",
        subtitle = "清澈冰蓝科技感",
        seedColor = Color(0xFF006493),
        accentPreview = Color(0xFF8ECDFF)
    )
}

// 1. IRIS VIOLET (MD3 Expressive Default)
val IrisLightScheme = lightColorScheme(
    primary = Color(0xFF563CD0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF170065),
    secondary = Color(0xFF7B4E7D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD6FA),
    onSecondaryContainer = Color(0xFF300935),
    tertiary = Color(0xFF984061),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3E001D),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE5E0EC),
    onSurfaceVariant = Color(0xFF47464F),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC9C5D0),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF3EFF7),
    inversePrimary = Color(0xFFC8BFFF)
)

val IrisDarkScheme = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF280096),
    primaryContainer = Color(0xFF3E1EB8),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = Color(0xFFEB9CEB),
    onSecondary = Color(0xFF48204C),
    secondaryContainer = Color(0xFF613764),
    onSecondaryContainer = Color(0xFFFFD6FA),
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF5E1133),
    tertiaryContainer = Color(0xFF7B2949),
    onTertiaryContainer = Color(0xFFFFD9E2),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF35343D),
    onSurfaceVariant = Color(0xFFC9C5D0),
    outline = Color(0xFF928F99),
    outlineVariant = Color(0xFF47464F),
    inverseSurface = Color(0xFFE5E1E9),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF563CD0)
)

// 2. PIXEL MINT
val MintLightScheme = lightColorScheme(
    primary = Color(0xFF006D43),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF92F7BC),
    onPrimaryContainer = Color(0xFF002111),
    secondary = Color(0xFF4E6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D6),
    onSecondaryContainer = Color(0xFF0B1F14),
    tertiary = Color(0xFF3B6471),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFE9F8),
    onTertiaryContainer = Color(0xFF001F27),
    background = Color(0xFFF5FBF5),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF5FBF5),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDBE5DD),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707972)
)

val MintDarkScheme = darkColorScheme(
    primary = Color(0xFF76DAA1),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005231),
    onPrimaryContainer = Color(0xFF92F7BC),
    secondary = Color(0xFFB5CCBA),
    onSecondary = Color(0xFF203528),
    secondaryContainer = Color(0xFF374B3E),
    onSecondaryContainer = Color(0xFFD0E8D6),
    tertiary = Color(0xFFA3CDDB),
    onTertiary = Color(0xFF043541),
    tertiaryContainer = Color(0xFF224C58),
    onTertiaryContainer = Color(0xFFBFE9F8),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF303933),
    onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF89938C)
)

// 3. SUNSET CORAL
val CoralLightScheme = lightColorScheme(
    primary = Color(0xFF98402B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD2),
    onPrimaryContainer = Color(0xFF3A0A01),
    secondary = Color(0xFF77574F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD2),
    onSecondaryContainer = Color(0xFF2C1510),
    tertiary = Color(0xFF6D5D2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7E1A6),
    onTertiaryContainer = Color(0xFF241A00),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DDD8),
    onSurfaceVariant = Color(0xFF534340),
    outline = Color(0xFF85736F)
)

val CoralDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB4A3),
    onPrimary = Color(0xFF5B1606),
    primaryContainer = Color(0xFF7A2916),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFFE7BDB3),
    onSecondary = Color(0xFF442A23),
    secondaryContainer = Color(0xFF5D4038),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFDAC58C),
    onTertiary = Color(0xFF3B2F05),
    tertiaryContainer = Color(0xFF534519),
    onTertiaryContainer = Color(0xFFF7E1A6),
    background = Color(0xFF1A110F),
    onBackground = Color(0xFFF1DFDC),
    surface = Color(0xFF1A110F),
    onSurface = Color(0xFFF1DFDC),
    surfaceVariant = Color(0xFF3D2E2B),
    onSurfaceVariant = Color(0xFFD8C2BD),
    outline = Color(0xFFA08C88)
)

// 4. CYBER OCEAN
val OceanLightScheme = lightColorScheme(
    primary = Color(0xFF006493),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCAE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF50606F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E5F5),
    onSecondaryContainer = Color(0xFF0C1D29),
    tertiary = Color(0xFF65587B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF201634),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF72787E)
)

val OceanDarkScheme = darkColorScheme(
    primary = Color(0xFF8ECDFF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFCAE6FF),
    secondary = Color(0xFFB7C9D9),
    onSecondary = Color(0xFF22323F),
    secondaryContainer = Color(0xFF384956),
    onSecondaryContainer = Color(0xFFD3E5F5),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF362B4A),
    tertiaryContainer = Color(0xFF4D4162),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE0E2E8),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE0E2E8),
    surfaceVariant = Color(0xFF31363C),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198)
)

fun ExpressivePalette.colorScheme(darkTheme: Boolean): ColorScheme = when (this) {
    ExpressivePalette.IRIS_VIOLET -> if (darkTheme) IrisDarkScheme else IrisLightScheme
    ExpressivePalette.PIXEL_MINT -> if (darkTheme) MintDarkScheme else MintLightScheme
    ExpressivePalette.SUNSET_CORAL -> if (darkTheme) CoralDarkScheme else CoralLightScheme
    ExpressivePalette.CYBER_OCEAN -> if (darkTheme) OceanDarkScheme else OceanLightScheme
}
