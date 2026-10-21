package app.sill.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sill.data.Seed
import app.sill.domain.Stage
import app.sill.ui.theme.Print
import app.sill.ui.theme.Serif

private val PacketPaper = Color(0xFFF6EEDC)
private val PrintInk = Color(0xFF2C2620)

/**
 * The front of a seed packet, printed like an old lithograph: a coloured band with the word
 * SEEDS, the plant in full bloom in an arched panel of halftone dots, the name underneath.
 * [title] is the band's word, [section] the line under it, [foot] the small print at the bottom.
 */
@Composable
fun SeedPacket(seed: Seed, name: String, title: String, section: String, foot: String, modifier: Modifier = Modifier, compact: Boolean = false, ghost: Boolean = false) {
    val ink = Color(seed.section.ink)
    Box(modifier.aspectRatio(0.72f)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = CornerRadius(w * 0.03f)
            drawRoundRect(Color(0x33000000), Offset(w * 0.015f, h * 0.012f), size, r)
            drawRoundRect(PacketPaper, Offset.Zero, size, r)
            // The folded top flap.
            drawRect(PacketPaper.darker(0.06f), Offset(0f, 0f), Size(w, h * 0.035f))
            drawLine(PacketPaper.darker(0.15f), Offset(0f, h * 0.035f), Offset(w, h * 0.035f), strokeWidth = 1.5f)
            val band = h * (if (compact) 0.16f else 0.17f)
            drawRect(ink, Offset(0f, h * 0.035f), Size(w, band))
            val m = w * 0.06f
            drawRect(PrintInk.copy(alpha = 0.85f), Offset(m * 0.5f, m * 0.5f + h * 0.035f + band), Size(w - m, h - band - h * 0.035f - m), style = Stroke(1.6f))
            // The arched panel with the plant.
            val top = h * 0.035f + band + m * 1.3f
            val bottom = h * (if (compact) 0.73f else 0.7f)
            val left = m * 1.4f
            val right = w - m * 1.4f
            val arch = Path().apply {
                moveTo(left, bottom)
                lineTo(left, top + (right - left) * 0.35f)
                quadraticTo(left, top, (left + right) / 2, top)
                quadraticTo(right, top, right, top + (right - left) * 0.35f)
                lineTo(right, bottom)
                close()
            }
            drawPath(arch, ink.lighter(0.72f))
            clipPath(arch) {
                val step = w * 0.035f
                var y = top
                var row = 0
                while (y < bottom) {
                    var x = left + if (row % 2 == 0) 0f else step / 2
                    while (x < right) {
                        drawCircle(ink.copy(alpha = 0.18f), step * 0.18f, Offset(x, y))
                        x += step
                    }
                    y += step * 0.87f
                    row++
                }
                drawRect(ink.lighter(0.45f), Offset(left, bottom - (bottom - top) * 0.12f), Size(right - left, (bottom - top) * 0.12f))
                drawPlant(PlantLook(seed.section, seed.hash, seed.hash.toLong(), 1f, Stage.BLOOM), Offset((left + right) / 2, bottom - (bottom - top) * 0.06f), (bottom - top) * 0.92f)
            }
            drawPath(arch, PrintInk.copy(alpha = 0.8f), style = Stroke(1.8f))
            if (ghost) drawRoundRect(Color(0x88F1E9DA), Offset.Zero, size, r)
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.fillMaxWidth().weight(0.035f))
            Column(Modifier.fillMaxWidth().weight(if (compact) 0.16f else 0.17f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Text(title.uppercase(), style = TextStyle(fontFamily = Print, fontSize = if (compact) 15.sp else 24.sp, letterSpacing = 0.25.em, color = PacketPaper), maxLines = 1)
                if (!compact) Text(section.uppercase(), style = TextStyle(fontFamily = Print, fontSize = 11.sp, letterSpacing = 0.2.em, color = PacketPaper.copy(alpha = 0.85f)), maxLines = 1)
            }
            Spacer(Modifier.fillMaxWidth().weight(if (compact) 0.57f else 0.53f))
            Column(Modifier.fillMaxWidth().weight(0.24f).padding(horizontal = if (compact) 10.dp else 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Text(
                    name, style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = if (compact) 13.5.sp else 22.sp, lineHeight = if (compact) 16.sp else 26.sp, color = PrintInk, textAlign = TextAlign.Center),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (foot.isNotBlank()) Text(foot, style = TextStyle(fontFamily = Print, fontSize = if (compact) 10.sp else 12.sp, letterSpacing = 0.12.em, color = ink.darker(0.1f)), maxLines = 1, modifier = Modifier.padding(top = 3.dp))
            }
            Spacer(Modifier.fillMaxWidth().weight(0.04f))
        }
    }
}
