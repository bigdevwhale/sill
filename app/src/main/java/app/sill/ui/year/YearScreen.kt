package app.sill.ui.year

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sill.R
import app.sill.data.Place
import app.sill.domain.Rules
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.T
import app.sill.ui.art.HerbariumSheet
import app.sill.ui.components.TopBar
import app.sill.ui.ctx
import app.sill.ui.theme.S
import app.sill.ui.theme.SType

/** The year's herbarium: every seed sown that year, pressed on a sheet, whatever became of it. */
@Composable
fun YearScreen(home: Home, go: Go) {
    val c = ctx()
    val thisYear = Rules.year(home.now)
    val first = home.plantings.minOfOrNull { Rules.year(it.plantedAt) } ?: thisYear
    var year by rememberSaveable { mutableIntStateOf(thisYear) }
    val sheets = home.plantings.filter { Rules.year(it.plantedAt) == year }.sortedBy { it.plantedAt }
    val ids = sheets.map { it.id }.toSet()
    val water = home.waterings.count { it.plantingId in ids }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.herbarium), go::back)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { year-- }, enabled = year > first) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = if (year > first) S.c.ink else S.c.line) }
                        Text("$year", style = SType.Hero.copy(fontSize = 34.sp), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        IconButton(onClick = { year++ }, enabled = year < thisYear) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = if (year < thisYear) S.c.ink else S.c.line) }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Stat(sheets.size, R.string.stat_sown, Modifier.weight(1f))
                        Stat(sheets.count { it.placeOf == Place.GARDEN }, R.string.stat_took, Modifier.weight(1f))
                        Stat(sheets.count { it.placeOf == Place.COMPOST || it.placeOf == Place.DRIED }, R.string.stat_let_go, Modifier.weight(1f))
                        Stat(water, R.string.stat_water, Modifier.weight(1f))
                    }
                    if (sheets.isEmpty()) Text(stringResource(R.string.herbarium_empty), style = SType.Muted, modifier = Modifier.padding(vertical = 24.dp))
                }
            }
            items(sheets, key = { it.id }) { p ->
                val s = home.seed(p.seed)
                Column(Modifier.clip(S.RSm).clickable { go.plant(p.id) }) {
                    HerbariumSheet(home.look(p, pressed = true), Modifier.fillMaxWidth().aspectRatio(0.75f))
                    Text(s?.name(home.ru).orEmpty(), style = SType.Hand.copy(fontSize = 21.sp, lineHeight = 22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    val fate = when (p.placeOf) {
                        Place.POT -> T.stage(c, home.stage(p))
                        Place.GARDEN -> stringResource(R.string.fate_garden)
                        Place.COMPOST -> stringResource(R.string.fate_compost)
                        Place.DRIED -> stringResource(R.string.fate_dried)
                    }
                    Text("${T.stamp(p.plantedAt)} · $fate", style = SType.Small)
                }
            }
        }
    }
}

@Composable
private fun Stat(n: Int, label: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", style = SType.Title.copy(fontSize = 24.sp))
        Text(stringResource(label), style = SType.Small, textAlign = TextAlign.Center)
    }
}
