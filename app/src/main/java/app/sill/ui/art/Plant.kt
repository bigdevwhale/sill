package app.sill.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.sill.data.Section
import app.sill.domain.Stage
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/**
 * How to draw one plant. [kind] (the seed's hash) picks the colours and the variety, the same for
 * everyone who sows that seed; [shape] (the planting's own number) bends its stems and places its
 * leaves, so no two are alike. [growth] runs 0..1 to the bloom, [droop] 0..1 with thirst.
 * [pressed] draws it flat and faded, as in a herbarium.
 */
data class PlantLook(
    val section: Section,
    val kind: Int,
    val shape: Long,
    val growth: Float,
    val stage: Stage,
    val droop: Float = 0f,
    val pressed: Boolean = false,
)

private val Dry = Color(0xFFB59B57)
private val Pressed = Color(0xFF8C8155)
private val Trunk = Color(0xFF6B4A33)
private val Bamboo = Color(0xFFC6AB6B)
private val Cream = Color(0xFFF3EAD6)

private class Ink(look: PlantLook) {
    private val base = Color(look.section.leaf).let {
        val v = (Math.floorMod(look.kind ushr 3, 5) - 2) * 0.06f
        if (v > 0) it.lighter(v) else it.darker(-v)
    }

    private fun aged(c: Color, droop: Float, pressed: Boolean) = c.mix(Dry, droop * 0.55f).let { if (pressed) it.mix(Pressed, 0.32f) else it }

    val leaf = aged(base, look.droop, look.pressed)
    val leafLight = aged(base.lighter(0.18f), look.droop, look.pressed)
    val leafDark = aged(base.darker(0.22f), look.droop, look.pressed)
    val stem = aged(base.darker(0.12f), look.droop, look.pressed)
    val flower = bloomOf(look.kind).let { if (look.pressed) it.mix(Pressed, 0.25f) else it.mix(Dry, look.droop * 0.3f) }
    val pressed = look.pressed
    fun keep(c: Color) = if (pressed) c.mix(Pressed, 0.25f) else c
}

private fun Random.f(a: Float, b: Float) = a + nextFloat() * (b - a)

private fun quad(p0: Offset, c: Offset, p1: Offset, t: Float): Offset {
    val u = 1 - t
    return Offset(u * u * p0.x + 2 * u * t * c.x + t * t * p1.x, u * u * p0.y + 2 * u * t * c.y + t * t * p1.y)
}

/** A point in a leaf's own frame: [x] across, [y] along [angle] from [p]. */
private fun local(p: Offset, angle: Float, x: Float, y: Float) = Offset(p.x + cos(angle) * x + sin(angle) * y, p.y + sin(angle) * x - cos(angle) * y)

/** Pushes a leaf's angle down towards the soil with thirst. */
private fun hang(angle: Float, droop: Float) = angle + sign(angle).let { if (it == 0f) 1f else it } * droop * 0.9f

private fun DrawScope.leaf(p: Offset, angle: Float, len: Float, wid: Float, c: Color, rib: Color? = null) {
    val path = Path().apply {
        moveTo(p.x, p.y)
        local(p, angle, -wid, len * 0.45f).let { c1 -> local(p, angle, 0f, len).let { t -> quadraticTo(c1.x, c1.y, t.x, t.y) } }
        local(p, angle, wid, len * 0.45f).let { c2 -> quadraticTo(c2.x, c2.y, p.x, p.y) }
        close()
    }
    drawPath(path, c)
    if (rib != null) drawLine(rib, p, local(p, angle, 0f, len * 0.82f), strokeWidth = (len * 0.035f).coerceAtLeast(1f), cap = StrokeCap.Round)
}

private fun DrawScope.heart(p: Offset, angle: Float, len: Float, c: Color, rib: Color) {
    val path = Path().apply {
        moveTo(p.x, p.y)
        val a1 = local(p, angle, -0.6f * len, 0.12f * len)
        val a2 = local(p, angle, -0.5f * len, 0.78f * len)
        val t = local(p, angle, 0f, len)
        val b1 = local(p, angle, 0.5f * len, 0.78f * len)
        val b2 = local(p, angle, 0.6f * len, 0.12f * len)
        cubicTo(a1.x, a1.y, a2.x, a2.y, t.x, t.y)
        cubicTo(b1.x, b1.y, b2.x, b2.y, p.x, p.y)
        close()
    }
    drawPath(path, c)
    drawLine(rib, p, local(p, angle, 0f, len * 0.8f), strokeWidth = (len * 0.04f).coerceAtLeast(1f), cap = StrokeCap.Round)
}

