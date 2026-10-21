package app.sill.ui.tin

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sill.R
import app.sill.data.Origin
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.T
import app.sill.ui.art.SeedPacket
import app.sill.ui.components.SoftButton
import app.sill.ui.components.TopBar
import app.sill.ui.ctx
import app.sill.ui.theme.S
import app.sill.ui.theme.SType

/** The seed tin: gifts, what the wind brought, what blooms dropped, what was put aside. */
@Composable
fun TinScreen(home: Home, go: Go) {
    val c = ctx()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.tin), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.tin_sub), style = SType.Sub)
            Spacer(Modifier.height(12.dp))
            if (home.packets.isEmpty()) {
                Text(stringResource(R.string.tin_empty), style = SType.Muted, modifier = Modifier.padding(vertical = 24.dp))
                SoftButton(stringResource(R.string.catalogue), go::catalogue, Modifier.fillMaxWidth())
            }
            home.packets.forEach { p ->
                val s = home.seed(p.seed) ?: return@forEach
                Row(Modifier.fillMaxWidth().clip(S.RMd).clickable { go.packet(p.seed, p.id) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SeedPacket(s, "", stringResource(R.string.seeds), T.section(c, s.section), foot = "", compact = true, modifier = Modifier.width(84.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name(home.ru), style = SType.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val who = if (p.originOf == Origin.SELF) home.seed(p.from)?.name(home.ru).orEmpty() else p.from
                        Text(T.origin(c, p.originOf, who), style = SType.Muted.copy(color = if (p.originOf == Origin.GIFT) S.c.clay else S.c.muted))
                        if (p.note.isNotBlank()) Text("«${p.note}»", style = SType.Hand.copy(fontSize = 20.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(T.ago(c, p.addedAt), style = SType.Small)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
