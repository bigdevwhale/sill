package app.sill.ui.plant

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.sill.R
import app.sill.container
import app.sill.data.Photos
import app.sill.data.Place
import app.sill.data.Seed
import app.sill.ru
import app.sill.domain.Stage
import app.sill.domain.Thirst
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.T
import app.sill.ui.art.PotPortrait
import app.sill.ui.components.Field
import app.sill.ui.components.PrimaryButton
import app.sill.ui.components.SectionTitle
import app.sill.ui.components.SoftButton
import app.sill.ui.components.TopBar
import app.sill.ui.ctx
import app.sill.ui.packet.GiveDialog
import app.sill.ui.theme.S
import app.sill.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Ask { COMPOST, DRY, UPROOT }

/** One plant: how far it's come, watering it, moving it out, or letting it go. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlantScreen(id: Long, home: Home, go: Go) {
    val c = ctx()
    val p = home.planting(id)
    val seed = p?.let { home.seed(it.seed) }
    if (p == null || seed == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) { TopBar("", go::back) }
        return
    }
    val scope = rememberCoroutineScope()
    val drops = remember { Animatable(0f) }
    var watering by rememberSaveable { mutableStateOf(false) }
    var ask by remember { mutableStateOf<Ask?>(null) }
    var menu by remember { mutableStateOf(false) }
    var bloomed by remember { mutableStateOf<Int?>(null) }
    var giving by remember { mutableStateOf(false) }
    var forget by remember { mutableStateOf<Long?>(null) }
    val stage = home.stage(p)
    val thirst = home.thirst(p)
    val count = home.count(p.id)
    val target = home.target(p)
    val place = p.placeOf

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(T.section(c, seed.section), go::back) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null, tint = S.c.ink) }
                DropdownMenu(menu, { menu = false }, containerColor = S.c.surface) {
                    DropdownMenuItem({ Text(stringResource(R.string.open_packet)) }, { menu = false; go.packet(seed.key) })
                    DropdownMenuItem({ Text(stringResource(R.string.uproot)) }, { menu = false; ask = Ask.UPROOT })
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            PotPortrait(home.view(p), p.pot, Modifier.fillMaxWidth().aspectRatio(1.05f), drops = drops.value)
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(seed.name(home.ru), style = SType.Display)
                Spacer(Modifier.height(4.dp))
                val headline = when (place) {
                    Place.GARDEN -> stringResource(R.string.in_garden_since, T.date(c, p.endedAt))
                    Place.COMPOST -> stringResource(R.string.in_compost_since, T.date(c, p.endedAt))
                    Place.DRIED -> stringResource(R.string.dried_since, T.date(c, p.endedAt))
                    Place.POT -> if (stage == Stage.SEED) T.stage(c, stage) else "${T.stage(c, stage)} · ${c.getString(R.string.watered_of, count, target)}"
                }
                Text(headline, style = SType.Sub)
                if (place == Place.POT) {
                    Spacer(Modifier.height(12.dp))
                    Progress(count, target)
                    Spacer(Modifier.height(10.dp))
                    val last = home.lastWater(p.id)
                    val line = when {
                        stage == Stage.BLOOM -> stringResource(R.string.bloom_line)
                        last == null -> stringResource(R.string.sown_line, T.ago(c, p.plantedAt).lowercase())
                        else -> stringResource(R.string.watered_line, T.ago(c, last).lowercase()) + if (thirst != Thirst.FRESH) " · " + T.thirst(c, thirst) else ""
                    }
                    Text(line, style = SType.Muted.copy(color = if (thirst != Thirst.FRESH) S.c.water else S.c.muted))
                }
                if (p.from.isNotBlank() || p.fromNote.isNotBlank()) {
                    Column(Modifier.padding(top = 16.dp).fillMaxWidth(0.85f).rotate(-1.2f).background(S.c.sticky).padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (p.fromNote.isNotBlank()) Text(p.fromNote, style = SType.Hand)
                        Text("— ${p.from.ifBlank { stringResource(R.string.a_friend) }}", style = SType.Hand.copy(color = S.c.muted, fontSize = 20.sp), modifier = Modifier.align(Alignment.End))
                    }
                }
                if (place == Place.POT && stage == Stage.SEED && seed.start(home.ru).isNotBlank()) {
                    Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(S.RSm).background(S.c.paper).border(1.dp, S.c.line, S.RSm).padding(16.dp)) {
                        Text(stringResource(R.string.how_to_start).uppercase(), style = SType.Eyebrow)
                        Spacer(Modifier.height(6.dp))
                        Text(seed.start(home.ru), style = SType.Body)
                    }
                }
                Spacer(Modifier.height(22.dp))
                when (place) {
                    Place.POT -> if (stage == Stage.BLOOM) {
                        PrimaryButton(stringResource(R.string.to_garden), { c.container.write { garden.toGarden(p.id) }; go.back() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Yard)
                        Spacer(Modifier.height(10.dp))
                        SoftButton(stringResource(R.string.give_seeds), { giving = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CardGiftcard)
                        Spacer(Modifier.height(10.dp))
                        SoftButton(stringResource(R.string.water_again), { watering = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.WaterDrop)
                    } else {
                        WaterButton(stringResource(if (stage == Stage.SEED) R.string.water_first else R.string.water)) { watering = true }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SoftButton(stringResource(R.string.compost), { ask = Ask.COMPOST }, Modifier.weight(1f), icon = Icons.Rounded.Eco)
                            SoftButton(stringResource(R.string.dry), { ask = Ask.DRY }, Modifier.weight(1f), icon = Icons.Rounded.LocalFlorist)
                        }
                    }
                    Place.GARDEN -> {
                        SoftButton(stringResource(R.string.water_garden), { watering = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.WaterDrop)
                        Spacer(Modifier.height(10.dp))
                        SoftButton(stringResource(R.string.give_seeds), { giving = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CardGiftcard)
                    }
                    Place.COMPOST, Place.DRIED -> {
                        SoftButton(
                            stringResource(if (home.freePot != null) R.string.sow_again else R.string.pots_full),
                            { c.container.write { garden.plant(seed.key) }; go.sill() }, Modifier.fillMaxWidth(), enabled = home.freePot != null, icon = Icons.Rounded.Spa,
                        )
                    }
                }
                val log = home.waterings(p.id)
                if (log.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.journal))
                    log.forEachIndexed { i, w ->
                        Row(
                            Modifier.fillMaxWidth().clip(S.RSm).combinedClickable(onClick = {}, onLongClick = { forget = w.id }).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.width(78.dp)) {
                                Text("№ ${log.size - i}", style = SType.Small.copy(color = S.c.water))
                                Text(T.date(c, w.at), style = SType.Small)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(w.note.ifBlank { "—" }, style = SType.Hand.copy(color = if (w.note.isBlank()) S.c.faint else S.c.ink))
                                w.photo?.let { Thumb(it) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(if (p.fertilized) R.string.sown_on_compost else R.string.sown_on, T.date(c, p.plantedAt)),
                    style = SType.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                )
            }
        }
    }

    if (watering) {
        WaterSheet(seed, onDismiss = { watering = false }) { note, photo ->
            watering = false
            scope.launch {
                val r = withContext(Dispatchers.Default) { c.container.garden.water(p.id, note, photo) }
                drops.snapTo(0f)
                drops.animateTo(1f, tween(1400, easing = LinearEasing))
                drops.snapTo(0f)
                if (r.bloomed) bloomed = r.selfSown
            }
        }
    }
    bloomed?.let { n ->
        AlertDialog(
            onDismissRequest = { bloomed = null },
            title = { Text(stringResource(R.string.bloomed_title), style = SType.Title) },
            text = {
                Column {
                    Text(stringResource(R.string.bloomed_text), style = SType.Body)
                    if (n > 0) Text(c.resources.getQuantityString(R.plurals.self_sown, n, n), style = SType.Muted, modifier = Modifier.padding(top = 10.dp))
                }
            },
            confirmButton = { TextButton(onClick = { bloomed = null; c.container.write { garden.toGarden(p.id) }; go.back() }) { Text(stringResource(R.string.to_garden)) } },
            dismissButton = { TextButton(onClick = { bloomed = null }) { Text(stringResource(R.string.later)) } },
            containerColor = S.c.surface,
        )
    }
    ask?.let { a ->
        val (title, text, yes) = when (a) {
            Ask.COMPOST -> Triple(R.string.compost_title, R.string.compost_text, R.string.compost)
            Ask.DRY -> Triple(R.string.dry_title, R.string.dry_text, R.string.dry)
            Ask.UPROOT -> Triple(R.string.uproot_title, R.string.uproot_text, R.string.uproot)
        }
        AlertDialog(
            onDismissRequest = { ask = null },
            title = { Text(stringResource(title), style = SType.Title) },
            text = { Text(stringResource(text), style = SType.Body) },
            confirmButton = {
                TextButton(onClick = {
                    ask = null
                    c.container.write {
                        when (a) {
                            Ask.COMPOST -> garden.compost(p.id)
                            Ask.DRY -> garden.dry(p.id)
                            Ask.UPROOT -> garden.uproot(p.id)
                        }
                    }
                    go.back()
                }) { Text(stringResource(yes)) }
            },
            dismissButton = { TextButton(onClick = { ask = null }) { Text(stringResource(R.string.cancel)) } },
            containerColor = S.c.surface,
        )
    }
    forget?.let { wid ->
        AlertDialog(
            onDismissRequest = { forget = null },
            title = { Text(stringResource(R.string.unwater_title), style = SType.Title) },
            confirmButton = { TextButton(onClick = { forget = null; c.container.write { garden.unwater(wid) } }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { forget = null }) { Text(stringResource(R.string.cancel)) } },
            containerColor = S.c.surface,
        )
    }
    if (giving) GiveDialog(seed, home, onDone = { giving = false })
}

/** Waterings so far against the bloom: drops for short roads, a bar for long ones. */
@Composable
private fun Progress(count: Int, target: Int) {
    if (target <= 12) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(target) { i ->
                Icon(Icons.Rounded.WaterDrop, null, Modifier.size(22.dp), tint = if (i < count) S.c.water else S.c.line)
            }
        }
    } else {
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(S.c.line)) {
            Box(Modifier.fillMaxWidth((count.toFloat() / target).coerceIn(0f, 1f)).height(10.dp).clip(CircleShape).background(S.c.water))
        }
    }
}