private fun DrawScope.curve(p0: Offset, c: Offset, p1: Offset, color: Color, w: Float) {
    drawPath(Path().apply { moveTo(p0.x, p0.y); quadraticTo(c.x, c.y, p1.x, p1.y) }, color, style = Stroke(w, cap = StrokeCap.Round))
}

/** A flower head: [n] petals around a disc. */
private fun DrawScope.flower(at: Offset, r: Float, n: Int, petal: Color, disc: Color, turn: Float) {
    for (k in 0 until n) {
        val a = turn + k * Art.TAU / n
        val c = at.toward(a, r * 0.62f)
        rotate(Math.toDegrees(a.toDouble()).toFloat(), c) {
            drawOval(petal.darker(0.08f), Offset(c.x - r * 0.3f, c.y - r * 0.56f), Size(r * 0.6f, r * 1.12f))
            drawOval(petal, Offset(c.x - r * 0.24f, c.y - r * 0.5f), Size(r * 0.48f, r * 0.98f))
        }
    }
    drawCircle(disc, r * 0.36f, at)
    drawCircle(disc.darker(0.25f), r * 0.36f, at, style = Stroke(r * 0.06f))
}

/** Draws [look] growing up from [base] (the soil), at most [h] tall. [sway] is a small angle in radians. */
fun DrawScope.drawPlant(look: PlantLook, base: Offset, h: Float, sway: Float = 0f) {
    val ink = Ink(look)
    val r = Random(look.shape)
    when {
        look.stage == Stage.SEED -> marker(look, base, h, ink)
        look.stage == Stage.SPROUT && look.section != Section.CACTI && look.section != Section.MUSHROOMS -> sprout(look, base, h, sway, ink)
        else -> when (look.section) {
            Section.HERBS -> herbs(look, base, h, sway, ink, r)
            Section.FLOWERS -> flowers(look, base, h, sway, ink, r)
            Section.ROOTS -> roots(look, base, h, sway, ink, r)
            Section.GRAINS -> grains(look, base, h, sway, ink, r)
            Section.VINES -> vines(look, base, h, sway, ink, r)
            Section.CACTI -> cacti(look, base, h, sway, ink, r)
            Section.MUSHROOMS -> mushrooms(look, base, h, sway, ink, r)
            Section.TREES -> trees(look, base, h, sway, ink, r)
        }
    }
}

private fun size(look: PlantLook) = if (look.stage == Stage.BLOOM) 1f else 0.2f + 0.8f * look.growth

/** Sown, not yet watered: the empty packet on a stick, as gardeners mark a row. */
private fun DrawScope.marker(look: PlantLook, base: Offset, h: Float, ink: Ink) {
    drawOval(Color(0xFF3A281C), Offset(base.x - h * 0.07f, base.y - h * 0.025f), Size(h * 0.14f, h * 0.05f))
    val x = base.x + h * 0.05f
    drawLine(Color(0xFFB89466), Offset(x, base.y + h * 0.02f), Offset(x, base.y - h * 0.3f), strokeWidth = h * 0.016f, cap = StrokeCap.Round)
    val w = h * 0.17f
    val ph = h * 0.21f
    val top = base.y - h * 0.42f
    rotate(-6f, Offset(x, top + ph / 2)) {
        drawRoundRect(ink.keep(Color(0xFFF6EEDC)), Offset(x - w / 2, top), Size(w, ph), CornerRadius(h * 0.01f))
        drawRect(ink.keep(Color(look.section.ink)), Offset(x - w / 2, top), Size(w, ph * 0.28f))
        drawCircle(ink.keep(Color(look.section.ink)).copy(alpha = 0.75f), w * 0.22f, Offset(x, top + ph * 0.62f))
        drawRoundRect(Color(0x33000000), Offset(x - w / 2, top), Size(w, ph), CornerRadius(h * 0.01f), style = Stroke(h * 0.004f))
    }
}

