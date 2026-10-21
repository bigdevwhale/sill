package app.sill.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sill.R
import app.sill.container
import app.sill.data.Section
import app.sill.domain.Stage
import app.sill.ui.T
import app.sill.ui.art.MiniPot
import app.sill.ui.art.PlantLook
import app.sill.ui.art.PotView
import app.sill.ui.art.SillScene
import app.sill.ui.components.Field
import app.sill.ui.components.PrimaryButton
import app.sill.ui.components.TextLink
import app.sill.ui.ctx
import app.sill.ui.theme.S
import app.sill.ui.theme.SType
import kotlinx.coroutines.launch

private const val PAGES = 5

/** A plant of [section] at [stage], the same one every time it's drawn in the guide. */
private fun look(section: Section, stage: Stage, growth: Float) = PlantLook(section, section.name.hashCode(), section.ordinal * 7L + 3, growth, stage)

/**
 * How the sill works, in five pages with pictures: why it exists, what the catalogue's sections
 * are, sowing is trying, watering is going back, bloom or compost. On the first run it ends by
 * asking for a name and opening the catalogue; later it can be opened again from the sill.
 */
@Composable
fun GuideScreen(first: Boolean, startPage: Int = 0, onDone: (toCatalogue: Boolean) -> Unit) {
    val c = ctx()
    val pager = rememberPagerState(startPage) { PAGES }
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    fun finish(catalogue: Boolean) {
        if (first) c.container.write { settings.update { it.copy(onboarded = true, name = name.trim()) } }
        onDone(catalogue)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = SType.Title.copy(fontSize = 18.sp), modifier = Modifier.weight(1f).padding(start = 12.dp))
            if (pager.currentPage < PAGES - 1) TextLink(stringResource(if (first) R.string.guide_skip else R.string.guide_close), { if (first) scope.launch { pager.animateScrollToPage(PAGES - 1) } else onDone(false) })
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                when (page) {
                    0 -> Why()
                    1 -> Sections()
                    2 -> Sow()
                    3 -> Water()
                    else -> Bloom(first, name) { name = it.take(40) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
            repeat(PAGES) { i ->
                Box(Modifier.padding(horizontal = 4.dp).size(if (i == pager.currentPage) 10.dp else 7.dp).clip(CircleShape).background(if (i == pager.currentPage) S.c.clay else S.c.line))
            }
        }
        Box(Modifier.padding(horizontal = 24.dp).padding(top = 6.dp, bottom = 16.dp)) {
            if (pager.currentPage < PAGES - 1) {
                PrimaryButton(stringResource(R.string.guide_next), { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
            } else {
                PrimaryButton(stringResource(R.string.guide_first_seed), { finish(true) }, Modifier.fillMaxWidth())
            }
        }
        if (pager.currentPage == PAGES - 1) TextLink(stringResource(R.string.guide_look_around), { finish(false) }, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
    }
}

@Composable
private fun Head(title: Int, body: Int) {
    Text(stringResource(title), style = SType.Display)
    Spacer(Modifier.height(10.dp))
    Text(stringResource(body), style = SType.Sub)
}

@Composable
private fun Why() {
    SillScene(
        pots = listOf(
            null, PotView(look(Section.FLOWERS, Stage.LEAVES, 0.5f), false), PotView(look(Section.HERBS, Stage.SPROUT, 0.15f), false), null, null,
        ),
        garden = listOf(look(Section.TREES, Stage.BLOOM, 1f), look(Section.GRAINS, Stage.BLOOM, 1f)), dried = emptyList(), windPacket = null,
        modifier = Modifier.fillMaxWidth().aspectRatio(1.15f).clip(S.RLg),
    )
    Spacer(Modifier.height(20.dp))
    Head(R.string.guide_1_t, R.string.guide_1_b)
}

/** The eight sections, each with its plant and what it holds. */
@Composable
private fun Sections() {
    val c = ctx()
    Head(R.string.guide_2_t, R.string.guide_2_b)
    Spacer(Modifier.height(12.dp))
    Section.entries.forEachIndexed { i, s ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniPot(look(s, Stage.BLOOM, 1f), Modifier.size(width = 46.dp, height = 56.dp), style = i)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(T.section(c, s), style = SType.Strong.copy(color = Color(s.ink)))
                Text(T.holds(c, s), style = SType.Muted)
            }
        }
    }
}

@Composable
private fun Stages(vararg stages: Pair<Stage, Float>, section: Section, drops: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        stages.forEachIndexed { i, (st, g) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (drops) Row { repeat(i + 1) { Icon(Icons.Rounded.WaterDrop, null, Modifier.size(14.dp), tint = S.c.water) } }
                MiniPot(look(section, st, g), Modifier.size(width = 104.dp, height = 176.dp), style = i + 1)
                Text(T.stage(ctx(), st).substringBefore(','), style = SType.Small, textAlign = TextAlign.Center, modifier = Modifier.width(104.dp).padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Sow() {
    Stages(Stage.SEED to 0f, Stage.SPROUT to 0.15f, section = Section.FLOWERS, drops = false)
    Spacer(Modifier.height(16.dp))
    Head(R.string.guide_3_t, R.string.guide_3_b)
    Spacer(Modifier.height(12.dp))
    Note(R.string.guide_3_n)
}

@Composable
private fun Water() {
    Stages(Stage.SPROUT to 0.15f, Stage.LEAVES to 0.5f, Stage.BUD to 0.8f, section = Section.FLOWERS, drops = true)
    Spacer(Modifier.height(16.dp))
    Head(R.string.guide_4_t, R.string.guide_4_b)
    Spacer(Modifier.height(12.dp))
    Note(R.string.guide_4_n)
}

@Composable
private fun Bloom(first: Boolean, name: String, onName: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiniPot(look(Section.FLOWERS, Stage.BLOOM, 1f), Modifier.size(width = 130.dp, height = 200.dp), style = 4)
            Text(stringResource(R.string.guide_to_garden), style = SType.Small.copy(color = S.c.leaf), modifier = Modifier.padding(top = 4.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiniPot(look(Section.CACTI, Stage.LEAVES, 0.4f).copy(droop = 1f), Modifier.size(width = 120.dp, height = 170.dp), style = 0)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Rounded.Eco, null, Modifier.size(14.dp), tint = S.c.muted)
                Text(stringResource(R.string.guide_to_compost), style = SType.Small)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Head(R.string.guide_5_t, R.string.guide_5_b)
    if (first) {
        Spacer(Modifier.height(18.dp))
        Field(name, onName, stringResource(R.string.your_name_hint))
        Text(stringResource(R.string.your_name_sub), style = SType.Muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
    }
}

/** A tip in the margin, on a sticky note. */
@Composable
private fun Note(text: Int) {
    Text(
        stringResource(text), style = SType.Hand.copy(fontSize = 21.sp, lineHeight = 24.sp),
        modifier = Modifier.fillMaxWidth().clip(S.RSm).background(S.c.sticky).padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** The first run: the guide, then the sill (and the catalogue, if asked). */
@Composable
fun WelcomeScreen(onDone: (toCatalogue: Boolean) -> Unit) {
    GuideScreen(first = true, onDone = onDone)
}
