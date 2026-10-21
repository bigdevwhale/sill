package app.sill.ui.sill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sill.R
import app.sill.domain.Rules
import app.sill.domain.Stage
import app.sill.domain.Thirst
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.T
import app.sill.ui.art.SillScene
import app.sill.ui.components.PrimaryButton
import app.sill.ui.components.SectionTitle
import app.sill.ui.components.SoftButton
import app.sill.ui.ctx
import app.sill.ui.theme.S
import app.sill.ui.theme.SType

@Composable
fun SillScreen(home: Home, go: Go) {
    val c = ctx()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = SType.Hero.copy(fontSize = 30.sp), modifier = Modifier.weight(1f))
            Badged(home.packets.size) { IconButton(onClick = go::tin) { Icon(Icons.Rounded.Inventory2, stringResource(R.string.tin), tint = S.c.ink) } }
            IconButton(onClick = go::year) { Icon(Icons.Rounded.AutoStories, stringResource(R.string.herbarium), tint = S.c.ink) }
            IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = S.c.ink) }
        }
        SillScene(
            pots = home.pots.map { p -> p?.let(home::view) },
            garden = home.garden.map { home.look(it) },
            dried = home.dried.map { home.look(it) },
            windPacket = home.windPacket?.let { home.seed(it.seed)?.section },
            modifier = Modifier.fillMaxWidth().aspectRatio(1.02f),
            onPot = { i -> home.pots[i]?.let { go.plant(it.id) } ?: go.catalogue() },
            onVase = go::garden,
            onWind = { home.windPacket?.let { go.packet(it.seed, it.id) } },
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            Plaque(
                home.lastSown?.let { c.getString(R.string.last_sown, T.ago(c, it).lowercase()) } ?: stringResource(R.string.nothing_sown),
            )
            home.windPacket?.let { w ->
                home.seed(w.seed)?.let { s -> Hint(Icons.Rounded.Air, stringResource(R.string.wind_brought, s.name(home.ru)), S.c.water) { go.packet(w.seed, w.id) } }
            }
            home.blooming.forEach { p ->
                home.seed(p.seed)?.let { s -> Hint(Icons.Rounded.LocalFlorist, stringResource(R.string.bloomed_hint, s.name(home.ru)), S.c.clay) { go.plant(p.id) } }
            }
            if (home.sown.isNotEmpty()) {
                SectionTitle(stringResource(R.string.on_the_sill))
                home.pots.forEachIndexed { i, p ->
                    if (p == null) return@forEachIndexed
                    val s = home.seed(p.seed) ?: return@forEachIndexed
                    val stage = home.stage(p)
                    val thirst = home.thirst(p)
                    Row(
                        Modifier.fillMaxWidth().clip(S.RMd).clickable { go.plant(p.id) }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(s.section.ink)))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name(home.ru), style = SType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val line = if (stage == Stage.SEED) T.stage(c, stage) else
                                "${T.stage(c, stage)} · ${c.getString(R.string.watered_of, home.count(p.id), home.target(p))}"
                            Text(line, style = SType.Muted)
                        }
                        if (thirst != Thirst.FRESH) {
                            Row(Modifier.clip(S.RSm).background(S.c.waterSoft).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WaterDrop, null, Modifier.size(14.dp), tint = S.c.water)
                                Spacer(Modifier.width(4.dp))
                                Text(T.thirst(c, thirst), style = SType.Small.copy(color = S.c.water))
                            }
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            val free = Rules.POTS - home.sown.size
            PrimaryButton(
                if (free > 0) c.resources.getQuantityString(R.plurals.free_pots, free, free) else stringResource(R.string.catalogue),
                go::catalogue, Modifier.fillMaxWidth(), icon = Icons.Rounded.Spa,
            )
            Spacer(Modifier.height(10.dp))
            SoftButton(
                if (home.garden.isEmpty()) stringResource(R.string.garden) else stringResource(R.string.garden_n, home.garden.size),
                go::garden, Modifier.fillMaxWidth(), icon = Icons.Rounded.Yard,
            )
            if (home.compostLeft > 0) {
                Text(
                    c.resources.getQuantityString(R.plurals.compost_ready, home.compostLeft, home.compostLeft), style = SType.Muted,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** The small brass plate by the window. */
@Composable
private fun Plaque(text: String) {
    Box(
        Modifier.fillMaxWidth().padding(top = 14.dp).clip(S.RSm).background(Color(0xFFE6D3A8)).border(1.dp, Color(0xFFC4A86E), S.RSm).padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = SType.Body.copy(fontFamily = app.sill.ui.theme.Serif, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color(0xFF5E4A26), fontSize = 15.sp)) }
}

@Composable
private fun Hint(icon: ImageVector, text: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(S.RMd).background(S.c.surface).border(1.dp, S.c.line, S.RMd).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
        Text(text, style = SType.Body, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
    }
}

@Composable
private fun Badged(count: Int, content: @Composable () -> Unit) {
    Box {
        content()
        if (count > 0) Box(
            Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(17.dp).clip(CircleShape).background(S.c.clay),
            contentAlignment = Alignment.Center,
        ) { Text("$count", style = SType.Small.copy(color = S.c.onClay, fontSize = 10.sp)) }
    }
}
