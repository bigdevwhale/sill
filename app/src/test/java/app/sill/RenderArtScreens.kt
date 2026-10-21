package app.sill

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.sill.data.DemoSeed
import app.sill.data.Place
import app.sill.domain.Stage
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.art.Art
import app.sill.ui.catalogue.CatalogueScreen
import app.sill.ui.garden.GardenScreen
import app.sill.ui.onboarding.GuideScreen
import app.sill.ui.onboarding.WelcomeScreen
import app.sill.ui.packet.PacketScreen
import app.sill.ui.plant.PlantScreen
import app.sill.ui.sill.SillScreen
import app.sill.ui.theme.S
import app.sill.ui.theme.SillTheme
import app.sill.ui.tin.TinScreen
import app.sill.ui.year.YearScreen
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo sill, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() {
        Art.still = true
    }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<SillApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        val dao = c.db.dao()
        Home(dao.plantings(), dao.waterings(), dao.own(), dao.packets(), c.settings.now(), c.catalogue, System.currentTimeMillis(), app.ru)
    }

    private fun shot(name: String, h: Home, content: @Composable (Home, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            SillTheme { Box(Modifier.fillMaxSize().background(S.c.bg)) { content(h, Go(nav)) } }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun potWith(h: Home, stage: Stage) = h.sown.first { h.stage(it) == stage }

    @Test fun welcome() = home().let { shot("0-welcome", it) { _, _ -> WelcomeScreen {} } }
    @Test fun guideSections() = home().let { shot("0-guide-2", it) { _, _ -> GuideScreen(first = true, startPage = 1) {} } }
    @Test fun guideSow() = home().let { shot("0-guide-3", it) { _, _ -> GuideScreen(first = true, startPage = 2) {} } }
    @Test fun guideWater() = home().let { shot("0-guide-4", it) { _, _ -> GuideScreen(first = true, startPage = 3) {} } }
    @Test fun guideBloom() = home().let { shot("0-guide-5", it) { _, _ -> GuideScreen(first = true, startPage = 4) {} } }
    @Test fun sill() = home().let { shot("1-sill", it) { h, go -> SillScreen(h, go) } }
    @Test fun plant() = home().let { h -> shot("2-plant", h) { hh, go -> PlantScreen(potWith(hh, Stage.BUD).id, hh, go) } }
    @Test fun catalogue() = home().let { shot("3-catalogue", it) { h, go -> CatalogueScreen(h, go) } }
    @Test fun packet() = home().let { h -> shot("4-packet", h) { hh, go -> PacketScreen("c:theremin", hh.packetOf("c:theremin")?.id ?: 0, hh, go) } }
    @Test fun garden() = home().let { shot("5-garden", it) { h, go -> GardenScreen(h, go) } }
    @Test fun year() = home().let { shot("6-herbarium", it) { h, go -> YearScreen(h, go) } }
    @Test fun tin() = home().let { shot("7-tin", it) { h, go -> TinScreen(h, go) } }
    @Test fun bloom() = home().let { h -> shot("8-bloom", h) { hh, go -> PlantScreen(potWith(hh, Stage.BLOOM).id, hh, go) } }
    @Test fun gift() = home().let { h -> shot("9-gift", h) { hh, go -> PlantScreen(hh.sown.first { it.from.isNotBlank() }.id, hh, go) } }
    @Test fun inGarden() = home().let { h -> shot("10-garden-plant", h) { hh, go -> PlantScreen(hh.plantings.first { it.placeOf == Place.GARDEN && it.seed == "c:guitar" }.id, hh, go) } }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
