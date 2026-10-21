package app.sill.ui.packet

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sill.R
import app.sill.container
import app.sill.data.Origin
import app.sill.data.Seed
import app.sill.domain.Gift
import app.sill.domain.Gifts
import app.sill.domain.Rules
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.Standing
import app.sill.ui.T
import app.sill.ui.art.SeedPacket
import app.sill.ui.components.Field
import app.sill.ui.components.PrimaryButton
import app.sill.ui.components.SectionTitle
import app.sill.ui.components.SoftButton
import app.sill.ui.components.TextLink
import app.sill.ui.components.TopBar
import app.sill.ui.ctx
import app.sill.ui.theme.S
import app.sill.ui.theme.SType
import app.sill.ui.theme.Serif

/** A packet, front and back: what it is, how to begin, and sowing it, saving it or giving it away. */
@Composable
fun PacketScreen(key: String, tinId: Long, home: Home, go: Go) {
    val c = ctx()
    val seed = home.seed(key)
    if (seed == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) { TopBar("", go::back) }
        return
    }
    val packet = home.packets.firstOrNull { it.id == tinId } ?: home.packetOf(key)
    val standing = home.standing(key)
    val planting = home.plantingOf(key)
    var giving by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(T.section(c, seed.section), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            SeedPacket(
                seed, seed.name(home.ru), stringResource(R.string.seeds), T.section(c, seed.section),
                foot = c.resources.getQuantityString(R.plurals.waterings_to_bloom, seed.bloom, seed.bloom),
                modifier = Modifier.fillMaxWidth(0.74f).align(Alignment.CenterHorizontally).padding(top = 4.dp),
            )
            if (packet != null) Note(packet.originOf, packet.from, packet.note, home)
            Spacer(Modifier.height(18.dp))
            Back(seed, home)
            if (seed.neighbors.isNotEmpty()) {
                SectionTitle(stringResource(R.string.neighbors))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    seed.neighbors.mapNotNull(home::seed).forEach { n ->
                        SeedPacket(
                            n, n.name(home.ru), stringResource(R.string.seeds), T.section(c, n.section), foot = "", compact = true,
                            modifier = Modifier.width(118.dp).clip(S.RSm).clickable { go.packet(n.key) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            when (standing) {
                Standing.POT -> PrimaryButton(stringResource(R.string.growing_open), { planting?.let { go.plant(it.id) } }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Spa)
                Standing.GARDEN -> SoftButton(
                    stringResource(R.string.in_garden_since, T.date(c, planting?.endedAt ?: home.now)), { planting?.let { go.plant(it.id) } }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Yard,
                )
                else -> {
                    val free = home.freePot != null
                    PrimaryButton(
                        stringResource(if (free) R.string.sow else R.string.pots_full),
                        { c.container.write { garden.plant(key, packet?.id ?: 0) }; go.sill() },
                        Modifier.fillMaxWidth(), enabled = free, icon = Icons.Rounded.Spa,
                    )
                    if (free && home.compostLeft > 0) {
                        Text(
                            stringResource(R.string.on_compost, Rules.target(seed, true)), style = SType.Muted, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    if (!free) Text(stringResource(R.string.pots_full_sub), style = SType.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Spacer(Modifier.height(10.dp))
                    if (packet == null) SoftButton(stringResource(R.string.to_tin), { c.container.write { garden.save(key) } }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Inventory2)
                }
            }
            Spacer(Modifier.height(10.dp))
            SoftButton(stringResource(R.string.give), { giving = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CardGiftcard)
            if (packet != null && standing != Standing.POT) {
                TextLink(
                    stringResource(if (packet.originOf == Origin.WIND) R.string.blow_away else R.string.out_of_tin),
                    { c.container.write { garden.discard(packet.id) }; go.back() },
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (giving) GiveDialog(seed, home, onDone = { giving = false })
}

/** Who it came from: a sticky note from a friend, or a line about the wind or the plant that dropped it. */
@Composable
private fun Note(origin: Origin, from: String, note: String, home: Home) {
    val c = ctx()
    val who = if (origin == Origin.SELF) home.seed(from)?.name(home.ru).orEmpty() else from
    if (origin == Origin.GIFT) {
        Column(
            Modifier.fillMaxWidth(0.8f).padding(top = 16.dp).rotate(-1.5f).background(S.c.sticky).padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (note.isNotBlank()) Text(note, style = SType.Hand)
            Text("— ${who.ifBlank { stringResource(R.string.a_friend) }}", style = SType.Hand.copy(color = S.c.muted, fontSize = 20.sp), modifier = Modifier.align(Alignment.End))
        }
    } else {
        Text(T.origin(c, origin, who), style = SType.Muted.copy(fontStyle = FontStyle.Italic), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
    }
}

/** The back of the packet: the small print. */
@Composable
private fun Back(seed: Seed, home: Home) {
    val c = ctx()
    Column(Modifier.fillMaxWidth().clip(S.RSm).background(S.c.paper).border(1.dp, S.c.line, S.RSm).padding(18.dp)) {
        Text(seed.about(home.ru), style = SType.Body.copy(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 24.sp))
        Spacer(Modifier.height(14.dp))
        Spec(stringResource(R.string.depth), "●".repeat(seed.depth) + "○".repeat(3 - seed.depth) + "  " + stringResource(listOf(R.string.depth_1, R.string.depth_2, R.string.depth_3)[seed.depth - 1]))
        Spec(stringResource(R.string.germination), stringResource(R.string.germination_v, Rules.germination(seed)))
        Spec(stringResource(R.string.to_bloom), c.resources.getQuantityString(R.plurals.waterings, seed.bloom, seed.bloom))
        Spec(stringResource(R.string.sow_when), T.months(c, seed.months))
        if (seed.start(home.ru).isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.how_to_start).uppercase(), style = SType.Eyebrow)
            Spacer(Modifier.height(6.dp))
            Text(seed.start(home.ru), style = SType.Body)
        }
    }
}

@Composable
private fun Spec(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label.uppercase(), style = SType.Typed.copy(color = S.c.muted, fontSize = 12.sp), modifier = Modifier.weight(1f))
        Text(value, style = SType.Typed, textAlign = TextAlign.End)
    }
}

/** A note to go with the packet, then the share sheet. */
@Composable
fun GiveDialog(seed: Seed, home: Home, onDone: () -> Unit) {
    val c = ctx()
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.give_title), style = SType.Title) },
        text = {
            Column {
                Text(stringResource(R.string.give_sub), style = SType.Muted)
                Spacer(Modifier.height(12.dp))
                Field(note, { note = it.take(200) }, stringResource(R.string.give_note), singleLine = false, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { share(c, seed, home, note); onDone() }) { Text(stringResource(R.string.give_send)) } },
        dismissButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) } },
        containerColor = S.c.surface,
    )
}

fun share(c: Context, seed: Seed, home: Home, note: String) {
    val from = home.settings.name
    val gift = if (seed.own) {
        Gift(key = "", from = from, note = note.trim(), section = seed.section, name = seed.name(home.ru), about = seed.about(home.ru), start = seed.start(home.ru), bloom = seed.bloom)
    } else Gift(key = seed.key, from = from, note = note.trim())
    val text = Gifts.message(gift, seed.name(home.ru), c.getString(R.string.give_invite))
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    c.startActivity(Intent.createChooser(send, c.getString(R.string.give)))
}
