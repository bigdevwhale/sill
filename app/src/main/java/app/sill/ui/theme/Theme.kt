package app.sill.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sill.R

/**
 * A morning windowsill: limewashed wall, a painted wooden frame, terracotta pots, green leaves,
 * and seed packets printed like old lithographs. Only the daylight look.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** Terracotta: the one thing to do. */
    val clay: Color,
    val claySoft: Color,
    val onClay: Color,
    /** Watering. */
    val water: Color,
    val waterSoft: Color,
    val leaf: Color,
    val leafSoft: Color,
    /** Packet paper and its print. */
    val paper: Color,
    val print: Color,
    val wood: Color,
    val woodDark: Color,
    val soil: Color,
    val sticky: Color,
)

val Morning = Palette(
    bg = Color(0xFFF1E9DA),
    surface = Color(0xFFFBF6EC),
    surface2 = Color(0xFFEDE3D0),
    line = Color(0xFFDCCDB2),
    ink = Color(0xFF2E2921),
    muted = Color(0xFF6B604F),
    faint = Color(0xFF9E917B),
    clay = Color(0xFFB65A36),
    claySoft = Color(0xFFF2DCCB),
    onClay = Color(0xFFFFF8F0),
    water = Color(0xFF3F7EA3),
    waterSoft = Color(0xFFDCE9EF),
    leaf = Color(0xFF4C7A35),
    leafSoft = Color(0xFFE1EAD2),
    paper = Color(0xFFF6EEDC),
    print = Color(0xFF2C2620),
    wood = Color(0xFFC9A57A),
    woodDark = Color(0xFF8E6A45),
    soil = Color(0xFF4A3426),
    sticky = Color(0xFFFFF1B8),
)

val LocalPalette = staticCompositionLocalOf { Morning }

/** Forum: the capitals printed on a seed packet. */
val Print = FontFamily(Font(R.font.forum_regular, FontWeight.Normal))

/** Lora: names and headings, a little old-fashioned. */
val Serif = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Manrope: plain and clear, for everything around the sill. */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Caveat: the lines written after a watering. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

/** PT Mono: the small print on the back of a packet. */
val Mono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

object S {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(24.dp)
    val RLg = RoundedCornerShape(18.dp)
    val RMd = RoundedCornerShape(14.dp)
    val RSm = RoundedCornerShape(10.dp)
}

object SType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 38.sp, lineHeight = 44.sp, color = S.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 27.sp, lineHeight = 33.sp, color = S.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 25.sp, color = S.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Print, fontSize = 13.sp, letterSpacing = 0.2.em, color = S.c.clay)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = S.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, color = S.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = S.c.faint)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val Hand: TextStyle @Composable get() = TextStyle(fontFamily = app.sill.ui.theme.Hand, fontSize = 23.sp, lineHeight = 26.sp, color = S.c.ink)
    val Typed: TextStyle @Composable get() = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 18.sp, color = S.c.print)
}

@Composable
fun SillTheme(content: @Composable () -> Unit) {
    val p = Morning
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.clay,
                onPrimary = p.onClay,
                primaryContainer = p.claySoft,
                onPrimaryContainer = p.ink,
                secondary = p.leaf,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = SType.Body,
                bodyMedium = SType.Body.copy(fontSize = 15.sp),
                labelLarge = SType.Button,
            ),
            content = content,
        )
    }
}
