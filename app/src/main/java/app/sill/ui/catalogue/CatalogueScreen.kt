package app.sill.ui.catalogue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sill.R
import app.sill.data.Section
import app.sill.data.Seed
import app.sill.domain.Rules
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.Standing
import app.sill.ui.T
import app.sill.ui.art.MiniPot
import app.sill.ui.art.PlantLook
import app.sill.ui.art.SeedPacket
import app.sill.ui.art.darker
import app.sill.domain.Stage
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import app.sill.ui.components.Chip
import app.sill.ui.components.Field
import app.sill.ui.components.TopBar
import app.sill.ui.ctx
import app.sill.ui.theme.S
import app.sill.ui.theme.SType

/** The seed catalogue: every packet by section, with what's in season and a search. */
@Composable
fun CatalogueScreen(home: Home, go: Go) {
    val c = ctx()
    var section by rememberSaveable { mutableStateOf<String?>(null) }
    var season by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val month = Rules.month(home.now)
    val shown = remember(home.seeds, section, season, query, home.ru) {
        home.seeds.filter { s ->
            (section == null || s.section.name == section) && (!season || s.inSeason(month)) &&
                (query.isBlank() || s.nameEn.contains(query, true) || s.nameRu.contains(query, true) || s.aboutEn.contains(query, true) || s.aboutRu.contains(query, true))
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.catalogue), go::back) {
            IconButton(onClick = go::own) { Icon(Icons.Rounded.Edit, stringResource(R.string.own_seed), tint = S.c.ink) }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Field(query, { query = it }, stringResource(R.string.search))
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.in_season), season) { season = !season }
                        Chip(stringResource(R.string.all), section == null) { section = null }
                        Section.entries.forEach { s -> Chip(T.section(c, s), section == s.name) { section = if (section == s.name) null else s.name } }
                    }
                    section?.let { SectionHead(Section.valueOf(it), null) }
                    if (section == null && query.isBlank()) Text(stringResource(R.string.catalogue_sub), style = SType.Muted, modifier = Modifier.padding(top = 12.dp, start = 4.dp))
                }
            }
            if (shown.isEmpty()) item(span = { GridItemSpan(2) }) {
                Text(stringResource(R.string.nothing_found), style = SType.Muted, modifier = Modifier.padding(24.dp))
            }
            // Browsing everything: section by section, each with a head saying what it holds.
            if (section == null && query.isBlank()) {
                shown.groupBy { it.section }.toSortedMap().forEach { (sec, seeds) ->
                    item(span = { GridItemSpan(2) }, key = "head-${sec.name}") { SectionHead(sec) { section = sec.name } }
                    items(seeds, key = { it.key }) { s -> CatalogueItem(s, home, go) }
                }
            } else {
                items(shown, key = { it.key }) { s -> CatalogueItem(s, home, go) }
            }
            item(span = { GridItemSpan(2) }) {
                Box(
                    Modifier.fillMaxWidth().clip(S.RMd).background(S.c.surface).clickable(onClick = go::own).padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.own_seed), style = SType.Title)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.own_seed_sub), style = SType.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogueItem(s: Seed, home: Home, go: Go) {
    val c = ctx()
    val standing = home.standing(s.key)
    Column(Modifier.clip(S.RSm).clickable { go.packet(s.key) }) {
        SeedPacket(
            s, s.name(home.ru), stringResource(R.string.seeds), T.section(c, s.section),
            foot = c.resources.getQuantityString(R.plurals.waterings_to_bloom, s.bloom, s.bloom),
            compact = true, ghost = standing == Standing.GARDEN,
            modifier = Modifier.fillMaxWidth(),
        )
        val tag = when (standing) {
            Standing.POT -> R.string.tag_pot
            Standing.GARDEN -> R.string.tag_garden
            Standing.COMPOST -> R.string.tag_compost
            Standing.DRIED -> R.string.tag_dried
            Standing.TIN -> R.string.tag_tin
            Standing.NONE -> null
        }
        if (tag != null) Text(stringResource(tag), style = SType.Small.copy(color = if (standing == Standing.POT || standing == Standing.GARDEN) S.c.leaf else S.c.faint), modifier = Modifier.padding(top = 6.dp, start = 2.dp))
    }
}

/** A section's head: its plant, its name in its ink, and what kind of new things it holds. */
@Composable
private fun SectionHead(s: Section, onClick: (() -> Unit)?) {
    val c = ctx()
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).clip(S.RMd).background(Color(s.ink).copy(alpha = 0.1f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniPot(PlantLook(s, s.name.hashCode(), s.ordinal * 7L + 3, 1f, Stage.BLOOM), Modifier.size(width = 44.dp, height = 54.dp), style = s.ordinal)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(T.section(c, s), style = SType.Title.copy(color = Color(s.ink).darker(0.1f)))
            Text(T.holds(c, s), style = SType.Muted)
        }
    }
}