@Composable
private fun WaterButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = S.RLg,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = S.c.water, contentColor = Color.White),
    ) {
        Icon(Icons.Rounded.WaterDrop, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = SType.Button.copy(fontSize = 17.sp))
    }
}

/** One more time: a line about it and, if wanted, a photo from the phone's camera. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaterSheet(seed: Seed, onDismiss: () -> Unit, onSave: (String, String?) -> Unit) {
    val c = ctx()
    var note by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val name = pending
        if (ok && name != null && Photos.file(c, name).length() > 0) {
            photo?.let { Photos.delete(c, it) }
            photo = name
        } else Photos.delete(c, name)
        pending = null
    }
    ModalBottomSheet(onDismissRequest = { photo?.let { Photos.delete(c, it) }; onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = S.c.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.water_title), style = SType.Title)
            Text(seed.name(c.ru), style = SType.Muted)
            Spacer(Modifier.height(14.dp))
            Field(note, { note = it.take(240) }, stringResource(R.string.water_note), singleLine = false, minLines = 2, style = SType.Hand)
            Spacer(Modifier.height(12.dp))
            SoftButton(
                stringResource(if (photo != null) R.string.photo_added else R.string.photo),
                {
                    val f = Photos.fresh(c)
                    pending = f.name
                    val uri: Uri = FileProvider.getUriForFile(c, "${c.packageName}.files", f)
                    runCatching { camera.launch(uri) }.onFailure { Toast.makeText(c, R.string.no_camera, Toast.LENGTH_LONG).show() }
                },
                Modifier.fillMaxWidth(), icon = if (photo != null) Icons.Rounded.Check else Icons.Rounded.PhotoCamera,
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(stringResource(R.string.water_save), { onSave(note, photo) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.WaterDrop)
        }
    }
}

@Composable
private fun Thumb(name: String) {
    val c = ctx()
    val image by produceState<ImageBitmap?>(null, name) {
        value = withContext(Dispatchers.IO) {
            val f = Photos.file(c, name)
            if (!f.exists()) return@withContext null
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, o)
            var s = 1
            while (o.outWidth / s > 600) s *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
        }
    }
    image?.let { Image(it, null, Modifier.padding(top = 6.dp).size(width = 140.dp, height = 100.dp).clip(S.RSm), contentScale = ContentScale.Crop) }
}