/** The first two seed leaves, the same for almost everything. */
private fun DrawScope.sprout(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink) {
    val tip = base.toward(sway * 2f, h * 0.15f)
    curve(base, Offset(base.x - h * 0.03f, base.y - h * 0.07f), tip, ink.stem, h * 0.016f)
    val d = look.droop
    leaf(tip, hang(-1.15f, d), h * 0.1f, h * 0.04f, ink.leafLight, ink.leafDark)
    leaf(tip, hang(1.15f, d), h * 0.1f, h * 0.04f, ink.leafLight, ink.leafDark)
    if (look.growth > 0.15f) leaf(tip, sway, h * 0.07f, h * 0.025f, ink.leaf)
}

private fun DrawScope.herbs(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink, r: Random) {
    val s = size(look)
    val stems = 3 + r.nextInt(3)
    // Basil with flower spikes, needle-leaved rosemary with blue sprigs, round-leaved chives with pink balls.
    val variety = Math.floorMod(look.kind ushr 1, 3)
    val width = listOf(0.42f, 0.16f, 0.55f)[variety]
    val bloom = listOf(Color(0xFFF4EFE4), Color(0xFFB79AD6), Color(0xFFE7A6C4))[Math.floorMod(look.kind, 3)].let(ink::keep)
    for (i in 0 until stems) {
        val a = -0.6f + 1.2f * i / (stems - 1) + r.f(-0.12f, 0.12f) + sway
        val len = h * r.f(0.42f, 0.62f) * s
        val tip = base.toward(a + look.droop * 0.3f * sign(a), len * (1 - look.droop * 0.12f))
        val c = base.toward(a * 0.35f, len * 0.6f)
        curve(base, c, tip, ink.stem, h * 0.016f)
        val nodes = 2 + (s * 4).toInt()
        for (j in 1..nodes) {
            val t = j / (nodes + 0.4f)
            val p = quad(base, c, tip, t)
            val l = h * 0.11f * (1.1f - t * 0.5f) * (0.7f + 0.3f * s)
            val col = if (j % 2 == 0) ink.leaf else ink.leafLight
            leaf(p, hang(a - 0.95f, look.droop), l * (if (variety == 1) 0.85f else 1f), l * width, col, ink.leafDark)
            leaf(p, hang(a + 0.95f, look.droop), l * (if (variety == 1) 0.85f else 1f), l * width, col, ink.leafDark)
        }
        leaf(tip, a, h * 0.06f, h * 0.025f, ink.leafLight)
        when (look.stage) {
            Stage.BUD -> repeat(4) { k -> drawCircle(ink.leafLight.lighter(0.15f), h * 0.014f, tip.toward(a, h * 0.02f * (k + 1))) }
            Stage.BLOOM -> when (variety) {
                0 -> repeat(6) { k ->
                    val p = tip.toward(a, h * 0.022f * (k + 1))
                    drawCircle(bloom, h * 0.017f * (1.1f - k * 0.1f), Offset(p.x - h * 0.012f, p.y))
                    drawCircle(bloom, h * 0.017f * (1.1f - k * 0.1f), Offset(p.x + h * 0.012f, p.y))
                }
                1 -> repeat(4) { k -> drawCircle(ink.keep(Color(0xFF7FA6DA)), h * 0.014f, quad(base, c, tip, 0.55f + k * 0.13f).toward(a + if (k % 2 == 0) -0.9f else 0.9f, h * 0.03f)) }
                else -> {
                    val ball = tip.toward(a, h * 0.04f)
                    repeat(9) { k -> drawCircle(ink.keep(Color(0xFFD58AC0)), h * 0.014f, ball.toward(k * Art.TAU / 9, h * 0.02f)) }
                    drawCircle(ink.keep(Color(0xFFE7A6D6)), h * 0.018f, ball)
                }
            }
            else -> {}
        }
    }
}

