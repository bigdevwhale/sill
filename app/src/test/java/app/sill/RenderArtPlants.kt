package app.sill

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.sill.data.Section
import app.sill.domain.Stage
import app.sill.ui.art.Art
import app.sill.ui.art.PlantLook
import app.sill.ui.art.drawPot
import app.sill.ui.art.drawPlant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every section at every stage, plus a thirsty one, on one sheet (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w600dp-h900dp-xxhdpi")
class RenderArtPlants {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun sheet() {
        Art.still = true
        val stages = listOf(Stage.SEED to 0f, Stage.SPROUT to 0.15f, Stage.LEAVES to 0.5f, Stage.BUD to 0.8f, Stage.BLOOM to 1f, Stage.LEAVES to 0.55f)
        rule.setContent {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFFF1E9DA))
                val cw = size.width / stages.size
                val rh = size.height / Section.entries.size
                Section.entries.forEachIndexed { r, s ->
                    stages.forEachIndexed { c, (st, g) ->
                        val x = cw * (c + 0.5f)
                        val bottom = rh * (r + 1) - rh * 0.05f
                        val soil = drawPot(x, bottom, cw * 0.5f, rh * 0.28f, c, false)
                        drawPlant(PlantLook(s, s.name.hashCode() + c, (r * 10 + c).toLong(), g, st, droop = if (c == 5) 1f else 0f), soil, rh * 0.68f)
                        if (c == 0) drawCircle(Color(s.ink), 6f, Offset(8f, rh * r + 10f))
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }
            File(out, "plants.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
