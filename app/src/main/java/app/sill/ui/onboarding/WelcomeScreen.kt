package app.sill.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sill.R
import app.sill.container
import app.sill.data.Section
import app.sill.domain.Stage
import app.sill.ui.art.PlantLook
import app.sill.ui.art.PotView
import app.sill.ui.art.SillScene
import app.sill.ui.components.Field
import app.sill.ui.components.PrimaryButton
import app.sill.ui.ctx
import app.sill.ui.theme.S
import app.sill.ui.theme.SType

@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val c = ctx()
    var name by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().imePadding()) {
        // Five empty pots and one first sprout.
        SillScene(
            pots = listOf(null, null, PotView(PlantLook(Section.FLOWERS, 7, 5L, 0.1f, Stage.SPROUT), false), null, null),
            garden = emptyList(), dried = emptyList(), windPacket = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
        )
        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.app_name), style = SType.Hero)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.welcome_sub), style = SType.Sub)
            Spacer(Modifier.height(20.dp))
            Point(Icons.Rounded.Spa, stringResource(R.string.welcome_1))
            Point(Icons.Rounded.WaterDrop, stringResource(R.string.welcome_2))
            Point(Icons.Rounded.LocalFlorist, stringResource(R.string.welcome_3))
            Spacer(Modifier.height(18.dp))
            Field(name, { name = it.take(40) }, stringResource(R.string.your_name_hint))
            Text(stringResource(R.string.your_name_sub), style = SType.Muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.welcome_go), {
                c.container.write { settings.update { it.copy(onboarded = true, name = name.trim()) } }
                onDone()
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(22.dp).padding(top = 1.dp), tint = S.c.leaf)
        Text(text, style = SType.Body, modifier = Modifier.weight(1f))
    }
}