private fun DrawScope.flowers(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink, r: Random) {
    val s = size(look)
    val stems = 1 + r.nextInt(3)
    val petals = 5 + Math.floorMod(look.kind, 4)
    for (i in 0 until stems) {
        val a = (i - (stems - 1) / 2f) * 0.3f + r.f(-0.08f, 0.08f) + sway * 1.3f
        val len = h * r.f(0.68f, 0.88f) * s * (if (i == stems / 2) 1f else 0.84f)
        val bend = look.droop * 0.7f * (if (a >= 0) 1f else -1f)
        val tip = base.toward(a + bend, len * (1 - look.droop * 0.18f))
        val c = base.toward(a * 0.3f, len * 0.55f)
        curve(base, c, tip, ink.stem, h * 0.017f)
        val leaves = 2 + r.nextInt(2)
        for (j in 0 until leaves) {
            val t = 0.15f + 0.45f * j / leaves
            val side = if ((j + i) % 2 == 0) -1f else 1f
            val l = h * 0.22f * (0.6f + 0.4f * s)
            leaf(quad(base, c, tip, t), hang(a + side * 0.75f, look.droop), l, l * 0.17f, if (j % 2 == 0) ink.leaf else ink.leafDark, ink.leafDark.darker(0.15f))
        }
        val turn = r.f(0f, 1f)
        when (look.stage) {
            Stage.BUD -> {
                val b = tip.toward(a + bend, h * 0.03f)
                drawOval(ink.leaf, Offset(b.x - h * 0.03f, b.y - h * 0.045f), Size(h * 0.06f, h * 0.09f))
                drawOval(ink.flower, Offset(b.x - h * 0.016f, b.y - h * 0.05f), Size(h * 0.032f, h * 0.06f))
            }
            Stage.BLOOM -> flower(tip, h * 0.11f * (if (i == stems / 2) 1f else 0.85f), petals, ink.flower, ink.keep(Color(0xFFE2A93B)), turn)
            else -> leaf(tip, a, h * 0.07f, h * 0.02f, ink.leafLight)
        }
    }
}

private fun DrawScope.roots(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink, r: Random) {
    val s = size(look)
    val root = listOf(Color(0xFFB3263E), Color(0xFFE07A2C), Color(0xFFF0E2E4), Color(0xFF7A3E8C))[Math.floorMod(look.kind, 4)].let(ink::keep)
    if (look.stage == Stage.BUD || look.stage == Stage.BLOOM) {
        val top = base.toward(sway, h * 0.84f)
        drawLine(ink.stem, base, top, strokeWidth = h * 0.016f, cap = StrokeCap.Round)
        val rays = 7
        for (k in 0 until rays) {
            val a = -0.9f + 1.8f * k / (rays - 1) + sway
            val e = top.toward(a, h * 0.1f)
            drawLine(ink.stem, top, e, strokeWidth = h * 0.007f)
            val col = if (look.stage == Stage.BLOOM) ink.keep(Color(0xFFF7F1E2)) else ink.leafLight
            repeat(3) { m -> drawCircle(col, h * 0.016f, e.toward(a + (m - 1) * 0.9f, h * 0.014f)) }
        }
    }
    val n = 5 + r.nextInt(4)
    for (i in 0 until n) {
        var a = -1.1f + 2.2f * i / (n - 1) + r.f(-0.1f, 0.1f) + sway * 0.6f
        a = hang(a, look.droop * 0.8f)
        val len = h * r.f(0.3f, 0.44f) * s
        val end = base.toward(a, len)
        curve(base, base.toward(a * 0.5f, len * 0.6f), end, root.mix(ink.stem, 0.45f), h * 0.014f)
        val l = h * 0.2f * (0.55f + 0.45f * s)
        leaf(end, a, l, l * 0.42f, if (i % 2 == 0) ink.leaf else ink.leafDark, root.mix(ink.leafDark, 0.5f))
    }
    val w = h * 0.17f * (0.45f + 0.55f * s)
    drawArc(root, 180f, 180f, true, Offset(base.x - w / 2, base.y - w * 0.4f), Size(w, w * 0.8f))
    drawArc(root.lighter(0.25f), 200f, 60f, false, Offset(base.x - w * 0.36f, base.y - w * 0.3f), Size(w * 0.72f, w * 0.6f), style = Stroke(w * 0.06f, cap = StrokeCap.Round))
}

