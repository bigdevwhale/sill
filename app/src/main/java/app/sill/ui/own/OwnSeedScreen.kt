package app.sill.ui.own

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sill.R
import app.sill.container
import app.sill.data.Section
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.T
import app.sill.ui.components.Chip
import app.sill.ui.components.Field
import app.sill.ui.components.PrimaryButton
import app.sill.ui.components.SectionTitle
import app.sill.ui.components.TopBar
import app.sill.ui.ctx
import app.sill.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Waterings to bloom a hand-written packet can ask for: from a radish to an oak. */
private val Blooms = listOf(3, 5, 8, 12, 20, 30, 40)

/** A packet written by hand, for something the catalogue doesn't have. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnSeedScreen(@Suppress("UNUSED_PARAMETER") home: Home, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf(Section.FLOWERS.name) }
    var about by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf("") }
    var bloom by rememberSaveable { mutableIntStateOf(5) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.own_seed), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.own_seed_sub), style = SType.Sub)
            Spacer(Modifier.height(16.dp))
            Field(name, { name = it.take(60) }, stringResource(R.string.own_name))
            SectionTitle(stringResource(R.string.own_section))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Section.entries.forEach { s -> Chip(T.section(c, s), section == s.name) { section = s.name } }
            }
            Text(T.holds(c, Section.valueOf(section)), style = SType.Muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
            Spacer(Modifier.height(16.dp))
            Field(about, { about = it.take(300) }, stringResource(R.string.own_about), singleLine = false, minLines = 2)
            Spacer(Modifier.height(12.dp))
            Field(start, { start = it.take(300) }, stringResource(R.string.own_start), singleLine = false, minLines = 2)
            SectionTitle(stringResource(R.string.to_bloom))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Blooms.forEach { b -> Chip(c.resources.getQuantityString(R.plurals.waterings, b, b), bloom == b) { bloom = b } }
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                stringResource(R.string.own_save),
                {
                    scope.launch {
                        val key = withContext(Dispatchers.Default) { c.container.garden.writeOwn(Section.valueOf(section), name, about, start, bloom) }
                        go.written(key)
                    }
                },
                Modifier.fillMaxWidth(), enabled = name.isNotBlank(), icon = Icons.Rounded.Check,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
