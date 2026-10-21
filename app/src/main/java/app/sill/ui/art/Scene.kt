package app.sill.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import app.sill.data.Section
import java.time.LocalDateTime
import kotlin.math.sin
import kotlin.random.Random

/** What stands in one pot: the plant, and whether the soil is dark with compost. */
@Immutable
data class PotView(val look: PlantLook, val fertilized: Boolean)

private val Pots = listOf(
    Color(0xFFC0673F), Color(0xFFD9A77E), Color(0xFF4F7FA8), Color(0xFFEDE4D3), Color(0xFF5E8A62),
)

private val Soil = Color(0xFF4A3426)
private val Frame = Color(0xFFF5F1E8)
private val Wall = Color(0xFFF1E9DA)

/** A pot standing on [bottom], centred on [cx]; returns the middle of its soil, where a plant grows from. */
fun DrawScope.drawPot(cx: Float, bottom: Float, w: Float, h: Float, style: Int, fertilized: Boolean): Offset {
    val c = Pots[Math.floorMod(style, Pots.size)]
    drawOval(Color(0x33000000), Offset(cx - w * 0.42f, bottom - h * 0.05f), Size(w * 0.84f, h * 0.12f))
    val rimH = h * 0.22f
    val top = bottom - h
    val body = Path().apply {
        moveTo(cx - w * 0.45f, top + rimH)
        lineTo(cx + w * 0.45f, top + rimH)
        lineTo(cx + w * 0.34f, bottom)
        lineTo(cx - w * 0.34f, bottom)
        close()
    }
    drawPath(body, c)
    drawPath(Path().apply {
        moveTo(cx + w * 0.18f, top + rimH); lineTo(cx + w * 0.45f, top + rimH); lineTo(cx + w * 0.34f, bottom); lineTo(cx + w * 0.12f, bottom); close()
    }, c.darker(0.12f))
    drawLine(c.lighter(0.25f), Offset(cx - w * 0.33f, top + rimH * 1.3f), Offset(cx - w * 0.27f, bottom - h * 0.08f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
    drawRect(Color(0x22000000), Offset(cx - w * 0.45f, top + rimH), Size(w * 0.9f, h * 0.04f))
    drawRoundRect(c.lighter(0.06f), Offset(cx - w / 2, top), Size(w, rimH), CornerRadius(w * 0.03f))
    drawRect(c.darker(0.1f), Offset(cx + w * 0.22f, top), Size(w * 0.28f, rimH))
    if (style == 3) drawRect(Color(0xFF3F6F9E), Offset(cx - w / 2, top + rimH * 0.15f), Size(w, rimH * 0.14f))
    val soil = if (fertilized) Soil.darker(0.3f) else Soil
    drawOval(soil, Offset(cx - w * 0.43f, top - h * 0.035f), Size(w * 0.86f, h * 0.14f))
    if (fertilized) {
        val r = Random(style + 7)
        repeat(9) { drawCircle(Color(0xFF8A6B3E), w * 0.012f, Offset(cx + r.nextFloat() * w * 0.7f - w * 0.35f, top + r.nextFloat() * h * 0.05f - h * 0.01f)) }
    }
    return Offset(cx, top + h * 0.02f)
}

private enum class Season { WINTER, SPRING, SUMMER, AUTUMN }

private fun season(month: Int) = when (month) {
    12, 1, 2 -> Season.WINTER
    3, 4, 5 -> Season.SPRING
    6, 7, 8 -> Season.SUMMER
    else -> Season.AUTUMN
}

/** 0 by day, 1 deep at night, in between at dawn and dusk. */
private fun night(hour: Int) = when (hour) {
    in 9..17 -> 0f
    7, 8 -> 0.25f
    18, 19 -> 0.3f
    20 -> 0.6f
    6 -> 0.55f
    else -> 1f
}

/** The view through the glass: sky, hills and the garden of everything that took. */
private fun DrawScope.outside(glass: Rect, garden: List<PlantLook>, hour: Int, month: Int, sway: Float, lift: Float = 0f) {
    val n = night(hour)
    val dusk = hour in 17..20 || hour in 5..7
    val skyTop = Color(0xFF8CC0E6).mix(Color(0xFF1B2440), n)
    val skyLow = (if (dusk) Color(0xFFF2B48A) else Color(0xFFDDEFF6)).mix(Color(0xFF34436A), n * 0.9f)
    drawRect(Brush.verticalGradient(listOf(skyTop, skyLow), glass.top, glass.bottom - glass.height * 0.3f), glass.topLeft, glass.size)
    val r = Random(3)
    if (n > 0.5f) {
        repeat(40) { drawCircle(Color.White.copy(alpha = 0.4f + r.nextFloat() * 0.5f), 1.2f + r.nextFloat() * 1.5f, Offset(glass.left + r.nextFloat() * glass.width, glass.top + r.nextFloat() * glass.height * 0.5f)) }
        drawCircle(Color(0xFFF4EED8), glass.width * 0.045f, Offset(glass.left + glass.width * 0.78f, glass.top + glass.height * 0.16f))
    } else {
        drawCircle(Color(0x66FFF6D8), glass.width * 0.1f, Offset(glass.left + glass.width * 0.2f, glass.top + glass.height * 0.14f))
        drawCircle(Color(0xFFFFF4D2), glass.width * 0.045f, Offset(glass.left + glass.width * 0.2f, glass.top + glass.height * 0.14f))
        repeat(2) { k ->
            val cx = glass.left + glass.width * (0.45f + k * 0.3f) + sin(sway) * 4f
            val cy = glass.top + glass.height * (0.1f + k * 0.08f)
            for (j in 0..2) drawCircle(Color(0xCCFFFFFF), glass.width * (0.04f + j % 2 * 0.015f), Offset(cx + j * glass.width * 0.04f, cy))
        }
    }
    val s = season(month)
    val horizon = glass.top + glass.height * 0.55f
    val hill = when (s) {
        Season.WINTER -> Color(0xFFDCE3E6)
        Season.AUTUMN -> Color(0xFFB9A46A)
        else -> Color(0xFF9DBB8C)
    }.mix(Color(0xFF243052), n * 0.7f)
    drawPath(Path().apply {
        moveTo(glass.left, horizon)
        quadraticTo(glass.left + glass.width * 0.25f, horizon - glass.height * 0.12f, glass.left + glass.width * 0.55f, horizon - glass.height * 0.03f)
        quadraticTo(glass.left + glass.width * 0.8f, horizon - glass.height * 0.1f, glass.right, horizon - glass.height * 0.02f)
        lineTo(glass.right, glass.bottom); lineTo(glass.left, glass.bottom); close()
    }, hill)
    val crowns = when (s) {
        Season.WINTER -> Color(0xFF8E959A)
        Season.AUTUMN -> Color(0xFFD08A3A)
        Season.SPRING -> Color(0xFF7FAE5E)
        Season.SUMMER -> Color(0xFF5E8F4A)
    }.mix(Color(0xFF1E2A44), n * 0.7f)
    repeat(9) { k ->
        val x = glass.left + glass.width * (0.04f + k * 0.12f) + r.nextFloat() * 8f
        val y = horizon - glass.height * (0.04f + r.nextFloat() * 0.05f)
        drawLine(Color(0xFF6B5A48).mix(Color(0xFF1E2A44), n * 0.7f), Offset(x, y + glass.height * 0.05f), Offset(x, y), strokeWidth = 2f)
        drawCircle(crowns, glass.width * (0.025f + r.nextFloat() * 0.015f), Offset(x, y))
    }
    val meadowTop = horizon + glass.height * 0.04f
    val meadow = when (s) {
        Season.WINTER -> Color(0xFFF1F4F5)
        Season.AUTUMN -> Color(0xFFC2A65A)
        Season.SPRING -> Color(0xFF93C25E)
        Season.SUMMER -> Color(0xFF80AE4E)
    }.mix(Color(0xFF26304C), n * 0.7f)
    drawRect(Brush.verticalGradient(listOf(meadow.lighter(0.06f), meadow.darker(0.08f)), meadowTop, glass.bottom), Offset(glass.left, meadowTop), Size(glass.width, glass.bottom - meadowTop))
    // The garden: everything that took, in two rows, the newest in front.
    val back = garden.filterIndexed { i, _ -> i % 2 == 1 }
    val front = garden.filterIndexed { i, _ -> i % 2 == 0 }
    fun row(list: List<PlantLook>, y: Float, h: Float, shift: Float) {
        if (list.isEmpty()) return
        val step = glass.width / (list.size + 1)
        list.forEachIndexed { i, look ->
            val x = glass.left + step * (i + 1) + shift * step
            drawPlant(look, Offset(x, y), h, sin(sway + i) * 0.03f)
        }
    }
    clipRect(glass.left, glass.top, glass.right, glass.bottom) {
        row(back, meadowTop + glass.height * 0.1f, glass.height * 0.2f, 0.35f)
        row(front, glass.bottom - glass.height * (0.04f + lift), glass.height * 0.28f, 0f)
        if (s != Season.WINTER) repeat(26) {
            val x = glass.left + r.nextFloat() * glass.width
            val y = meadowTop + glass.height * 0.04f + r.nextFloat() * (glass.bottom - meadowTop)
            drawLine(meadow.darker(0.2f), Offset(x, y), Offset(x + 2f, y - 6f), strokeWidth = 1.5f)
        }
        if (s == Season.WINTER) repeat(30) { drawCircle(Color.White.copy(alpha = 0.85f), 1.5f + r.nextFloat() * 1.5f, Offset(glass.left + r.nextFloat() * glass.width, glass.top + r.nextFloat() * glass.height)) }
    }
    if (n > 0f) drawRect(Color(0xFF141C33).copy(alpha = n * 0.35f), glass.topLeft, glass.size)
}

private class Layout(w: Float, h: Float) {
    val sillY = h * 0.8f
    val window = Rect(w * 0.06f, h * 0.02f, w * 0.94f, sillY)
    val ft = w * 0.034f
    val glass = Rect(window.left + ft, window.top + ft, window.right - ft, window.bottom - ft * 0.4f)
    val potW = w * 0.135f
    val potH = h * 0.135f
    val potBottom = sillY + h * 0.03f
    val plantH = h * 0.42f
    fun potX(i: Int, w: Float) = w * (0.105f + i * 0.163f)
    val vaseX = w * 0.905f
}

/**
 * The home picture: a window onto the garden, the sill under it with its five pots, a vase of
 * dried flowers and, if the wind brought one, a seed packet lying on the sill.
 */
@Composable
fun SillScene(
    pots: List<PotView?>,
    garden: List<PlantLook>,
    dried: List<PlantLook>,
    windPacket: Section?,
    modifier: Modifier = Modifier,
    onPot: (Int) -> Unit = {},
    onVase: () -> Unit = {},
    onWind: () -> Unit = {},
) {
    val sway by rememberSway()
    val clock = LocalDateTime.now()
    val hour = Art.hour ?: clock.hour
    val month = clock.monthValue
    Canvas(
        modifier.clipToBounds().pointerInput(pots, dried.size, windPacket) {
            detectTapGestures { p ->
                val l = Layout(size.width.toFloat(), size.height.toFloat())
                val w = size.width.toFloat()
                if (p.y > l.sillY - l.plantH && p.y < l.potBottom + l.potH * 0.3f) {
                    if (p.x > w * 0.84f) {
                        if (windPacket != null && p.y > l.sillY - l.potH * 0.2f) onWind() else if (dried.isNotEmpty()) onVase() else if (windPacket != null) onWind()
                        return@detectTapGestures
                    }
                    val i = (0 until 5).minBy { kotlin.math.abs(l.potX(it, w) - p.x) }
                    if (kotlin.math.abs(l.potX(i, w) - p.x) < w * 0.085f) onPot(i)
                }
            }
        },
    ) {
        val l = Layout(size.width, size.height)
        drawRect(Brush.verticalGradient(listOf(Wall.darker(0.03f), Wall, Wall.darker(0.05f))), Offset.Zero, size)
        val ph = sway
        outside(l.glass, garden, hour, month, ph, lift = 0.16f)
        // The frame, painted many times.
        val win = l.window
        val ft = l.ft
        drawRect(Color(0x22000000), Offset(win.left + ft * 0.3f, win.top + ft * 0.3f), Size(win.width, win.height))
        drawRect(Frame, Offset(win.left, win.top), Size(win.width, ft))
        drawRect(Frame, Offset(win.left, win.top), Size(ft, win.height))
        drawRect(Frame, Offset(win.right - ft, win.top), Size(ft, win.height))
        val mid = (win.left + win.right) / 2
        drawRect(Frame, Offset(mid - ft * 0.35f, win.top), Size(ft * 0.7f, win.height))
        val transom = l.glass.top + l.glass.height * 0.34f
        drawRect(Frame, Offset(win.left, transom - ft * 0.3f), Size(win.width, ft * 0.6f))
        drawRect(Color(0x1A000000), Offset(win.left + ft, win.top + ft), Size(win.width - 2 * ft, ft * 0.18f))
        drawLine(Color(0x22000000), Offset(mid + ft * 0.35f, transom + ft * 0.3f), Offset(mid + ft * 0.35f, win.bottom), strokeWidth = 2f)
        drawCircle(Color(0xFFB7A27A), ft * 0.22f, Offset(mid + ft * 0.9f, (transom + win.bottom) / 2))
        // Light on the glass.
        drawPath(Path().apply {
            moveTo(l.glass.left + l.glass.width * 0.08f, l.glass.bottom); lineTo(l.glass.left + l.glass.width * 0.2f, l.glass.top + l.glass.height * 0.38f)
            lineTo(l.glass.left + l.glass.width * 0.26f, l.glass.top + l.glass.height * 0.38f); lineTo(l.glass.left + l.glass.width * 0.14f, l.glass.bottom); close()
        }, Color(0x18FFFFFF))
        // The sill: its top, then its front edge.
        val sillTop = l.sillY - size.height * 0.005f
        drawRect(Color(0xFFEDE6D8), Offset(0f, sillTop), Size(size.width, size.height * 0.05f))
        drawRect(Color(0xFFD9CFBD), Offset(0f, sillTop + size.height * 0.05f), Size(size.width, size.height * 0.03f))
        drawRect(Brush.verticalGradient(listOf(Color(0x33000000), Color(0x00000000)), sillTop + size.height * 0.08f, sillTop + size.height * 0.14f), Offset(0f, sillTop + size.height * 0.08f), Size(size.width, size.height * 0.06f))
        if (night(hour) < 0.5f) drawPath(Path().apply {
            moveTo(size.width * 0.15f, sillTop); lineTo(size.width * 0.62f, sillTop); lineTo(size.width * 0.52f, sillTop + size.height * 0.05f); lineTo(size.width * 0.02f, sillTop + size.height * 0.05f); close()
        }, Color(0x33FFF3C8))
        // The vase of dried flowers.
        if (dried.isNotEmpty()) {
            val vx = l.vaseX
            val vb = l.potBottom - size.height * 0.005f
            val vh = l.potH * 0.95f
            dried.take(5).forEachIndexed { i, look ->
                val a = (i - (minOf(dried.size, 5) - 1) / 2f) * 0.22f + sin(ph + i) * 0.01f
                val top = Offset(vx, vb - vh * 0.5f).toward(a, l.plantH * 0.42f)
                drawLine(Color(0xFFA48A5C), Offset(vx, vb - vh * 0.3f), top, strokeWidth = 2.5f, cap = StrokeCap.Round)
                val head = bloomOf(look.kind).mix(Color(0xFF9B7E57), 0.55f)
                repeat(6) { k -> drawCircle(head, l.potW * 0.035f, top.toward(k * Art.TAU / 6, l.potW * 0.04f)) }
                drawCircle(Color(0xFF7D6440), l.potW * 0.03f, top)
            }
            val vw = l.potW * 0.55f
            drawRoundRect(Color(0x55BFD8D6), Offset(vx - vw / 2, vb - vh * 0.75f), Size(vw, vh * 0.75f), CornerRadius(vw * 0.35f))
            drawRoundRect(Color(0x88E6F2F1), Offset(vx - vw / 2, vb - vh * 0.75f), Size(vw, vh * 0.75f), CornerRadius(vw * 0.35f), style = Stroke(2f))
            drawRect(Color(0x339FC4C0), Offset(vx - vw * 0.4f, vb - vh * 0.3f), Size(vw * 0.8f, vh * 0.25f))
        }
        // The five pots, back to front.
        pots.forEachIndexed { i, pv ->
            val x = l.potX(i, size.width)
            val soil = drawPot(x, l.potBottom, l.potW, l.potH, i, pv?.fertilized == true)
            if (pv != null) drawPlant(pv.look, soil, l.plantH, sin(ph + i * 1.3f) * 0.035f)
        }
        // A packet the wind left on the sill.
        if (windPacket != null) {
            val pw = l.potW * 0.5f
            val pc = Offset(size.width * 0.9f, l.potBottom - size.height * 0.01f)
            rotate(-14f, pc) {
                drawRect(Color(0x33000000), Offset(pc.x - pw / 2 + 3f, pc.y - pw * 0.65f + 3f), Size(pw, pw * 1.3f))
                drawRect(Color(0xFFF6EEDC), Offset(pc.x - pw / 2, pc.y - pw * 0.65f), Size(pw, pw * 1.3f))
                drawRect(Color(windPacket.ink), Offset(pc.x - pw / 2, pc.y - pw * 0.65f), Size(pw, pw * 0.35f))
                drawCircle(Color(windPacket.ink).copy(alpha = 0.7f), pw * 0.22f, Offset(pc.x, pc.y + pw * 0.15f))
            }
        }
    }
}

/** One pot close up on a strip of sill against the wall, for a plant's page. [drops] 0..1 rains water from above. */
@Composable
fun PotPortrait(view: PotView, style: Int, modifier: Modifier = Modifier, drops: Float = 0f) {
    val sway by rememberSway()
    Canvas(modifier.clipToBounds()) {
        drawRect(Brush.verticalGradient(listOf(Wall.darker(0.02f), Wall.darker(0.06f))), Offset.Zero, size)
        drawRect(Brush.radialGradient(listOf(Color(0x40FFF3C8), Color(0x00FFF3C8)), Offset(size.width * 0.3f, size.height * 0.3f), size.width * 0.7f), Offset.Zero, size)
        val sillY = size.height * 0.84f
        drawRect(Color(0xFFEDE6D8), Offset(0f, sillY), Size(size.width, size.height * 0.08f))
        drawRect(Color(0xFFD9CFBD), Offset(0f, sillY + size.height * 0.08f), Size(size.width, size.height * 0.08f))
        val potW = size.height * 0.3f
        val potH = size.height * 0.27f
        val soil = drawPot(size.width / 2, sillY + size.height * 0.05f, potW, potH, style, view.fertilized)
        drawPlant(view.look, soil, size.height * 0.62f, sin(sway) * 0.03f)
        if (drops > 0f && drops < 1f) {
            val r = Random(9)
            repeat(14) {
                val x = size.width / 2 + (r.nextFloat() - 0.5f) * potW * 0.8f
                val start = r.nextFloat() * 0.5f
                val t = ((drops - start * 0.5f) / 0.6f).coerceIn(0f, 1f)
                if (t > 0f && t < 1f) {
                    val y = soil.y - size.height * 0.7f + t * size.height * 0.7f
                    drawOval(Color(0xCC5FA3CC), Offset(x - 3f, y - 7f), Size(6f, 11f))
                }
            }
        }
    }
}

/** The garden out of the window, wide and without the frame. */
@Composable
fun GardenView(garden: List<PlantLook>, modifier: Modifier = Modifier) {
    val sway by rememberSway()
    val clock = LocalDateTime.now()
    Canvas(modifier.clipToBounds()) {
        outside(Rect(Offset.Zero, size), garden, Art.hour ?: clock.hour, clock.monthValue, sway)
    }
}

/** A sheet of herbarium paper with one pressed plant taped to it. */
@Composable
fun HerbariumSheet(look: PlantLook, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFFF4EEDF), Offset.Zero, size)
        val r = Random(look.shape)
        repeat(30) { drawCircle(Color(0x0F6B5A3A), 1.5f + r.nextFloat() * 2f, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height)) }
        drawRect(Color(0x22000000), Offset.Zero, size, style = Stroke(1.5f))
        val base = Offset(size.width / 2, size.height * 0.9f)
        drawPlant(look, base, size.height * 0.8f)
        rotate(-8f, Offset(base.x, base.y - size.height * 0.08f)) {
            drawRect(Color(0xAAE9DFC2), Offset(base.x - size.width * 0.14f, base.y - size.height * 0.1f), Size(size.width * 0.28f, size.height * 0.05f))
        }
        rotate(6f, Offset(base.x, base.y - size.height * 0.4f)) {
            drawRect(Color(0x88E9DFC2), Offset(base.x - size.width * 0.12f, base.y - size.height * 0.42f), Size(size.width * 0.24f, size.height * 0.045f))
        }
    }
}