private fun DrawScope.grains(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink, r: Random) {
    val s = size(look)
    val blades = 7 + r.nextInt(6)
    for (i in 0 until blades) {
        val a = r.f(-0.55f, 0.55f) + sway * 1.4f
        val len = h * r.f(0.42f, 0.78f) * s
        val curl = (if (a >= 0) 1f else -1f) * (0.25f + look.droop * 0.9f)
        val tip = base.toward(a + curl, len * (1 - look.droop * 0.25f))
        val c = base.toward(a, len * 0.7f)
        val w = h * 0.016f
        val path = Path().apply {
            moveTo(base.x - w, base.y)
            quadraticTo(c.x - w * 0.6f, c.y, tip.x, tip.y)
            quadraticTo(c.x + w * 0.6f, c.y, base.x + w, base.y)
            close()
        }
        drawPath(path, if (i % 3 == 0) ink.leafLight else if (i % 3 == 1) ink.leaf else ink.leafDark)
    }
    if (look.stage == Stage.BUD || look.stage == Stage.BLOOM) {
        val gold = if (look.stage == Stage.BLOOM) ink.keep(Color(0xFFD9AE48)) else ink.leafLight.lighter(0.1f)
        val ears = 3 + r.nextInt(3)
        for (i in 0 until ears) {
            val a = (i - (ears - 1) / 2f) * 0.16f + sway * 1.6f
            val len = h * r.f(0.78f, 0.95f)
            val top = base.toward(a, len)
            drawLine(ink.stem.mix(gold, 0.4f), base, top, strokeWidth = h * 0.009f)
            for (k in 0 until 8) {
                val p = base.toward(a, len - h * 0.022f * k)
                val side = if (k % 2 == 0) -1f else 1f
                val g = p.toward(a + side * 0.5f, h * 0.012f)
                rotate(Math.toDegrees((a + side * 0.4f).toDouble()).toFloat(), g) {
                    drawOval(gold, Offset(g.x - h * 0.011f, g.y - h * 0.02f), Size(h * 0.022f, h * 0.04f))
                }
                drawLine(gold.darker(0.1f), g, g.toward(a + side * 0.35f, h * 0.06f), strokeWidth = h * 0.003f)
            }
        }
    }
}

private fun DrawScope.vines(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink, r: Random) {
    val s = size(look)
    val sx = base.x + h * 0.05f
    drawLine(ink.keep(Bamboo), Offset(sx, base.y + h * 0.02f), Offset(sx, base.y - h * 0.95f), strokeWidth = h * 0.018f, cap = StrokeCap.Round)
    for (k in 1..5) drawLine(ink.keep(Bamboo.darker(0.25f)), Offset(sx - h * 0.01f, base.y - h * 0.18f * k), Offset(sx + h * 0.01f, base.y - h * 0.18f * k), strokeWidth = h * 0.006f)
    val vh = h * 0.92f * s
    val wave = h * 0.07f
    val turns = h * 0.11f
    val phase = r.f(0f, Art.TAU)
    fun at(y: Float): Offset {
        val hangs = (y / vh - 0.85f).coerceAtLeast(0f) * look.droop * 4f
        return Offset(sx + sin(y / turns + phase) * wave + hangs * h * 0.08f + sway * y * 0.6f, base.y - y + hangs * h * 0.1f)
    }
    val path = Path().apply {
        moveTo(base.x, base.y)
        var y = 0f
        while (y <= vh) {
            val p = at(y)
            lineTo(p.x, p.y)
            y += h * 0.01f
        }
    }
    drawPath(path, ink.stem, style = Stroke(h * 0.013f, cap = StrokeCap.Round))
    var y = h * 0.07f
    var i = 0
    while (y < vh) {
        val p = at(y)
        val side = if (i % 2 == 0) -1f else 1f
        val l = h * 0.085f * (0.7f + 0.3f * s)
        heart(p, hang(side * 1.05f + sway, look.droop), l, if (i % 2 == 0) ink.leaf else ink.leafLight, ink.leafDark)
        if (i % 3 == 2) {
            val t = p.toward(-side * 0.8f, h * 0.03f)
            drawArc(ink.stem, 0f, 270f, false, Offset(t.x - h * 0.015f, t.y - h * 0.015f), Size(h * 0.03f, h * 0.03f), style = Stroke(h * 0.004f))
        }
        if (i % 3 == 1 && (look.stage == Stage.BLOOM || look.stage == Stage.BUD)) {
            val a = -side * 0.9f
            if (look.stage == Stage.BLOOM) {
                val mouth = p.toward(a, h * 0.075f)
                val tube = Path().apply {
                    moveTo(p.x, p.y)
                    val l1 = local(p, a, -h * 0.035f, h * 0.075f)
                    val l2 = local(p, a, h * 0.035f, h * 0.075f)
                    lineTo(l1.x, l1.y); lineTo(l2.x, l2.y); close()
                }
                drawPath(tube, ink.flower.darker(0.12f))
                drawCircle(ink.flower, h * 0.05f, mouth)
                drawCircle(ink.keep(Color(0xFFF7F1E2)), h * 0.012f, mouth)
            } else {
                leaf(p, a, h * 0.05f, h * 0.012f, ink.flower.mix(ink.leaf, 0.4f))
            }
        }
        y += h * 0.085f
        i++
    }
}

