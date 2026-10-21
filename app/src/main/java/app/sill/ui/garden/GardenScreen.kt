package app.sill.ui.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sill.R
import app.sill.data.Planting
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.T
import app.sill.ui.art.GardenView
import app.sill.ui.components.SectionTitle
import app.sill.ui.components.TopBar
import app.sill.ui.ctx
import app.sill.ui.theme.S
import app.sill.ui.theme.SType

/** Out of the window: everything that took, then the vase and the compost heap. */
@Composable
fun GardenScreen(home: Home, go: Go) {
    val c = ctx()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.garden), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            GardenView(home.garden.map { home.look(it) }, Modifier.fillMaxWidth().aspectRatio(1.25f))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(14.dp))
                Text(
                    if (home.garden.isEmpty()) stringResource(R.string.garden_empty)
                    else c.resources.getQuantityString(R.plurals.garden_line, home.garden.size, home.garden.size),
                    style = SType.Sub,
                )
                home.garden.forEach { p -> Line(p, home, go, stringResource(R.string.in_garden_since, T.date(c, p.endedAt))) }
                if (home.dried.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.vase))
                    Text(stringResource(R.string.vase_sub), style = SType.Muted)
                    home.dried.forEach { p -> Line(p, home, go, stringResource(R.string.dried_since, T.date(c, p.endedAt))) }
                }
                if (home.compost.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.compost_heap))
                    Text(
                        if (home.compostLeft > 0) c.resources.getQuantityString(R.plurals.compost_ready, home.compostLeft, home.compostLeft) else stringResource(R.string.compost_used),
                        style = SType.Muted,
                    )
                    home.compost.forEach { p -> Line(p, home, go, stringResource(R.string.in_compost_since, T.date(c, p.endedAt)), faded = true) }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun Line(p: Planting, home: Home, go: Go, sub: String, faded: Boolean = false) {
    val c = ctx()
    val s = home.seed(p.seed) ?: return
    Row(Modifier.fillMaxWidth().clip(S.RMd).clickable { go.plant(p.id) }.padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(s.section.ink).copy(alpha = if (faded) 0.4f else 1f)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name(home.ru), style = SType.Strong.copy(color = if (faded) S.c.muted else S.c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$sub · ${c.resources.getQuantityString(R.plurals.waterings, home.count(p.id), home.count(p.id))}", style = SType.Muted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
    }
}
