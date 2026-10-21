package app.sill.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.sill.BuildConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Shared bits of the drawn world. */
object Art {
    /** Screenshots and renders want a still picture: no sway, the light at ten in the morning. */
    var still: Boolean = BuildConfig.SHOTS

    /** The hour the window shows; null follows the clock. */
    val hour: Int? get() = if (still) 10 else null

    const val TAU = (2 * PI).toFloat()
}

/** A slow 0..TAU phase for leaves moving in the air from the open window; 0 when still. */
@Composable
fun rememberSway(periodMs: Int = 7000): State<Float> {
    if (Art.still) return remember { mutableFloatStateOf(0f) }
    val t = rememberInfiniteTransition(label = "sway")
    return t.animateFloat(0f, Art.TAU, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "phase")
}

/** A point [len] away from this one, at [angle] radians from straight up, clockwise. */
fun Offset.toward(angle: Float, len: Float) = Offset(x + sin(angle) * len, y - cos(angle) * len)

fun Color.mix(other: Color, t: Float) = lerp(this, other, t.coerceIn(0f, 1f))
fun Color.darker(t: Float) = mix(Color.Black, t)
fun Color.lighter(t: Float) = mix(Color.White, t)

/** Flower colours, as in an old seed catalogue. */
val Blooms = listOf(
    Color(0xFFD9475B), Color(0xFFF08A3C), Color(0xFFF2C53D), Color(0xFFF4EFE4),
    Color(0xFF8A5BB8), Color(0xFF5C8FD6), Color(0xFFE77FA6), Color(0xFFC23B2E),
)

fun bloomOf(hash: Int) = Blooms[Math.floorMod(hash, Blooms.size)]