private fun DrawScope.cacti(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink, r: Random) {
    val s = if (look.stage == Stage.SPROUT) 0.12f else size(look)
    val body = ink.leaf
    val lean = Math.toDegrees((look.droop * 0.12f + sway * 0.25f).toDouble()).toFloat()
    val spine = ink.keep(Cream)
    val barrel = Math.floorMod(look.kind, 2) == 1
    rotate(lean, base) {
        if (barrel) {
            val w = h * (0.22f + 0.24f * s)
            val bh = w * 0.95f
            val top = Offset(base.x, base.y - bh)
            drawOval(body, Offset(base.x - w / 2, base.y - bh), Size(w, bh * 1.15f))
            for (k in 1..4) {
                val x = w * (k / 5f - 0.5f)
                drawArc(ink.leafDark, -90f - 50f, 100f, false, Offset(base.x + x - w * 0.1f, base.y - bh), Size(w * 0.2f, bh * 1.15f), style = Stroke(h * 0.005f))
            }
            repeat(18) {
                val p = Offset(base.x + r.f(-0.4f, 0.4f) * w, base.y - r.f(0.15f, 0.9f) * bh)
                drawCircle(spine, h * 0.006f, p)
            }
            if (look.droop > 0.3f) for (k in 1..3) drawLine(Dry.copy(alpha = 0.6f), Offset(base.x - w * 0.3f, base.y - bh * k / 4f), Offset(base.x + w * 0.3f, base.y - bh * k / 4f), strokeWidth = h * 0.004f)
            when (look.stage) {
                Stage.BUD -> repeat(3) { k -> drawCircle(ink.flower.mix(body, 0.3f), h * 0.018f, top.toward((k - 1) * 0.7f, w * 0.12f)) }
                Stage.BLOOM -> repeat(4) { k -> flower(top.toward((k - 1.5f) * 0.55f, w * 0.14f), h * 0.04f, 9, ink.flower, ink.keep(Color(0xFFF2D46A)), k * 0.4f) }
                else -> {}
            }
        } else {
            val cw = h * (0.16f + Math.floorMod(look.kind ushr 2, 3) * 0.025f) * (0.6f + 0.4f * s)
            val ch = h * (0.2f + 0.58f * s)
            fun column(x: Float, bottom: Float, height: Float, width: Float) {
                drawRoundRect(body, Offset(x - width / 2, bottom - height), Size(width, height + width * 0.2f), CornerRadius(width / 2))
                for (k in listOf(-0.22f, 0f, 0.22f)) drawLine(if (k == 0f) ink.leafLight else ink.leafDark, Offset(x + k * width, bottom - height + width * 0.3f), Offset(x + k * width, bottom), strokeWidth = h * 0.005f)
                var yy = bottom - height + width * 0.35f
                while (yy < bottom - width * 0.1f) {
                    for (k in listOf(-0.36f, 0.11f, -0.11f, 0.36f)) {
                        val p = Offset(x + k * width, yy)
                        drawLine(spine, p, Offset(p.x + (if (k < 0) -1 else 1) * h * 0.012f, p.y - h * 0.008f), strokeWidth = h * 0.003f)
                    }
                    yy += h * 0.045f
                }
            }
            if (s > 0.55f) {
                val y = base.y - ch * 0.42f
                val ax = base.x - cw * 0.95f
                drawRoundRect(body, Offset(ax, y - cw * 0.22f), Size(cw * 0.95f, cw * 0.44f), CornerRadius(cw * 0.22f))
                column(ax + cw * 0.16f, y + cw * 0.2f, ch * 0.32f, cw * 0.55f)
            }
            if (s > 0.82f) {
                val y = base.y - ch * 0.6f
                drawRoundRect(body, Offset(base.x, y - cw * 0.2f), Size(cw * 0.9f, cw * 0.4f), CornerRadius(cw * 0.2f))
                column(base.x + cw * 0.78f, y + cw * 0.18f, ch * 0.25f, cw * 0.5f)
            }
            column(base.x, base.y, ch, cw)
            if (look.droop > 0.3f) for (k in 1..4) drawLine(Dry.copy(alpha = 0.55f), Offset(base.x - cw * 0.35f, base.y - ch * k / 5f), Offset(base.x + cw * 0.35f, base.y - ch * k / 5f), strokeWidth = h * 0.004f)
            val top = Offset(base.x, base.y - ch)
            when (look.stage) {
                Stage.BUD -> drawOval(ink.flower.mix(body, 0.25f), Offset(top.x - h * 0.025f, top.y - h * 0.04f), Size(h * 0.05f, h * 0.06f))
                Stage.BLOOM -> {
                    flower(Offset(top.x, top.y - h * 0.02f), h * 0.075f, 12, ink.flower, ink.keep(Color(0xFFF2D46A)), 0.2f)
                    flower(Offset(top.x, top.y - h * 0.02f), h * 0.045f, 8, ink.flower.lighter(0.25f), ink.keep(Color(0xFFF2D46A)), 0.6f)
                }
                else -> {}
            }
        }
    }
}

private fun DrawScope.mushrooms(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink, r: Random) {
    val s = if (look.stage == Stage.SPROUT) 0.1f else size(look)
    val moss = ink.keep(Color(0xFF6E8A3C)).mix(Dry, look.droop * 0.4f)
    val style = Math.floorMod(look.kind, 3)
    val count = 1 + (s * 4).toInt() + if (look.stage == Stage.BLOOM) 1 else 0
    data class Shroom(val x: Float, val hh: Float, val cap: Float, val tilt: Float)
    val list = (0 until count).map {
        Shroom(r.f(-0.2f, 0.2f) * h, h * r.f(0.16f, 0.4f) * (0.5f + 0.5f * s), h * r.f(0.06f, 0.11f) * (0.6f + 0.4f * s), r.f(-0.25f, 0.25f) + sway * 1.5f + look.droop * 0.3f)
    }.sortedByDescending { it.hh }
    if (look.stage == Stage.BLOOM) for (m in list) {
        val c = Offset(base.x + m.x, base.y - m.hh)
        drawCircle(Brush.radialGradient(listOf(Color(0x5596F0C8), Color(0x0096F0C8)), c, m.cap * 2.6f), m.cap * 2.6f, c)
    }
    for (m in list) {
        val foot = Offset(base.x + m.x, base.y)
        val top = foot.toward(m.tilt * 0.4f, m.hh)
        curve(foot, foot.toward(0f, m.hh * 0.5f), top, ink.keep(Cream).darker(0.05f), m.cap * 0.32f)
        val capC = ink.keep(listOf(Color(0xFFC8322B), Color(0xFF8A5A33), Color(0xFFE8DCCB))[style]).mix(Dry, look.droop * 0.35f)
        val closed = look.stage == Stage.BUD || look.stage == Stage.SPROUT
        rotate(Math.toDegrees(m.tilt.toDouble()).toFloat(), top) {
            if (style == 2) {
                val path = Path().apply {
                    moveTo(top.x - m.cap * 0.7f, top.y + m.cap * 0.5f)
                    quadraticTo(top.x - m.cap * 0.75f, top.y - m.cap * 1.5f, top.x, top.y - m.cap * 1.6f)
                    quadraticTo(top.x + m.cap * 0.75f, top.y - m.cap * 1.5f, top.x + m.cap * 0.7f, top.y + m.cap * 0.5f)
                    close()
                }
                drawPath(path, capC)
                drawLine(capC.darker(0.3f), Offset(top.x - m.cap * 0.65f, top.y + m.cap * 0.5f), Offset(top.x + m.cap * 0.65f, top.y + m.cap * 0.5f), strokeWidth = m.cap * 0.12f, cap = StrokeCap.Round)
            } else {
                val w = if (closed) m.cap * 1.3f else m.cap * 2f
                val ch = if (closed) m.cap * 1.2f else m.cap * 1.05f
                drawArc(capC, 180f, 180f, true, Offset(top.x - w / 2, top.y - ch), Size(w, ch * 2))
                drawRect(capC.darker(0.25f), Offset(top.x - w / 2, top.y - m.cap * 0.04f), Size(w, m.cap * 0.12f))
                if (style == 0) repeat(5) { k -> drawCircle(ink.keep(Cream), m.cap * 0.11f, Offset(top.x + (k - 2) * w * 0.17f, top.y - ch * (0.35f + 0.3f * ((k + 1) % 2)))) }
                else drawArc(capC.lighter(0.2f), 200f, 60f, false, Offset(top.x - w * 0.35f, top.y - ch * 0.8f), Size(w * 0.7f, ch * 1.4f), style = Stroke(m.cap * 0.1f, cap = StrokeCap.Round))
            }
        }
    }
    val mw = h * 0.56f
    drawOval(moss, Offset(base.x - mw / 2, base.y - h * 0.05f), Size(mw, h * 0.1f))
    repeat(7) { k -> drawCircle(if (k % 2 == 0) moss.lighter(0.12f) else moss.darker(0.1f), h * 0.03f, Offset(base.x - mw * 0.4f + k * mw * 0.13f, base.y - h * 0.035f)) }
    if (look.stage == Stage.BLOOM) repeat(9) {
        drawCircle(Color(0xCCDDFBEA), h * r.f(0.004f, 0.009f), Offset(base.x + r.f(-0.3f, 0.3f) * h, base.y - h * r.f(0.35f, 0.75f)))
    }
}

private fun DrawScope.trees(look: PlantLook, base: Offset, h: Float, sway: Float, ink: Ink, r: Random) {
    val s = size(look)
    val depth = (1 + (s * 4).roundToInt()).coerceIn(1, 5)
    val tips = mutableListOf<Offset>()
    val bark = ink.keep(Trunk)
    fun branch(p: Offset, a: Float, len: Float, w: Float, level: Int) {
        val e = p.toward(a, len)
        drawLine(bark, p, e, strokeWidth = w, cap = StrokeCap.Round)
        if (level >= depth) {
            tips += e
            return
        }
        val kids = if (r.nextFloat() < 0.3f) 3 else 2
        for (k in 0 until kids) {
            val spread = r.f(0.32f, 0.58f)
            val na = a + (k - (kids - 1) / 2f) * spread * 1.4f + sway * level * 0.4f
            branch(e, na, len * r.f(0.66f, 0.78f), w * 0.66f, level + 1)
        }
    }
    branch(base, r.f(-0.1f, 0.1f) + sway * 0.5f, h * 0.26f * (0.5f + 0.5f * s), h * 0.052f * (0.5f + 0.5f * s), 1)
    val clump = h * 0.075f * (0.6f + 0.4f * s)
    for ((i, t) in tips.withIndex()) {
        val off = clump * 0.5f
        drawCircle(ink.leafDark, clump, Offset(t.x, t.y + off * 0.3f))
        drawCircle(ink.leaf, clump * 0.85f, Offset(t.x - off * 0.6f, t.y - off * 0.2f))
        drawCircle(ink.leafLight, clump * 0.6f, Offset(t.x + off * 0.4f, t.y - off * 0.6f))
        if (look.droop > 0.5f && i % 3 == 0) drawCircle(Dry, clump * 0.2f, Offset(t.x + clump * 0.3f, t.y + clump * 0.9f))
    }
    val fruit = Math.floorMod(look.kind, 2) == 1
    for (t in tips) {
        when (look.stage) {
            Stage.BUD -> repeat(2) { drawCircle(ink.keep(Color(0xFFE9A3B5)), h * 0.009f, Offset(t.x + r.f(-1f, 1f) * clump, t.y + r.f(-1f, 0.5f) * clump)) }
            Stage.BLOOM -> repeat(3) {
                val p = Offset(t.x + r.f(-1f, 1f) * clump * 0.9f, t.y + r.f(-1f, 0.6f) * clump * 0.9f)
                if (fruit) {
                    drawCircle(ink.flower.let { c -> if (c.red > 0.85f && c.green > 0.85f) Color(0xFFD9475B) else c }, h * 0.018f, p)
                    drawCircle(Color(0x66FFFFFF), h * 0.006f, Offset(p.x - h * 0.006f, p.y - h * 0.006f))
                } else {
                    repeat(5) { k -> drawCircle(ink.keep(Color(0xFFF6D3DC)), h * 0.008f, p.toward(k * Art.TAU / 5, h * 0.009f)) }
                    drawCircle(ink.keep(Color(0xFFD9657F)), h * 0.005f, p)
                }
            }
            else -> {}
        }
    }
}
